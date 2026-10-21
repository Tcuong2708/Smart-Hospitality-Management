import 'package:flutter/material.dart';

import '../services/mobile_operations_api.dart';

class ReceptionOperationsScreen extends StatefulWidget {
  const ReceptionOperationsScreen({super.key});

  @override
  State<ReceptionOperationsScreen> createState() =>
      _ReceptionOperationsScreenState();
}

class _ReceptionOperationsScreenState extends State<ReceptionOperationsScreen>
    with SingleTickerProviderStateMixin {
  final _api = MobileOperationsApi();
  late final TabController _tabs;
  int _reloadToken = 0;

  @override
  void initState() {
    super.initState();
    _tabs = TabController(length: 5, vsync: this);
  }

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  void _reload() => setState(() => _reloadToken++);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Nghiệp vụ lễ tân'),
        actions: [
          IconButton(onPressed: _reload, icon: const Icon(Icons.refresh)),
        ],
        bottom: TabBar(
          controller: _tabs,
          isScrollable: true,
          tabs: const [
            Tab(text: 'Đặt trực tiếp'),
            Tab(text: 'Chờ nhận phòng'),
            Tab(text: 'Đang lưu trú'),
            Tab(text: 'Hóa đơn'),
            Tab(text: 'No-show'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabs,
        children: [
          _DirectBookingForm(
            key: ValueKey('direct-$_reloadToken'),
            api: _api,
            onCreated: _reload,
          ),
          _BookingList(
            key: ValueKey('pending-$_reloadToken'),
            api: _api,
            active: false,
            onChanged: _reload,
          ),
          _BookingList(
            key: ValueKey('active-$_reloadToken'),
            api: _api,
            active: true,
            onChanged: _reload,
          ),
          _InvoiceList(
            key: ValueKey('invoice-$_reloadToken'),
            api: _api,
            onChanged: _reload,
          ),
          _NoShowList(
            key: ValueKey('risk-$_reloadToken'),
            api: _api,
            onChanged: _reload,
          ),
        ],
      ),
    );
  }
}

class _DirectBookingForm extends StatefulWidget {
  const _DirectBookingForm({
    super.key,
    required this.api,
    required this.onCreated,
  });

  final MobileOperationsApi api;
  final VoidCallback onCreated;

  @override
  State<_DirectBookingForm> createState() => _DirectBookingFormState();
}

