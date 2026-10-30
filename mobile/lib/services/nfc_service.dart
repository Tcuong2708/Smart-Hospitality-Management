import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:cccd_vietnam/dmrtd.dart';
import 'package:flutter/foundation.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/nfc_manager_android.dart';

enum IdentityNfcMethod { realNfc, signedNdef, legacyNdef, aiOnly }

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
    this.faceImageMimeType,
    this.fullName,
    this.dateOfBirth,
    this.dateOfExpiry,
    this.nationality,
    this.gender,
    this.sodRead = false,
    this.activeAuthenticationAttempted = false,
  });

  final IdentityNfcMethod method;
  final String? identityValue;
  final String? ndefProof;
  final Uint8List? faceImageBytes;
  final String? faceImageMimeType;
  final String? fullName;
  final DateTime? dateOfBirth;
  final DateTime? dateOfExpiry;
  final String? nationality;
  final String? gender;
  final bool sodRead;
  final bool activeAuthenticationAttempted;

  bool get isLocalChipRead => method == IdentityNfcMethod.realNfc;
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
    if (!await isNfcAvailable()) {
      throw StateError('Thiết bị chưa bật hoặc không hỗ trợ NFC.');
    }
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
      final chipDocumentNumber = dg1.mrz.documentNumber
          .replaceAll('<', '')
          .trim()
          .toUpperCase();
      final requestedDocumentNumber = access.documentNumber
          .replaceAll('<', '')
          .trim()
          .toUpperCase();
      if (chipDocumentNumber != requestedDocumentNumber ||
          !_sameDate(dg1.mrz.dateOfBirth, access.dateOfBirth) ||
          !_sameDate(dg1.mrz.dateOfExpiry, access.dateOfExpiry)) {
        throw StateError(
          'Dữ liệu DG1 không khớp khóa truy cập đã nhập. Phiên đọc bị từ chối.',
        );
      }
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
        identityValue: chipDocumentNumber,
        faceImageBytes: faceBytes,
        faceImageMimeType: _detectFaceMimeType(faceBytes),
        fullName: '${dg1.mrz.lastName} ${dg1.mrz.firstName}'.trim(),
        dateOfBirth: dg1.mrz.dateOfBirth,
        dateOfExpiry: dg1.mrz.dateOfExpiry,
        nationality: dg1.mrz.nationality,
        gender: dg1.mrz.gender,
        sodRead: sodRead,
        activeAuthenticationAttempted: activeAuthenticationAttempted,
      );
    } catch (error) {
      throw StateError(_friendlyRealNfcError(error));
    } finally {
      try {
        await nfc.disconnect();
      } catch (error) {
        debugPrint('Không thể đóng phiên NFC ICAO: $error');
      }
    }
  }

  static String _detectFaceMimeType(Uint8List bytes) {
    if (bytes.length >= 12 &&
        bytes[0] == 0x00 &&
        bytes[1] == 0x00 &&
        bytes[2] == 0x00 &&
        bytes[3] == 0x0C &&
        bytes[4] == 0x6A &&
        bytes[5] == 0x50) {
      return 'image/jp2';
    }
    if (bytes.length >= 2 && bytes[0] == 0xFF && bytes[1] == 0xD8) {
      return 'image/jpeg';
    }
    return 'application/octet-stream';
  }

  static bool _sameDate(DateTime left, DateTime right) =>
      left.year == right.year &&
      left.month == right.month &&
      left.day == right.day;

  static String _friendlyRealNfcError(Object error) {
    final message = error.toString().toLowerCase();
    if (message.contains('tag was lost') || message.contains('tag lost')) {
      return 'Kết nối NFC bị ngắt. Giữ CCCD sát máy và thử lại.';
    }
    if (message.contains('timeout')) {
      return 'Quá thời gian đọc chip. Giữ CCCD ổn định lâu hơn và thử lại.';
    }
    if (message.contains('security status not satisfied') ||
        message.contains('bac') ||
        message.contains('pace')) {
      return 'Không mở được chip. Kiểm tra lại số giấy tờ, ngày sinh và ngày hết hạn.';
    }
    if (message.contains('transceive') || message.contains('iso-7816')) {
      return 'Không giao tiếp được với chip CCCD qua ISO-DEP.';
    }
    return 'Không đọc được chip CCCD trong chế độ nghiên cứu cục bộ.';
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
          final parsed = _parseDemoPayload(decoded);
          if (!completer.isCompleted) {
            completer.complete(parsed);
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

  /// Supports both the current signed JSON card and the original white demo
  /// card whose NDEF record contains labelled Vietnamese text. A legacy card
  /// is deliberately marked as unsigned; face verification remains mandatory.
  static NfcIdentityResult _parseDemoPayload(String decoded) {
    try {
      final json = jsonDecode(decoded);
      if (json is Map && json['issuer'] == 'MAY_HOTEL_DEMO') {
        final tokenId = json['tokenId']?.toString().trim();
        if (tokenId == null || tokenId.isEmpty) {
          throw const FormatException('Thẻ demo thiếu tokenId.');
        }
        return NfcIdentityResult(
          method: IdentityNfcMethod.signedNdef,
          identityValue: tokenId,
          ndefProof: decoded,
        );
      }
    } on FormatException {
      // Fall through to the legacy labelled-text parser.
    }

    final match = RegExp(
      r'(?:Số\s*CCCD|CCCD|Số\s*giấy\s*tờ)\s*:\s*([0-9A-Za-z]{6,20})',
      caseSensitive: false,
    ).firstMatch(decoded);
    final documentNumber = match?.group(1)?.trim().toUpperCase();
    if (documentNumber == null || documentNumber.isEmpty) {
      throw const FormatException(
        'Thẻ không phải NDEF MAY HOTEL ký số hoặc thẻ văn bản CCCD mô phỏng.',
      );
    }
    return NfcIdentityResult(
      method: IdentityNfcMethod.legacyNdef,
      identityValue: documentNumber,
    );
  }
}
