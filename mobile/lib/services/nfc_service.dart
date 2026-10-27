import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:cccd_vietnam/dmrtd.dart';
import 'package:flutter/foundation.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/nfc_manager_android.dart';

enum IdentityNfcMethod { realNfc, signedNdef, aiOnly }

class RealCccdAccessData {
  const RealCccdAccessData({
    required this.documentNumber,
    required this.dateOfBirth,
    required this.dateOfExpiry,
  });

  final String documentNumber;
  final DateTime dateOfBirth;
  final DateTime dateOfExpiry;
}

class NfcIdentityResult {
  const NfcIdentityResult({
    required this.method,
    this.identityValue,
    this.ndefProof,
    this.faceImageBytes,
    this.sodRead = false,
    this.activeAuthenticationAttempted = false,
  });

  final IdentityNfcMethod method;
  final String? identityValue;
  final String? ndefProof;
  final Uint8List? faceImageBytes;
  final bool sodRead;
  final bool activeAuthenticationAttempted;
}

class NfcService {
  static Future<bool> isNfcAvailable() async =>
      await NfcManager.instance.checkAvailability() == NfcAvailability.enabled;

  /// Research-only ICAO reader. BAC/PACE grants access to the chip but does not
  /// by itself prove an official C06 identity. The caller must label the result
  /// accordingly and compare the DG2 portrait with a live selfie.
  static Future<NfcIdentityResult> readRealCccd(
    RealCccdAccessData access,
  ) async {
    final nfc = NfcProvider()..timeout = const Duration(seconds: 25);
    try {
      await nfc.connect(
        timeout: const Duration(seconds: 25),
        iosAlertMessage: 'Giữ CCCD ổn định gần vị trí NFC',
      );
      if (!nfc.isConnected()) {
        throw StateError('Không nhận được thẻ ISO-7816.');
      }

      final passport = Passport(nfc);
      final paceKey = DBAKey(
        access.documentNumber.trim(),
        access.dateOfBirth,
        access.dateOfExpiry,
        paceMode: true,
      );
      var sessionStarted = false;
      try {
        final cardAccess = await passport.readEfCardAccess();
        await passport.startSessionPACE(paceKey, cardAccess);
        sessionStarted = true;
      } catch (_) {
        final bacKey = DBAKey(
          access.documentNumber.trim(),
          access.dateOfBirth,
          access.dateOfExpiry,
        );
        await passport.startSession(bacKey);
        sessionStarted = true;
      }
      if (!sessionStarted) {
        throw StateError('Không thể thiết lập phiên BAC/PACE.');
      }

      final com = await passport.readEfCOM();
      if (!com.dgTags.contains(EfDG1.TAG) || !com.dgTags.contains(EfDG2.TAG)) {
        throw StateError('Chip không cung cấp đủ DG1/DG2.');
      }
      final dg1 = await passport.readEfDG1();
      final dg2 = await passport.readEfDG2();
      final faceBytes = dg2.imageData;
      if (faceBytes == null || faceBytes.isEmpty) {
        throw StateError('Không đọc được ảnh chân dung DG2.');
      }

      var sodRead = false;
      try {
        await passport.readEfSOD();
        sodRead = true;
      } catch (_) {
        // EF.SOD is optional for this research fallback. The backend will keep
        // the trust level below an officially authenticated identity.
      }

      var activeAuthenticationAttempted = false;
      if (com.dgTags.contains(EfDG15.TAG)) {
        try {
          await passport.readEfDG15();
          final random = Random.secure();
          await passport.activeAuthenticate(
            Uint8List.fromList(
              List<int>.generate(8, (_) => random.nextInt(256)),
            ),
          );
          activeAuthenticationAttempted = true;
        } catch (_) {
          activeAuthenticationAttempted = false;
        }
      }

      return NfcIdentityResult(
        method: IdentityNfcMethod.realNfc,
        identityValue: dg1.mrz.documentNumber,
        faceImageBytes: faceBytes,
        sodRead: sodRead,
        activeAuthenticationAttempted: activeAuthenticationAttempted,
      );
    } finally {
      try {
        await nfc.disconnect();
      } catch (error) {
        debugPrint('Không thể đóng phiên NFC ICAO: $error');
      }
    }
  }

  /// Reads a signed NDEF demo payload. Signature verification is intentionally
  /// performed by the backend; the mobile app never contains the signing key.
  static Future<NfcIdentityResult> readSignedDemoNdef() async {
    if (!await isNfcAvailable()) {
      throw StateError('Thiết bị chưa bật hoặc không hỗ trợ NFC.');
    }

    final completer = Completer<NfcIdentityResult>();
    await NfcManager.instance.startSession(
      pollingOptions: const {NfcPollingOption.iso14443},
      onDiscovered: (NfcTag tag) async {
        try {
          final ndef = NdefAndroid.from(tag);
          if (ndef == null) {
            throw const FormatException('Đây không phải thẻ NDEF mô phỏng.');
          }
          final message = ndef.cachedNdefMessage ?? await ndef.getNdefMessage();
          final list = message?.records ?? const [];
          if (list.isEmpty) throw const FormatException('Thẻ NDEF trống.');
          final payload = List<int>.from(list.first.payload);
          final decoded = _parseNdefTextPayload(payload).trim();
          final json = jsonDecode(decoded);
          if (json is! Map || json['issuer'] != 'MAY_HOTEL_DEMO') {
            throw const FormatException(
              'Payload không phải thẻ MAY HOTEL demo.',
            );
          }
          if (!completer.isCompleted) {
            completer.complete(
              NfcIdentityResult(
                method: IdentityNfcMethod.signedNdef,
                identityValue: json['tokenId']?.toString(),
                ndefProof: decoded,
              ),
            );
          }
        } catch (error, stackTrace) {
          if (!completer.isCompleted) {
            completer.completeError(error, stackTrace);
          }
        } finally {
          await NfcManager.instance.stopSession();
        }
      },
    );
    return completer.future.timeout(
      const Duration(seconds: 25),
      onTimeout: () async {
        await NfcManager.instance.stopSession();
        throw TimeoutException('Quá thời gian chờ thẻ NFC mô phỏng.');
      },
    );
  }

  static String _parseNdefTextPayload(List<int> payload) {
    if (payload.isEmpty) throw const FormatException('Payload NDEF rỗng.');
    final languageCodeLength = payload[0] & 0x3F;
    final start = 1 + languageCodeLength;
    if (start >= payload.length) {
      throw const FormatException('Payload NDEF Text không hợp lệ.');
    }
    return utf8.decode(payload.sublist(start));
  }
}