class _DirectBookingFormState extends State<_DirectBookingForm> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _note = TextEditingController();
  DateTime _checkIn = DateTime.now();
  DateTime _checkOut = DateTime.now().add(const Duration(days: 1));
  int _guestCount = 1;
  int? _roomTypeId;
  int? _roomId;
  List<Map<String, dynamic>> _roomTypes = [];
  List<Map<String, dynamic>> _rooms = [];
  bool _loading = true;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _loadRoomTypes();
  }

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _loadRoomTypes() async {
    try {
      final values = await widget.api.getReceptionRoomTypes();
      if (!mounted) return;
      setState(() {
        _roomTypes = values;
        _roomTypeId = values.isEmpty
            ? null
            : int.tryParse('${values.first['id']}');
        _loading = false;
      });
      await _loadRooms();
    } catch (error) {
      if (!mounted) return;
      setState(() => _loading = false);
      _message(_error(error), error: true);
    }
  }

  Future<void> _loadRooms() async {
    final typeId = _roomTypeId;
    if (typeId == null || !_checkOut.isAfter(_checkIn)) return;
    setState(() {
      _loading = true;
      _rooms = [];
      _roomId = null;
    });
    try {
      final rooms = await widget.api.getReceptionAvailableRooms(
        roomTypeId: typeId,
        checkIn: _checkIn,
        checkOut: _checkOut,
      );
      if (!mounted) return;
      setState(() {
        _rooms = rooms;
        _roomId = rooms.isEmpty ? null : int.tryParse('${rooms.first['id']}');
      });
    } catch (error) {
      if (mounted) _message(_error(error), error: true);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading && _roomTypes.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            'Tạo phiếu đặt phòng tại quầy',
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 6),
          const Text(
            'Phòng trống được kiểm tra theo đúng khoảng ngày; phiếu mới tự động gửi sang AI dự đoán no-show.',
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _name,
            textCapitalization: TextCapitalization.words,
            maxLength: 100,
            decoration: const InputDecoration(
              labelText: 'Họ và tên *',
              border: OutlineInputBorder(),
            ),
            validator: (value) => value == null || value.trim().length < 2
                ? 'Họ tên phải có ít nhất 2 ký tự'
                : null,
          ),
          const SizedBox(height: 10),
          TextFormField(
            controller: _phone,
            keyboardType: TextInputType.phone,
            maxLength: 20,
            decoration: const InputDecoration(
              labelText: 'Số điện thoại *',
              border: OutlineInputBorder(),
            ),
            validator: (value) =>
                !RegExp(
                  r'^\+?\d{9,15}$',
                ).hasMatch((value ?? '').replaceAll(RegExp(r'[\s\-()]'), ''))
                ? 'Số điện thoại phải có 9–15 chữ số'
                : null,
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(child: _dateButton('Nhận phòng', _checkIn, true)),
              const SizedBox(width: 10),
              Expanded(child: _dateButton('Trả phòng', _checkOut, false)),
            ],
          ),
          const SizedBox(height: 14),
          DropdownButtonFormField<int>(
            initialValue: _roomTypeId,
            decoration: const InputDecoration(
              labelText: 'Loại phòng *',
              border: OutlineInputBorder(),
            ),
            items: _roomTypes
                .map(
                  (type) => DropdownMenuItem<int>(
                    value: int.tryParse('${type['id']}'),
                    child: Text(
                      '${type['name']} · tối đa ${type['maxOccupancy']}',
                    ),
                  ),
                )
                .toList(),
            onChanged: (value) {
              setState(() => _roomTypeId = value);
              _loadRooms();
            },
          ),
          const SizedBox(height: 14),
          DropdownButtonFormField<int>(
            key: ValueKey('${_roomTypeId}_${_rooms.length}_$_roomId'),
            initialValue: _roomId,
            decoration: InputDecoration(
              labelText: _loading ? 'Đang tìm phòng...' : 'Phòng trống *',
              border: const OutlineInputBorder(),
            ),
            items: _rooms
                .map(
                  (room) => DropdownMenuItem<int>(
                    value: int.tryParse('${room['id']}'),
                    child: Text('${room['name']} · ${_money(room['price'])}'),
                  ),
                )
                .toList(),
            onChanged: _loading
                ? null
                : (value) => setState(() => _roomId = value),
            validator: (value) =>
                value == null ? 'Không có hoặc chưa chọn phòng' : null,
          ),
          const SizedBox(height: 14),
          DropdownButtonFormField<int>(
            initialValue: _guestCount,
            decoration: const InputDecoration(
              labelText: 'Số khách lưu trú *',
              border: OutlineInputBorder(),
            ),
            items: List.generate(
              10,
              (index) => DropdownMenuItem(
                value: index + 1,
                child: Text('${index + 1} khách'),
              ),
            ),
            onChanged: (value) => setState(() => _guestCount = value ?? 1),
          ),
          const SizedBox(height: 14),
          TextFormField(
            controller: _note,
            maxLength: 500,
            maxLines: 3,
            decoration: const InputDecoration(
              labelText: 'Ghi chú',
              border: OutlineInputBorder(),
            ),
          ),
          FilledButton.icon(
            onPressed: _submitting ? null : _submit,
            icon: _submitting
                ? const SizedBox.square(
                    dimension: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.add_business),
            label: const Text('Tạo phiếu đặt phòng'),
          ),
        ],
      ),
    );
  }

  Widget _dateButton(String label, DateTime value, bool isCheckIn) {
    return OutlinedButton.icon(
      onPressed: () async {
        final selected = await showDatePicker(
          context: context,
          initialDate: value,
          firstDate: DateTime.now().subtract(const Duration(days: 1)),
          lastDate: DateTime.now().add(const Duration(days: 730)),
        );
        if (selected == null || !mounted) return;
        setState(() {
          if (isCheckIn) {
            _checkIn = selected;
            if (!_checkOut.isAfter(selected)) {
              _checkOut = selected.add(const Duration(days: 1));
            }
          } else {
            _checkOut = selected;
          }
        });
        if (!_checkOut.isAfter(_checkIn)) {
          _message('Ngày trả phải sau ngày nhận.', error: true);
          return;
        }
        _loadRooms();
      },
      icon: const Icon(Icons.calendar_month),
      label: Text('$label\n${_shortDate(value)}', textAlign: TextAlign.center),
    );
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    if (!_checkOut.isAfter(_checkIn)) {
      _message('Ngày trả phải sau ngày nhận.', error: true);
      return;
    }
    setState(() => _submitting = true);
    try {
      await widget.api.createDirectBooking(
        guestName: _name.text.trim(),
        phone: _phone.text.replaceAll(RegExp(r'[\s\-()]'), ''),
        roomId: _roomId!,
        checkIn: _checkIn,
        checkOut: _checkOut,
        guestCount: _guestCount,
        note: _note.text.trim(),
      );
      if (!mounted) return;
      _message('Tạo phiếu thành công và đã gửi AI phân tích no-show.');
      _formKey.currentState?.reset();
      _name.clear();
      _phone.clear();
      _note.clear();
      widget.onCreated();
    } catch (error) {
      if (mounted) _message(_error(error), error: true);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _message(String value, {bool error = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(value),
        backgroundColor: error ? Colors.red : Colors.green,
      ),
    );
  }
}

