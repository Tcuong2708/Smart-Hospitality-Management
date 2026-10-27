import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:safe_device/safe_device.dart';

enum DeviceSecurityPlatform { android, ios, other }

enum DeviceSecurityIssue {
  bootloaderUnlocked,
  rootedOrJailbroken,
  usbDebugging,
  emulator,
  verificationFailed,
}

class AndroidSecuritySnapshot {
  const AndroidSecuritySnapshot({
    required this.deviceFingerprint,
    required this.isBootloaderUnlocked,
  });

  final String deviceFingerprint;
  final bool isBootloaderUnlocked;
}

class DeviceSecurityResult {
  const DeviceSecurityResult._({
    this.issue,
    this.deviceFingerprint,
    this.isTrustedDevice = false,
  });

  const DeviceSecurityResult.allowed({String? deviceFingerprint})
    : this._(deviceFingerprint: deviceFingerprint);

  const DeviceSecurityResult.trusted(String deviceFingerprint)
    : this._(deviceFingerprint: deviceFingerprint, isTrustedDevice: true);

  const DeviceSecurityResult.blocked(
    DeviceSecurityIssue issue, {
    String? deviceFingerprint,
  }) : this._(issue: issue, deviceFingerprint: deviceFingerprint);

  final DeviceSecurityIssue? issue;
  final String? deviceFingerprint;
  final bool isTrustedDevice;

  bool get isBlocked => issue != null;

  String? get message => switch (issue) {
    DeviceSecurityIssue.bootloaderUnlocked =>
      'Ứng dụng phát hiện thiết bị đã mở khóa bootloader. '
          'Để bảo đảm an toàn, ứng dụng sẽ không tiếp tục.',
    DeviceSecurityIssue.rootedOrJailbroken =>
      'Ứng dụng phát hiện thiết bị đã bị root hoặc jailbreak. '
          'Để bảo đảm an toàn, ứng dụng sẽ không tiếp tục.',
    DeviceSecurityIssue.usbDebugging =>
      'Ứng dụng phát hiện USB debugging đang được bật. '
          'Vui lòng tắt USB debugging trước khi sử dụng ứng dụng.',
    DeviceSecurityIssue.emulator =>
      'Ứng dụng phát hiện đang chạy trên máy ảo. '
          'Phiên bản này chỉ hỗ trợ thiết bị thật.',
    DeviceSecurityIssue.verificationFailed =>
      'Không thể xác minh trạng thái an toàn của thiết bị. '
          'Vui lòng khởi động lại ứng dụng hoặc liên hệ quản trị viên.',
    null => null,
  };
}

abstract class DeviceSecurityProbe {
  Future<AndroidSecuritySnapshot> getAndroidSnapshot();

  Future<bool> isRootedOrJailbroken();

  Future<bool> isUsbDebuggingEnabled();

  Future<bool> isRealDevice();
}

class PlatformDeviceSecurityProbe implements DeviceSecurityProbe {
  static const MethodChannel _platform = MethodChannel(
    'com.votricuong.mayhotel/security',
  );

  @override
  Future<AndroidSecuritySnapshot> getAndroidSnapshot() async {
    final values = await _platform.invokeMapMethod<Object?, Object?>(
      'getSecuritySnapshot',
    );
    final fingerprint = values?['deviceFingerprint']?.toString().toLowerCase();
    final bootloaderUnlocked = values?['isBootloaderUnlocked'];
    if (fingerprint == null ||
        !DeviceSecurityService.fingerprintPattern.hasMatch(fingerprint) ||
        bootloaderUnlocked is! bool) {
      throw const FormatException('Invalid Android security snapshot');
    }
    return AndroidSecuritySnapshot(
      deviceFingerprint: fingerprint,
      isBootloaderUnlocked: bootloaderUnlocked,
    );
  }

  @override
  Future<bool> isRealDevice() => SafeDevice.isRealDevice;

  @override
  Future<bool> isRootedOrJailbroken() => SafeDevice.isJailBroken;

  @override
  Future<bool> isUsbDebuggingEnabled() => SafeDevice.isUsbDebuggingEnabled;
}

