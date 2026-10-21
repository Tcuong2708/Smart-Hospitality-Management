import 'package:flutter/material.dart';

import '../services/management_api_service.dart';

class ManagerCatalogScreen extends StatelessWidget {
  const ManagerCatalogScreen({super.key});

  static const _modules = <_Module>[
    _Module('rooms', 'Phòng', Icons.meeting_room_outlined),
    _Module('room-types', 'Loại phòng', Icons.category_outlined),
    _Module('services', 'Dịch vụ', Icons.room_service_outlined),
    _Module('customers', 'Khách hàng', Icons.groups_outlined),
    _Module('reviews', 'Đánh giá', Icons.reviews_outlined),
    _Module('promotions', 'Khuyến mãi', Icons.local_offer_outlined),
    _Module(
      'loyalty-policy',
      'Chính sách tích điểm',
      Icons.workspace_premium_outlined,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Danh mục quản lý')),
      body: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _modules.length,
        separatorBuilder: (_, __) => const SizedBox(height: 8),
        itemBuilder: (context, index) {
          final module = _modules[index];
          return Card(
            child: ListTile(
              leading: Icon(module.icon),
              title: Text(module.title),
              subtitle: const Text('Dữ liệu đồng bộ từ MongoDB'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => module.resource == 'loyalty-policy'
                      ? const LoyaltyPolicyScreen()
                      : ManagementCollectionScreen(
                          resource: module.resource,
                          title: module.title,
                        ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

class ManagerStaffScreen extends StatelessWidget {
  const ManagerStaffScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ManagementCollectionScreen(
      resource: 'staff',
      title: 'Nhân sự',
    );
  }
}

class ManagementCollectionScreen extends StatefulWidget {
  const ManagementCollectionScreen({
    super.key,
    required this.resource,
    required this.title,
  });
  final String resource;
  final String title;

  @override
  State<ManagementCollectionScreen> createState() =>
      _ManagementCollectionScreenState();
}

class _ManagementCollectionScreenState
    extends State<ManagementCollectionScreen> {
  final _api = ManagementApiService();
  late Future<List<Map<String, dynamic>>> _future;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  void _reload() =>
      setState(() => _future = _api.getManagerResource(widget.resource));

  bool get _canCreate => !{'reviews'}.contains(widget.resource);
  bool get _canDelete => widget.resource != 'reviews';

  Future<void> _edit([Map<String, dynamic>? item]) async {
    final specs = _fieldSpecs(widget.resource, creating: item == null);
    if (specs.isEmpty) return;
    final controllers = <String, TextEditingController>{
      for (final spec in specs)
        spec.key: TextEditingController(
          text: item?[spec.key]?.toString() ?? spec.defaultValue,
        ),
    };
    final formKey = GlobalKey<FormState>();
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(
          item == null
              ? 'Thêm ${widget.title.toLowerCase()}'
              : 'Cập nhật ${widget.title.toLowerCase()}',
        ),
        content: Form(
          key: formKey,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: specs
                  .map(
                    (spec) => Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: TextFormField(
                        controller: controllers[spec.key],
                        obscureText: spec.secret,
                        keyboardType: spec.numeric
                            ? const TextInputType.numberWithOptions(
                                decimal: true,
                              )
                            : TextInputType.text,
                        decoration: InputDecoration(
                          labelText:
                              '${spec.label}${spec.required ? ' *' : ''}',
                        ),
                        validator: (value) =>
                            spec.required &&
                                (value == null || value.trim().isEmpty)
                            ? '${spec.label} không được để trống'
                            : null,
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () {
              if (formKey.currentState?.validate() == true)
                Navigator.pop(dialogContext, true);
            },
            child: const Text('Lưu'),
          ),
        ],
      ),
    );
    if (accepted != true) return;
    final payload = item == null
        ? <String, dynamic>{}
        : Map<String, dynamic>.from(item);
    for (final spec in specs) {
      final raw = controllers[spec.key]!.text.trim();
      payload[spec.key] = spec.numeric && raw.isNotEmpty
          ? num.tryParse(raw)
          : raw;
    }
    try {
      await _api.saveManagerResource(
        widget.resource,
        payload,
        id: int.tryParse(item?['id']?.toString() ?? ''),
      );
      if (!mounted) return;
      _reload();
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Đã lưu dữ liệu.')));
    } catch (error) {
      if (mounted)
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Không thể lưu: $error')));
    }
  }

  Future<void> _delete(Map<String, dynamic> item) async {
    final id = int.tryParse(item['id']?.toString() ?? '');
    if (id == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Xác nhận xóa'),
        content: Text(
          'Xóa "${_title(item)}"? Backend sẽ từ chối nếu dữ liệu đã được tham chiếu.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await _api.deleteManagerResource(widget.resource, id);
      if (mounted) _reload();
    } catch (error) {
      if (mounted)
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Không thể xóa: $error')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        actions: [
          IconButton(onPressed: _reload, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: FutureBuilder<List<Map<String, dynamic>>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(
              child: Text('Không tải được dữ liệu: ${snapshot.error}'),
            );
          }
          final items = snapshot.data ?? const [];
          if (items.isEmpty)
            return const Center(child: Text('Chưa có dữ liệu.'));
          return RefreshIndicator(
            onRefresh: () async => _reload(),
            child: ListView.separated(
              padding: const EdgeInsets.all(12),
              itemCount: items.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final item = items[index];
                return Card(
                  child: ListTile(
                    leading: CircleAvatar(
                      child: Text('${item['id'] ?? index + 1}'),
                    ),
                    title: Text(_title(item)),
                    subtitle: Text(
                      _subtitle(item),
                      maxLines: 3,
                      overflow: TextOverflow.ellipsis,
                    ),
                    trailing: PopupMenuButton<String>(
                      onSelected: (value) =>
                          value == 'edit' ? _edit(item) : _delete(item),
                      itemBuilder: (_) => [
                        const PopupMenuItem(
                          value: 'edit',
                          child: Text('Cập nhật'),
                        ),
                        if (_canDelete)
                          const PopupMenuItem(
                            value: 'delete',
                            child: Text('Xóa'),
                          ),
                      ],
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
      floatingActionButton: _canCreate
          ? FloatingActionButton(
              onPressed: () => _edit(),
              child: const Icon(Icons.add),
            )
          : null,
    );
  }

  String _title(Map<String, dynamic> item) {
    return (item['name'] ??
            item['fullName'] ??
            item['content'] ??
            'Bản ghi ${item['id'] ?? ''}')
        .toString();
  }

  String _subtitle(Map<String, dynamic> item) {
    const ignored = {
      'id',
      'name',
      'fullName',
      'content',
      'imageUrl',
      'roomType',
    };
    return item.entries
        .where(
          (entry) =>
              !ignored.contains(entry.key) &&
              entry.value != null &&
              entry.value.toString().isNotEmpty,
        )
        .take(4)
        .map((entry) => '${entry.key}: ${entry.value}')
        .join(' • ');
  }

  List<_FieldSpec> _fieldSpecs(String resource, {required bool creating}) {
    return switch (resource) {
      'rooms' => const [
        _FieldSpec('name', 'Tên phòng', required: true),
        _FieldSpec('price', 'Giá phòng', required: true, numeric: true),
        _FieldSpec(
          'roomTypeId',
          'Mã loại phòng',
          required: true,
          numeric: true,
        ),
        _FieldSpec(
          'status',
          'Trạng thái',
          required: true,
          defaultValue: 'AVAILABLE',
        ),
        _FieldSpec('detail', 'Mô tả'),
        _FieldSpec('note', 'Ghi chú'),
        _FieldSpec(
          'maxExtraBeds',
          'Số giường phụ',
          numeric: true,
          defaultValue: '0',
        ),
      ],
      'room-types' => const [
        _FieldSpec('name', 'Tên loại phòng', required: true),
        _FieldSpec('maxOccupancy', 'Sức chứa', required: true, numeric: true),
        _FieldSpec('quantity', 'Số lượng', numeric: true, defaultValue: '0'),
        _FieldSpec('imageUrl', 'Ảnh'),
      ],
      'services' => const [
        _FieldSpec('name', 'Tên dịch vụ', required: true),
        _FieldSpec('price', 'Đơn giá', required: true, numeric: true),
        _FieldSpec('unit', 'Đơn vị tính', required: true),
      ],
      'customers' => const [
        _FieldSpec('fullName', 'Họ tên', required: true),
        _FieldSpec('phone', 'Số điện thoại', required: true),
        _FieldSpec('email', 'Email'),
        _FieldSpec('cccd', 'CCCD/Hộ chiếu'),
        _FieldSpec('points', 'Điểm', numeric: true, defaultValue: '0'),
      ],
      'promotions' => const [
        _FieldSpec('name', 'Tên khuyến mãi', required: true),
        _FieldSpec(
          'discountPercent',
          'Phần trăm giảm',
          required: true,
          numeric: true,
        ),
        _FieldSpec('startDate', 'Ngày bắt đầu (yyyy-mm-dd)', required: true),
        _FieldSpec('endDate', 'Ngày kết thúc (yyyy-mm-dd)', required: true),
      ],
      'reviews' => const [
        _FieldSpec('status', 'Trạng thái (Hiện/Ẩn)', required: true),
        _FieldSpec('reply', 'Phản hồi'),
      ],
      'staff' => [
        const _FieldSpec('fullName', 'Họ tên', required: true),
        const _FieldSpec('phone', 'Số điện thoại', required: true),
        const _FieldSpec('position', 'Chức vụ', required: true),
        const _FieldSpec('gender', 'Giới tính'),
        if (creating) ...const [
          _FieldSpec('username', 'Tên đăng nhập', required: true),
          _FieldSpec('password', 'Mật khẩu', required: true, secret: true),
          _FieldSpec(
            'roleId',
            'Mã vai trò',
            required: true,
            numeric: true,
            defaultValue: '3',
          ),
        ],
      ],
      _ => const [],
    };
  }
}

class LoyaltyPolicyScreen extends StatefulWidget {
  const LoyaltyPolicyScreen({super.key});

  @override
  State<LoyaltyPolicyScreen> createState() => _LoyaltyPolicyScreenState();
}

class _LoyaltyPolicyScreenState extends State<LoyaltyPolicyScreen> {
  final _api = ManagementApiService();
  late Future<Map<String, dynamic>> _future = _api.getLoyaltyPolicy();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Chính sách tích điểm')),
      body: FutureBuilder<Map<String, dynamic>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting)
            return const Center(child: CircularProgressIndicator());
          if (snapshot.hasError)
            return Center(
              child: Text('Không tải được dữ liệu: ${snapshot.error}'),
            );
          final data = snapshot.data ?? const <String, dynamic>{};
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _policyTile('Số tiền cho 1 điểm', data['moneyPerPoint']),
              _policyTile('Mốc hạng Bạc', data['silverThreshold']),
              _policyTile('Mốc hạng Vàng', data['goldThreshold']),
              _policyTile('Mốc hạng Bạch Kim', data['platinumThreshold']),
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed: () => _edit(data),
                icon: const Icon(Icons.edit),
                label: const Text('Cập nhật'),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _policyTile(String label, dynamic value) => Card(
    child: ListTile(title: Text(label), trailing: Text('${value ?? 0}')),
  );

  Future<void> _edit(Map<String, dynamic> data) async {
    const keys = [
      'moneyPerPoint',
      'silverThreshold',
      'goldThreshold',
      'platinumThreshold',
    ];
    const labels = [
      'Số tiền cho 1 điểm',
      'Mốc Bạc',
      'Mốc Vàng',
      'Mốc Bạch Kim',
    ];
    final controllers = [
      for (final key in keys) TextEditingController(text: '${data[key] ?? 0}'),
    ];
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cập nhật chính sách'),
        content: SingleChildScrollView(
          child: Column(
            children: [
              for (var i = 0; i < keys.length; i++)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: TextField(
                    controller: controllers[i],
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(labelText: labels[i]),
                  ),
                ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Lưu'),
          ),
        ],
      ),
    );
    if (accepted != true) return;
    await _api.updateLoyaltyPolicy({
      for (var i = 0; i < keys.length; i++)
        keys[i]: num.tryParse(controllers[i].text),
    });
    if (mounted) setState(() => _future = _api.getLoyaltyPolicy());
  }
}

class _FieldSpec {
  const _FieldSpec(
    this.key,
    this.label, {
    this.required = false,
    this.numeric = false,
    this.secret = false,
    this.defaultValue = '',
  });
  final String key;
  final String label;
  final bool required;
  final bool numeric;
  final bool secret;
  final String defaultValue;
}

class _Module {
  const _Module(this.resource, this.title, this.icon);
  final String resource;
  final String title;
  final IconData icon;
}