class _BookingList extends StatefulWidget {
  const _BookingList({
    super.key,
    required this.api,
    required this.active,
    required this.onChanged,
  });
  final MobileOperationsApi api;
  final bool active;
  final VoidCallback onChanged;

  @override
  State<_BookingList> createState() => _BookingListState();
}

class _BookingListState extends State<_BookingList> {
  late Future<List<Map<String, dynamic>>> _future;

  @override
  void initState() {
    super.initState();
    _future = widget.api.getReceptionBookings(active: widget.active);
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<Map<String, dynamic>>>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return _ErrorView(error: snapshot.error, retry: _retry);
        }
        final items = snapshot.data ?? const [];
        if (items.isEmpty) {
          return Center(
            child: Text(
              widget.active
                  ? 'Không có khách đang lưu trú.'
                  : 'Không có phiếu chờ nhận phòng.',
            ),
          );
        }
        return RefreshIndicator(
          onRefresh: () async => _retry(),
          child: ListView.builder(
            padding: const EdgeInsets.all(12),
            itemCount: items.length,
            itemBuilder: (context, index) => _bookingCard(items[index]),
          ),
        );
      },
    );
  }

  Widget _bookingCard(Map<String, dynamic> item) {
    final verified = item['identityVerified'] == true;
    final invoice = item['invoice'] is Map
        ? Map<String, dynamic>.from(item['invoice'] as Map)
        : const <String, dynamic>{};
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    '${item['customerName'] ?? 'Khách hàng'} · #${item['bookingId']}',
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 17,
                    ),
                  ),
                ),
                _Badge(
                  text: '${item['status'] ?? ''}',
                  color: widget.active ? Colors.red : Colors.orange,
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              '${item['roomType'] ?? ''} · ${item['roomName'] ?? 'Chưa xếp phòng'}',
            ),
            Text(
              '${_date(item['expectedIn'])} → ${_date(item['expectedOut'])}',
            ),
            Text(
              'Khách: ${item['guestCount'] ?? 1} · Xác thực: ${item['verifiedGuestCount'] ?? 0}/${item['guestCount'] ?? 1}',
            ),
            Text(
              'Thanh toán: ${invoice['isPaid'] == true ? 'Đã thanh toán' : (invoice['payMethod'] ?? 'Chưa thanh toán')}',
            ),
            const SizedBox(height: 12),
            if (!widget.active)
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: verified
                      ? () => _assignRoom(item)
                      : () => _manualVerification(item),
                  icon: Icon(
                    verified ? Icons.key_outlined : Icons.badge_outlined,
                  ),
                  label: Text(
                    verified
                        ? 'Chọn phòng và duyệt nhận'
                        : 'Xác thực thủ công (ngoại lệ)',
                  ),
                ),
              )
            else
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  OutlinedButton.icon(
                    onPressed: () => _changeRoom(item),
                    icon: const Icon(Icons.swap_horiz),
                    label: const Text('Đổi phòng'),
                  ),
                  FilledButton.icon(
                    onPressed: () => _checkout(item),
                    icon: const Icon(Icons.logout),
                    label: const Text('Trả phòng'),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _manualVerification(Map<String, dynamic> booking) async {
    final id = int.tryParse('${booking['bookingId']}');
    if (id == null) return;
    final verified = int.tryParse('${booking['verifiedGuestCount']}') ?? 0;
    final total = int.tryParse('${booking['guestCount']}') ?? 1;
    final name = TextEditingController(
      text: verified == 0 ? '${booking['customerName'] ?? ''}' : '',
    );
    final document = TextEditingController();
    final nationality = TextEditingController(text: 'Việt Nam');
    final reason = TextEditingController();
    DateTime? dob;
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text('Xác thực khách ${verified + 1}/$total'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Card(
                  color: Color(0xFFFFF3CD),
                  child: Padding(
                    padding: EdgeInsets.all(10),
                    child: Text(
                      'Chỉ dùng khi khách nước ngoài, CCCD không hợp lệ hoặc không thể xác thực qua mobile.',
                    ),
                  ),
                ),
                TextField(
                  controller: name,
                  maxLength: 100,
                  decoration: const InputDecoration(labelText: 'Họ và tên *'),
                ),
                TextField(
                  controller: document,
                  maxLength: 30,
                  decoration: const InputDecoration(
                    labelText: 'Số hộ chiếu/giấy tờ *',
                  ),
                ),
                TextField(
                  controller: nationality,
                  maxLength: 60,
                  decoration: const InputDecoration(labelText: 'Quốc tịch *'),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Ngày sinh (không bắt buộc)'),
                  subtitle: Text(dob == null ? 'Chưa chọn' : _shortDate(dob!)),
                  trailing: const Icon(Icons.calendar_month),
                  onTap: () async {
                    final selected = await showDatePicker(
                      context: context,
                      initialDate: DateTime(1990),
                      firstDate: DateTime(1900),
                      lastDate: DateTime.now(),
                    );
                    if (selected != null) setDialogState(() => dob = selected);
                  },
                ),
                TextField(
                  controller: reason,
                  maxLength: 300,
                  maxLines: 2,
                  decoration: const InputDecoration(
                    labelText: 'Lý do xác thực thủ công *',
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Hủy'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Lưu xác thực'),
            ),
          ],
        ),
      ),
    );
    if (accepted != true) return;
    final cleanDocument = document.text.trim();
    if (name.text.trim().length < 2 ||
        !RegExp(r'^[A-Za-z0-9.-]{4,30}$').hasMatch(cleanDocument) ||
        nationality.text.trim().isEmpty ||
        reason.text.trim().isEmpty) {
      _message(
        'Vui lòng nhập đầy đủ và đúng định dạng thông tin xác thực.',
        error: true,
      );
      return;
    }
    try {
      final result = await widget.api.submitManualVerification(
        id,
        guestIndex: verified,
        fullName: name.text.trim(),
        documentNumber: cleanDocument,
        nationality: nationality.text.trim(),
        dob: dob,
        reason: reason.text.trim(),
      );
      if (!mounted) return;
      final verifiedCount = result['verifiedGuestCount'] ?? verified + 1;
      _message('Đã xác thực $verifiedCount/$total khách.');
      _retry();
      widget.onChanged();
    } catch (error) {
      if (mounted) _message(_error(error), error: true);
    }
  }

  Future<void> _assignRoom(Map<String, dynamic> booking) async {
    final id = int.tryParse('${booking['bookingId']}');
    if (id == null) return;
    try {
      final rooms = await widget.api.getAvailableRooms(id);
      if (!mounted) return;
      if (rooms.isEmpty) {
        _message(
          'Không có phòng đúng loại còn trống trong thời gian đã đặt.',
          error: true,
        );
        return;
      }
      int? selected = int.tryParse('${rooms.first['id']}');
      final accepted = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => StatefulBuilder(
          builder: (context, setDialogState) => AlertDialog(
            title: const Text('Gán phòng cho khách'),
            content: DropdownButtonFormField<int>(
              initialValue: selected,
              decoration: const InputDecoration(
                labelText: 'Phòng phù hợp',
                border: OutlineInputBorder(),
              ),
              items: rooms
                  .map(
                    (room) => DropdownMenuItem<int>(
                      value: int.tryParse('${room['id']}'),
                      child: Text(
                        '${room['name']} · ${room['roomType']} · ${_money(room['price'])}',
                      ),
                    ),
                  )
                  .toList(),
              onChanged: (value) => setDialogState(() => selected = value),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialogContext, false),
                child: const Text('Hủy'),
              ),
              FilledButton(
                onPressed: selected == null
                    ? null
                    : () => Navigator.pop(dialogContext, true),
                child: const Text('Duyệt nhận phòng'),
              ),
            ],
          ),
        ),
      );
      if (accepted != true || selected == null) return;
      await widget.api.assignRoom(id, selected!);
      if (mounted) {
        _message('Duyệt nhận phòng thành công.');
        widget.onChanged();
      }
    } catch (error) {
      if (mounted) _message(_error(error), error: true);
    }
  }

  Future<void> _changeRoom(Map<String, dynamic> booking) async {
    final id = int.tryParse('${booking['bookingId']}');
    if (id == null) return;
    try {
      final rooms = await widget.api.getChangeRoomOptions(id);
      if (!mounted) return;
      if (rooms.isEmpty) {
        return _message('Không có phòng trống khả dụng để đổi.', error: true);
      }
      int? selected = int.tryParse('${rooms.first['id']}');
      final reason = TextEditingController();
      final accepted = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => StatefulBuilder(
          builder: (context, setDialogState) => AlertDialog(
            title: const Text('Đổi phòng'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<int>(
                  initialValue: selected,
                  decoration: const InputDecoration(
                    labelText: 'Phòng mới',
                    border: OutlineInputBorder(),
                  ),
                  items: rooms
                      .map(
                        (room) => DropdownMenuItem<int>(
                          value: int.tryParse('${room['id']}'),
                          child: Text(
                            '${room['name']} · ${_money(room['price'])}',
                          ),
                        ),
                      )
                      .toList(),
                  onChanged: (value) => setDialogState(() => selected = value),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: reason,
                  maxLines: 2,
                  decoration: const InputDecoration(
                    labelText: 'Lý do đổi phòng *',
                    border: OutlineInputBorder(),
                  ),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialogContext, false),
                child: const Text('Hủy'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(dialogContext, true),
                child: const Text('Xác nhận'),
              ),
            ],
          ),
        ),
      );
      if (accepted != true || selected == null || reason.text.trim().isEmpty) {
        return;
      }
      await widget.api.changeRoom(
        id,
        roomId: selected!,
        reason: reason.text.trim(),
      );
      if (mounted) {
        _message('Đổi phòng thành công. Kiểm tra hóa đơn nếu có chênh lệch.');
        widget.onChanged();
      }
    } catch (error) {
      if (mounted) _message(_error(error), error: true);
    }
  }

  Future<void> _checkout(Map<String, dynamic> booking) async {
    final id = int.tryParse('${booking['bookingId']}');
    if (id == null) return;
    final surcharge = TextEditingController(text: '0');
    final damage = TextEditingController();
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Kiểm tra và trả phòng'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: surcharge,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Phụ thu/hư hỏng',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: damage,
              maxLines: 2,
              decoration: const InputDecoration(
                labelText: 'Ghi chú tình trạng phòng',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Xác nhận trả phòng'),
          ),
        ],
      ),
    );
    if (accepted != true) return;
    try {
      await widget.api.checkout(
        id,
        surcharge: double.tryParse(surcharge.text.trim()) ?? 0,
        damageNote: damage.text.trim(),
      );
      if (mounted) {
        _message('Trả phòng thành công; phòng đang chờ dọn.');
        widget.onChanged();
      }
    } catch (error) {
      if (mounted) _message(_error(error), error: true);
    }
  }

  void _retry() => setState(
    () => _future = widget.api.getReceptionBookings(active: widget.active),
  );
  void _message(String text, {bool error = false}) =>
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(text),
          backgroundColor: error ? Colors.red : Colors.green,
        ),
      );
}