class DeviceSecurityService {
  DeviceSecurityService({
    DeviceSecurityProbe? probe,
    this.trustedDeviceFingerprints = const String.fromEnvironment(
      'TRUSTED_DEVICE_FINGERPRINTS',
    ),
    DeviceSecurityPlatform? currentPlatform,
  }) : _probe = probe ?? PlatformDeviceSecurityProbe(),
       _currentPlatform = currentPlatform ?? _detectPlatform();

  static final RegExp fingerprintPattern = RegExp(r'^[0-9a-f]{64}$');
  static const int maximumTrustedDevices = 2;

  final DeviceSecurityProbe _probe;
  final String trustedDeviceFingerprints;
  final DeviceSecurityPlatform _currentPlatform;

  Future<DeviceSecurityResult> check() async {
    if (_currentPlatform == DeviceSecurityPlatform.android) {
      return _checkAndroid();
    }
    if (_currentPlatform == DeviceSecurityPlatform.ios) {
      try {
        return await _probe.isRootedOrJailbroken()
            ? const DeviceSecurityResult.blocked(
                DeviceSecurityIssue.rootedOrJailbroken,
              )
            : const DeviceSecurityResult.allowed();
      } catch (_) {
        return const DeviceSecurityResult.blocked(
          DeviceSecurityIssue.verificationFailed,
        );
      }
    }
    return const DeviceSecurityResult.allowed();
  }

  Future<DeviceSecurityResult> _checkAndroid() async {
    try {
      final snapshot = await _probe.getAndroidSnapshot();
      if (kDebugMode) {
        debugPrint(
          '[Security] Device fingerprint: ${snapshot.deviceFingerprint}',
        );
      }

      final trusted = parseTrustedFingerprints(trustedDeviceFingerprints);
      if (trusted.contains(snapshot.deviceFingerprint)) {
        debugPrint('[Security] Trusted test device accepted.');
        return DeviceSecurityResult.trusted(snapshot.deviceFingerprint);
      }

      final checks = await Future.wait<bool>([
        _probe.isRootedOrJailbroken(),
        _probe.isUsbDebuggingEnabled(),
        _probe.isRealDevice(),
      ]);
      if (snapshot.isBootloaderUnlocked) {
        return DeviceSecurityResult.blocked(
          DeviceSecurityIssue.bootloaderUnlocked,
          deviceFingerprint: snapshot.deviceFingerprint,
        );
      }
      if (checks[0]) {
        return DeviceSecurityResult.blocked(
          DeviceSecurityIssue.rootedOrJailbroken,
          deviceFingerprint: snapshot.deviceFingerprint,
        );
      }
      if (checks[1]) {
        return DeviceSecurityResult.blocked(
          DeviceSecurityIssue.usbDebugging,
          deviceFingerprint: snapshot.deviceFingerprint,
        );
      }
      if (!checks[2]) {
        return DeviceSecurityResult.blocked(
          DeviceSecurityIssue.emulator,
          deviceFingerprint: snapshot.deviceFingerprint,
        );
      }
      return DeviceSecurityResult.allowed(
        deviceFingerprint: snapshot.deviceFingerprint,
      );
    } catch (error) {
      debugPrint('[Security] Device verification failed: $error');
      return const DeviceSecurityResult.blocked(
        DeviceSecurityIssue.verificationFailed,
      );
    }
  }

  static Set<String> parseTrustedFingerprints(String rawValue) {
    final values = rawValue
        .split(',')
        .map((value) => value.trim().toLowerCase())
        .where((value) => value.isNotEmpty)
        .toSet();
    if (values.length > maximumTrustedDevices ||
        values.any((value) => !fingerprintPattern.hasMatch(value))) {
      throw const FormatException(
        'TRUSTED_DEVICE_FINGERPRINTS must contain at most two SHA-256 values',
      );
    }
    return values;
  }

  static DeviceSecurityPlatform _detectPlatform() {
    if (Platform.isAndroid) return DeviceSecurityPlatform.android;
    if (Platform.isIOS) return DeviceSecurityPlatform.ios;
    return DeviceSecurityPlatform.other;
  }
}
