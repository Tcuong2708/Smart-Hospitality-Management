import 'package:flutter_test/flutter_test.dart';
import 'package:may_hotel_app/services/device_security_service.dart';

void main() {
  const phoneOne =
      '1111111111111111111111111111111111111111111111111111111111111111';
  const phoneTwo =
      '2222222222222222222222222222222222222222222222222222222222222222';
  const otherDevice =
      'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';

  group('trusted device configuration', () {
    test('accepts at most two SHA-256 fingerprints', () {
      expect(
        DeviceSecurityService.parseTrustedFingerprints(
          ' $phoneOne,${phoneTwo.toUpperCase()} ',
        ),
        {phoneOne, phoneTwo},
      );
    });

    test('rejects a third trusted device', () {
      expect(
        () => DeviceSecurityService.parseTrustedFingerprints(
          '$phoneOne,$phoneTwo,$otherDevice',
        ),
        throwsFormatException,
      );
    });

    test('rejects malformed fingerprints', () {
      expect(
        () => DeviceSecurityService.parseTrustedFingerprints('phone-1'),
        throwsFormatException,
      );
    });
  });

  group('Android security policy', () {
    test(
      'trusted phone allows USB debugging but still runs scanners',
      () async {
        final probe = _FakeSecurityProbe(
          fingerprint: phoneOne,
          usbDebugging: true,
        );
        final result = await DeviceSecurityService(
          probe: probe,
          currentPlatform: DeviceSecurityPlatform.android,
          trustedDeviceFingerprints: '$phoneOne,$phoneTwo',
        ).check();

        expect(result.isBlocked, isFalse);
        expect(result.isTrustedDevice, isTrue);
        expect(probe.rootChecks, 1);
        expect(probe.usbChecks, 1);
        expect(probe.realDeviceChecks, 1);
      },
    );

    test('trusted phone with unlocked bootloader is still blocked', () async {
      final probe = _FakeSecurityProbe(
        fingerprint: phoneOne,
        bootloaderUnlocked: true,
        usbDebugging: true,
      );
      final result = await DeviceSecurityService(
        probe: probe,
        currentPlatform: DeviceSecurityPlatform.android,
        trustedDeviceFingerprints: phoneOne,
      ).check();

      expect(result.issue, DeviceSecurityIssue.bootloaderUnlocked);
    });

    test('untrusted emulator is blocked', () async {
      final probe = _FakeSecurityProbe(
        fingerprint: otherDevice,
        realDevice: false,
      );
      final result = await DeviceSecurityService(
        probe: probe,
        currentPlatform: DeviceSecurityPlatform.android,
        trustedDeviceFingerprints: '$phoneOne,$phoneTwo',
      ).check();

      expect(result.issue, DeviceSecurityIssue.emulator);
    });

    test('untrusted phone with USB debugging is blocked', () async {
      final probe = _FakeSecurityProbe(
        fingerprint: otherDevice,
        usbDebugging: true,
      );
      final result = await DeviceSecurityService(
        probe: probe,
        currentPlatform: DeviceSecurityPlatform.android,
        trustedDeviceFingerprints: '$phoneOne,$phoneTwo',
      ).check();

      expect(result.issue, DeviceSecurityIssue.usbDebugging);
    });

    test(
      'development build accepts USB debugging but keeps other checks',
      () async {
        final probe = _FakeSecurityProbe(
          fingerprint: otherDevice,
          usbDebugging: true,
        );
        final result = await DeviceSecurityService(
          probe: probe,
          currentPlatform: DeviceSecurityPlatform.android,
          trustedDeviceFingerprints: '$phoneOne,$phoneTwo',
          allowUsbDebuggingForDevelopment: true,
        ).check();

        expect(result.isBlocked, isFalse);
        expect(result.isTrustedDevice, isFalse);
      },
    );

    test('bootloader warning has priority over other findings', () async {
      final probe = _FakeSecurityProbe(
        fingerprint: otherDevice,
        bootloaderUnlocked: true,
        rooted: true,
        usbDebugging: true,
      );
      final result = await DeviceSecurityService(
        probe: probe,
        currentPlatform: DeviceSecurityPlatform.android,
        trustedDeviceFingerprints: '$phoneOne,$phoneTwo',
      ).check();

      expect(result.issue, DeviceSecurityIssue.bootloaderUnlocked);
    });
  });
}

class _FakeSecurityProbe implements DeviceSecurityProbe {
  _FakeSecurityProbe({
    required this.fingerprint,
    this.bootloaderUnlocked = false,
    this.rooted = false,
    this.usbDebugging = false,
    this.realDevice = true,
  });

  final String fingerprint;
  final bool bootloaderUnlocked;
  final bool rooted;
  final bool usbDebugging;
  final bool realDevice;

  int rootChecks = 0;
  int usbChecks = 0;
  int realDeviceChecks = 0;

  @override
  Future<AndroidSecuritySnapshot> getAndroidSnapshot() async {
    return AndroidSecuritySnapshot(
      deviceFingerprint: fingerprint,
      isBootloaderUnlocked: bootloaderUnlocked,
    );
  }

  @override
  Future<bool> isRealDevice() async {
    realDeviceChecks++;
    return realDevice;
  }

  @override
  Future<bool> isRootedOrJailbroken() async {
    rootChecks++;
    return rooted;
  }

  @override
  Future<bool> isUsbDebuggingEnabled() async {
    usbChecks++;
    return usbDebugging;
  }
}