class _InvoiceList extends StatefulWidget {
  const _InvoiceList({super.key, required this.api, required this.onChanged});
  final MobileOperationsApi api;
  final VoidCallback onChanged;

  @override
  State<_InvoiceList> createState() => _InvoiceListState();
}

class _InvoiceListState extends State<_InvoiceList> {
  final _search = TextEditingController();
  late Future<List<Map<String, dynamic>>> _future = widget.api.getInvoices();

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  void _reload() => setState(
    () => _future = widget.api.getInvoices(query: _search.text.trim()),
  );

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: TextField(
            controller: _search,
            onSubmitted: (_) => _reload(),
            decoration: InputDecoration(
              labelText: 'Tìm theo mã, khách hoặc số điện thoại',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: IconButton(
                onPressed: _reload,
                icon: const Icon(Icons.arrow_forward),
              ),
              border: const OutlineInputBorder(),
            ),
          ),
        ),
        Expanded(
          child: FutureBuilder<List<Map<String, dynamic>>>(
            future: _future,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snapshot.hasError) {
                return _ErrorView(error: snapshot.error, retry: _reload);
              }
              final values = snapshot.data ?? const [];
              if (values.isEmpty) {
                return const Center(child: Text('Không tìm thấy hóa đơn.'));
              }
              return ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 12),
                itemCount: values.length,
                itemBuilder: (context, index) {
                  final item = values[index];
                  final paid = item['isPaid'] == true;
                  return Card(
                    child: ListTile(
                      leading: CircleAvatar(
                        backgroundColor: paid
                            ? Colors.green.shade50
                            : Colors.orange.shade50,
                        child: Icon(
                          Icons.receipt_long,
                          color: paid ? Colors.green : Colors.orange,
                        ),
                      ),
                      title: Text(
                        '${item['guestName'] ?? 'Khách hàng'} · HĐ #${item['id']}',
                      ),
                      subtitle: Text(
                        '${item['phone'] ?? ''}\n${item['status'] ?? ''} · ${item['payMethod'] ?? 'Chưa chọn'}',
                      ),
                      isThreeLine: true,
                      trailing: paid
                          ? Text(
                              _money(item['totalAmount']),
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                              ),
                            )
                          : FilledButton(
                              onPressed: () => _cash(item),
                              child: const Text('Thu tiền'),
                            ),
                    ),
                  );
                },
              );
            },
          ),
        ),
      ],
    );
  }

  Future<void> _cash(Map<String, dynamic> invoice) async {
    final id = int.tryParse('${invoice['id']}');
    if (id == null) return;
    final amount = TextEditingController(
      text: '${(num.tryParse('${invoice['totalAmount']}') ?? 0).round()}',
    );
    bool earnPoints = true;
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Thanh toán tiền mặt'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Cần thu: ${_money(invoice['totalAmount'])}',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: amount,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Số tiền nhận từ khách',
                  border: OutlineInputBorder(),
                ),
              ),
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                value: earnPoints,
                onChanged: (value) =>
                    setDialogState(() => earnPoints = value ?? false),
                title: const Text('Khách đồng ý tích điểm'),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Hủy'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Đã nhận tiền'),
            ),
          ],
        ),
      ),
    );
    if (accepted != true) return;
    try {
      final result = await widget.api.payCash(
        id,
        amountReceived: double.tryParse(amount.text.trim()) ?? 0,
        earnPoints: earnPoints,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Thanh toán thành công. Tiền thối: ${_money(result['change'])}',
          ),
          backgroundColor: Colors.green,
        ),
      );
      _reload();
      widget.onChanged();
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(_error(error)), backgroundColor: Colors.red),
        );
      }
    }
  }
}

