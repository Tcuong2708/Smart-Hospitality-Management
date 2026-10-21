import 'package:flutter/material.dart';

import '../services/mobile_operations_api.dart';

class AccountingReportScreen extends StatefulWidget {
  const AccountingReportScreen({super.key});

  @override
  State<AccountingReportScreen> createState() => _AccountingReportScreenState();
}

class _AccountingReportScreenState extends State<AccountingReportScreen> {
  final _api = MobileOperationsApi();
  DateTime _from = DateTime(DateTime.now().year, DateTime.now().month, 1);
  DateTime _to = DateTime.now();
  late Future<Map<String, dynamic>> _future = _load();
  bool _exporting = false;

  Future<Map<String, dynamic>> _load() => _api.getRevenueReport(from: _from, to: _to);
  void _reload() => setState(() => _future = _load());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Thống kê doanh thu'),
        actions: [
          IconButton(
            tooltip: 'Xuất CSV',
            onPressed: _exporting ? null : _export,
            icon: _exporting
                ? const SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.download_outlined),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: Row(
              children: [
                Expanded(child: _DateButton(label: 'Từ ngày', value: _from, onTap: () => _pick(true))),
                const SizedBox(width: 10),
                Expanded(child: _DateButton(label: 'Đến ngày', value: _to, onTap: () => _pick(false))),
                const SizedBox(width: 8),
                IconButton.filled(onPressed: _reload, icon: const Icon(Icons.search)),
              ],
            ),
          ),
          Expanded(
            child: FutureBuilder<Map<String, dynamic>>(
              future: _future,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snapshot.hasError) {
                  return Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(snapshot.error.toString(), textAlign: TextAlign.center)));
                }
                final data = snapshot.data ?? const <String, dynamic>{};
                final invoices = (data['invoices'] as List? ?? const []).whereType<Map>().toList();
                return RefreshIndicator(
                  onRefresh: () async => _reload(),
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      Row(
                        children: [
                          Expanded(child: _Metric(title: 'Doanh thu', value: _money(data['totalRevenue']), icon: Icons.payments_outlined)),
                          const SizedBox(width: 10),
                          Expanded(child: _Metric(title: 'Hóa đơn', value: '${data['paidInvoiceCount'] ?? 0}', icon: Icons.receipt_long_outlined)),
                        ],
                      ),
                      const SizedBox(height: 10),
                      _Metric(title: 'Trung bình hóa đơn', value: _money(data['averageInvoice']), icon: Icons.analytics_outlined),
                      const SizedBox(height: 22),
                      const Text('Hóa đơn đã thanh toán', style: TextStyle(fontSize: 19, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      if (invoices.isEmpty)
                        const Card(child: Padding(padding: EdgeInsets.all(24), child: Text('Không có dữ liệu doanh thu trong khoảng thời gian này.')))
                      else
                        ...invoices.map((raw) {
                          final item = Map<String, dynamic>.from(raw);
                          return Card(
                            child: ListTile(
                              leading: const CircleAvatar(child: Icon(Icons.receipt_outlined)),
                              title: Text('${item['guestName'] ?? 'Khách hàng'} · HĐ #${item['id']}'),
                              subtitle: Text('${_dateTime(item['createdAt'])} · ${item['payMethod'] ?? 'Chưa rõ'}'),
                              trailing: Text(_money(item['totalAmount']), style: const TextStyle(fontWeight: FontWeight.bold)),
                            ),
                          );
                        }),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _pick(bool start) async {
    final current = start ? _from : _to;
    final picked = await showDatePicker(
      context: context,
      initialDate: current,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
    );
    if (picked == null) return;
    setState(() {
      if (start) {
        _from = picked;
        if (_to.isBefore(_from)) _to = _from;
      } else {
        _to = picked;
        if (_from.isAfter(_to)) _from = _to;
      }
      _future = _load();
    });
  }

  Future<void> _export() async {
    setState(() => _exporting = true);
    try {
      final file = await _api.downloadRevenueCsv(from: _from, to: _to);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Đã xuất báo cáo: ${file.path}'), backgroundColor: Colors.green),
      );
    } catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Không thể xuất báo cáo: $error'), backgroundColor: Colors.red));
    } finally {
      if (mounted) setState(() => _exporting = false);
    }
  }

  String _money(dynamic raw) {
    final value = num.tryParse(raw?.toString() ?? '') ?? 0;
    final digits = value.round().toString();
    return '${digits.replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => '.')} đ';
  }

  String _dateTime(dynamic raw) {
    final value = DateTime.tryParse(raw?.toString() ?? '')?.toLocal();
    if (value == null) return 'Không rõ ngày';
    return '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
  }
}

class _DateButton extends StatelessWidget {
  const _DateButton({required this.label, required this.value, required this.onTap});
  final String label;
  final DateTime value;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => OutlinedButton.icon(
    onPressed: onTap,
    icon: const Icon(Icons.calendar_month_outlined, size: 18),
    label: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 10)),
        Text('${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}'),
      ],
    ),
  );
}

class _Metric extends StatelessWidget {
  const _Metric({required this.title, required this.value, required this.icon});
  final String title;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          CircleAvatar(child: Icon(icon)),
          const SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: const TextStyle(color: Colors.black54)), const SizedBox(height: 4), Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold))])),
        ],
      ),
    ),
  );
}
