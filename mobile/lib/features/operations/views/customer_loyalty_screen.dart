import 'package:flutter/material.dart';

import '../services/mobile_operations_api.dart';

class CustomerLoyaltyScreen extends StatefulWidget {
  const CustomerLoyaltyScreen({super.key});

  @override
  State<CustomerLoyaltyScreen> createState() => _CustomerLoyaltyScreenState();
}

class _CustomerLoyaltyScreenState extends State<CustomerLoyaltyScreen> {
  final _api = MobileOperationsApi();
  late Future<Map<String, dynamic>> _future = _api.getLoyalty();

  void _reload() => setState(() => _future = _api.getLoyalty());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Hạng thành viên'),
        actions: [IconButton(onPressed: _reload, icon: const Icon(Icons.refresh))],
      ),
      body: FutureBuilder<Map<String, dynamic>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _ErrorState(message: _message(snapshot.error), onRetry: _reload);
          }
          final data = snapshot.data ?? const <String, dynamic>{};
          final points = int.tryParse('${data['points'] ?? 0}') ?? 0;
          final next = int.tryParse('${data['nextTierPoints'] ?? 0}') ?? 0;
          final history = (data['history'] as List? ?? const [])
              .whereType<Map>()
              .map((item) => Map<String, dynamic>.from(item))
              .toList();
          return RefreshIndicator(
            onRefresh: () async => _reload(),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Container(
                  padding: const EdgeInsets.all(22),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF0D3150), Color(0xFF1B5A7A)],
                    ),
                    borderRadius: BorderRadius.circular(24),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.workspace_premium, color: Color(0xFFE5BC38), size: 42),
                      const SizedBox(height: 18),
                      Text(
                        'Hạng ${data['tier'] ?? 'Đồng'}',
                        style: const TextStyle(color: Colors.white, fontSize: 25, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 8),
                      Text('$points điểm', style: const TextStyle(color: Color(0xFFE5BC38), fontSize: 30, fontWeight: FontWeight.w800)),
                      const SizedBox(height: 6),
                      Text(
                        next > 0 ? 'Cần thêm $next điểm để đạt hạng tiếp theo' : 'Bạn đang ở hạng cao nhất',
                        style: const TextStyle(color: Colors.white70),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                const Text('Lịch sử điểm', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                const SizedBox(height: 10),
                if (history.isEmpty)
                  const Card(
                    child: Padding(
                      padding: EdgeInsets.all(22),
                      child: Text('Bạn chưa có giao dịch tích điểm nào!'),
                    ),
                  )
                else
                  ...history.map((item) {
                    final earned = item['type'] == 'EARN';
                    return Card(
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: earned ? Colors.green.shade50 : Colors.orange.shade50,
                          child: Icon(earned ? Icons.add : Icons.remove, color: earned ? Colors.green : Colors.orange),
                        ),
                        title: Text('Hóa đơn #${item['invoiceId'] ?? ''}'),
                        subtitle: Text(_formatDate(item['occurredAt'])),
                        trailing: Text(
                          '${earned ? '+' : ''}${item['points'] ?? 0}',
                          style: TextStyle(fontWeight: FontWeight.bold, color: earned ? Colors.green : Colors.orange),
                        ),
                      ),
                    );
                  }),
              ],
            ),
          );
        },
      ),
    );
  }

  String _formatDate(dynamic raw) {
    final date = DateTime.tryParse(raw?.toString() ?? '')?.toLocal();
    if (date == null) return 'Không rõ thời gian';
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  String _message(Object? error) {
    final text = error.toString();
    return text.replaceFirst(RegExp(r'^.*Exception:\s*'), '');
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: Colors.redAccent),
          const SizedBox(height: 12),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 16),
          FilledButton.icon(onPressed: onRetry, icon: const Icon(Icons.refresh), label: const Text('Thử lại')),
        ],
      ),
    ),
  );
}