class _NoShowList extends StatefulWidget {
  const _NoShowList({super.key, required this.api, required this.onChanged});
  final MobileOperationsApi api;
  final VoidCallback onChanged;

  @override
  State<_NoShowList> createState() => _NoShowListState();
}

class _NoShowListState extends State<_NoShowList> {
  late Future<List<Map<String, dynamic>>> _future = widget.api
      .getNoShowBookings();

  @override
  Widget build(
    BuildContext context,
  ) => FutureBuilder<List<Map<String, dynamic>>>(
    future: _future,
    builder: (context, snapshot) {
      if (snapshot.connectionState == ConnectionState.waiting) {
        return const Center(child: CircularProgressIndicator());
      }
      if (snapshot.hasError) {
        return _ErrorView(error: snapshot.error, retry: _reload);
      }
      final values = snapshot.data ?? const [];
      if (values.isEmpty) {
        return const Center(
          child: Text('Không có cảnh báo no-show đang hoạt động.'),
        );
      }
      return ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: values.length,
        itemBuilder: (context, index) {
          final item = values[index];
          final high = '${item['riskLevel']}'.toLowerCase().contains('cao');
          return Card(
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: high
                    ? Colors.red.shade50
                    : Colors.orange.shade50,
                child: Icon(
                  Icons.warning_amber,
                  color: high ? Colors.red : Colors.orange,
                ),
              ),
              title: Text('${item['customerName']} · #${item['bookingId']}'),
              subtitle: Text(
                'Rủi ro: ${item['riskLevel'] ?? 'Đang phân tích'} (${item['riskPercentage'] ?? 0}%)\nTự hủy: ${_dateTime(item['autoCancelTime'])}',
              ),
              isThreeLine: true,
              trailing: TextButton(
                onPressed: () => _approve(item),
                child: const Text('Đến trễ'),
              ),
            ),
          );
        },
      );
    },
  );

  Future<void> _approve(Map<String, dynamic> item) async {
    final id = int.tryParse('${item['bookingId']}');
    if (id == null) return;
    final reason = TextEditingController();
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Xác nhận khách đến trễ'),
        content: TextField(
          controller: reason,
          maxLines: 2,
          decoration: const InputDecoration(
            labelText: 'Lý do/giờ dự kiến',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Dừng tự hủy'),
          ),
        ],
      ),
    );
    if (accepted != true || reason.text.trim().isEmpty) return;
    try {
      await widget.api.approveLateArrival(id, reason.text.trim());
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã dừng tự hủy cho phiếu này.'),
            backgroundColor: Colors.green,
          ),
        );
        _reload();
        widget.onChanged();
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(_error(error)), backgroundColor: Colors.red),
        );
      }
    }
  }

  void _reload() => setState(() => _future = widget.api.getNoShowBookings());
}

