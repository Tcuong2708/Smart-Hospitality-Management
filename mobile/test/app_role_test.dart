import 'package:flutter_test/flutter_test.dart';
import 'package:may_hotel_app/features/auth/models/app_role.dart';

void main() {
  group('AppRole', () {
    test('maps every canonical backend role', () {
      expect(AppRole.fromApi(roleCode: 'ADMIN'), AppRole.admin);
      expect(AppRole.fromApi(roleCode: 'MANAGER'), AppRole.manager);
      expect(AppRole.fromApi(roleCode: 'RECEPTIONIST'), AppRole.receptionist);
      expect(AppRole.fromApi(roleCode: 'HOUSEKEEPER'), AppRole.housekeeper);
      expect(AppRole.fromApi(roleCode: 'CUSTOMER'), AppRole.customer);
      expect(AppRole.fromApi(roleCode: 'SERVICE_STAFF'), AppRole.serviceStaff);
      expect(AppRole.fromApi(roleCode: 'ACCOUNTANT'), AppRole.accountant);
    });

    test('uses roleId only as a compatibility fallback', () {
      expect(AppRole.fromApi(roleId: 1), AppRole.admin);
      expect(AppRole.fromApi(roleId: 2), AppRole.manager);
      expect(AppRole.fromApi(roleId: 3), AppRole.receptionist);
      expect(AppRole.fromApi(roleId: 4), AppRole.housekeeper);
      expect(AppRole.fromApi(roleId: 5), AppRole.customer);
      expect(AppRole.fromApi(roleId: 6), AppRole.serviceStaff);
      expect(AppRole.fromApi(roleId: 7), AppRole.accountant);
    });

    test('does not grant customer permissions to an unknown role', () {
      expect(AppRole.fromApi(roleCode: 'UNKNOWN'), AppRole.unsupported);
      expect(AppRole.fromStorage('UNKNOWN'), AppRole.unsupported);
      expect(AppRole.fromStorage(null), AppRole.guest);
    });
  });
}
