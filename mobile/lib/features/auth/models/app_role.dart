enum AppRole {
  guest,
  customer,
  admin,
  manager,
  receptionist,
  housekeeper,
  serviceStaff,
  accountant,
  unsupported;

  static AppRole fromApi({Object? roleCode, Object? roleId}) {
    final normalized = roleCode?.toString().trim().toUpperCase() ?? '';
    switch (normalized) {
      case 'ADMIN':
        return AppRole.admin;
      case 'MANAGER':
        return AppRole.manager;
      case 'RECEPTIONIST':
      case 'STAFF':
        return AppRole.receptionist;
      case 'HOUSEKEEPER':
        return AppRole.housekeeper;
      case 'CUSTOMER':
        return AppRole.customer;
      case 'SERVICE_STAFF':
        return AppRole.serviceStaff;
      case 'ACCOUNTANT':
        return AppRole.accountant;
    }

    final id = int.tryParse(roleId?.toString() ?? '');
    return switch (id) {
      1 => AppRole.admin,
      2 => AppRole.manager,
      3 => AppRole.receptionist,
      4 => AppRole.housekeeper,
      5 => AppRole.customer,
      6 => AppRole.serviceStaff,
      7 => AppRole.accountant,
      _ => AppRole.unsupported,
    };
  }

  static AppRole fromStorage(String? value) {
    return switch (value?.trim().toLowerCase()) {
      'admin' => AppRole.admin,
      'manager' => AppRole.manager,
      'staff' || 'receptionist' => AppRole.receptionist,
      'housekeeper' => AppRole.housekeeper,
      'service_staff' => AppRole.serviceStaff,
      'customer' => AppRole.customer,
      'accountant' => AppRole.accountant,
      'guest' || null || '' => AppRole.guest,
      _ => AppRole.unsupported,
    };
  }

  String get storageValue => switch (this) {
    AppRole.guest => 'guest',
    AppRole.customer => 'customer',
    AppRole.admin => 'admin',
    AppRole.manager => 'manager',
    AppRole.receptionist => 'receptionist',
    AppRole.housekeeper => 'housekeeper',
    AppRole.serviceStaff => 'service_staff',
    AppRole.accountant => 'accountant',
    AppRole.unsupported => 'unsupported',
  };
}