class _Badge extends StatelessWidget {
  const _Badge({required this.text, required this.color});
  final String text;
  final Color color;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
    decoration: BoxDecoration(
      color: color.withValues(alpha: .12),
      borderRadius: BorderRadius.circular(20),
    ),
    child: Text(
      text,
      style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold),
    ),
  );
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.error, required this.retry});
  final Object? error;
  final VoidCallback retry;
  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: Colors.red),
          const SizedBox(height: 12),
          Text(_error(error), textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: retry,
            icon: const Icon(Icons.refresh),
            label: const Text('Thử lại'),
          ),
        ],
      ),
    ),
  );
}

String _date(dynamic raw) {
  final value = DateTime.tryParse(raw?.toString() ?? '')?.toLocal();
  if (value == null) return 'Chưa rõ ngày';
  return '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
}

String _shortDate(DateTime value) =>
    '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';

String _dateTime(dynamic raw) {
  final value = DateTime.tryParse(raw?.toString() ?? '')?.toLocal();
  if (value == null) return 'Chưa xác định';
  return '${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')} ${_date(raw)}';
}

String _money(dynamic raw) {
  final value = num.tryParse(raw?.toString() ?? '') ?? 0;
  final digits = value.round().toString();
  return '${digits.replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => '.')} đ';
}

String _error(Object? error) {
  final text = error.toString();
  final match = RegExp(r'"message"\s*:\s*"([^"]+)"').firstMatch(text);
  return match?.group(1) ?? text.replaceFirst(RegExp(r'^.*Exception:\s*'), '');
}
