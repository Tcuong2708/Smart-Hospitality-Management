import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../services/management_api_service.dart';

class ManagerDashboardScreen extends StatefulWidget {
  const ManagerDashboardScreen({super.key, this.revenueFocus = false});

  final bool revenueFocus;

  @override
  State<ManagerDashboardScreen> createState() => _ManagerDashboardScreenState();
}

class _ManagerDashboardScreenState extends State<ManagerDashboardScreen> {
  final _api = ManagementApiService();
  late Future<Map<String, dynamic>> _future;

  @override
  void initState() {
    super.initState();
    _future = _api.getManagerDashboard();
  }

  void _reload() => setState(() => _future = _api.getManagerDashboard());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.revenueFocus ? 'Thống kê vận hành' : 'Tổng quan quản lý',
        ),
        actions: [
          IconButton(onPressed: _reload, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: FutureBuilder<Map<String, dynamic>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _ErrorState(
              message: snapshot.error.toString(),
              onRetry: _reload,
            );
          }
          final data = snapshot.data ?? const <String, dynamic>{};
          final entries = widget.revenueFocus
              ? <_Metric>[
                  _Metric(
                    'Doanh thu',
                    _money(data['revenue']),
                    Icons.payments_outlined,
                  ),
                  _Metric(
                    'Phiếu đặt',
                    '${data['bookings'] ?? 0}',
                    Icons.event_note_outlined,
                  ),
                  _Metric(
                    'Khách hàng',
                    '${data['customers'] ?? 0}',
                    Icons.groups_outlined,
                  ),
                  _Metric(
                    'Đánh giá',
                    '${data['reviews'] ?? 0}',
                    Icons.star_outline,
                  ),
                ]
              : <_Metric>[
                  _Metric(
                    'Phòng',
                    '${data['rooms'] ?? 0}',
                    Icons.meeting_room_outlined,
                  ),
                  _Metric(
                    'Loại phòng',
                    '${data['roomTypes'] ?? 0}',
                    Icons.category_outlined,
                  ),
                  _Metric(
                    'Dịch vụ',
                    '${data['services'] ?? 0}',
                    Icons.room_service_outlined,
                  ),
                  _Metric(
                    'Nhân sự',
                    '${data['staff'] ?? 0}',
                    Icons.badge_outlined,
                  ),
                ];
          return RefreshIndicator(
            onRefresh: () async => _reload(),
            child: GridView.builder(
              padding: const EdgeInsets.all(16),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                childAspectRatio: 1.15,
              ),
              itemCount: entries.length,
              itemBuilder: (context, index) =>
                  _MetricCard(metric: entries[index]),
            ),
          );
        },
      ),
    );
  }

  static String _money(dynamic value) {
    final amount = num.tryParse(value?.toString() ?? '') ?? 0;
    return '${amount.toStringAsFixed(0).replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (match) => '.')} đ';
  }
}

class AdminSystemDashboardScreen extends StatefulWidget {
  const AdminSystemDashboardScreen({super.key});

  @override
  State<AdminSystemDashboardScreen> createState() =>
      _AdminSystemDashboardScreenState();
}

class _AdminSystemDashboardScreenState
    extends State<AdminSystemDashboardScreen> {
  final _api = ManagementApiService();
  late Future<Map<String, dynamic>> _future = _api.getAdminDashboard();

  void _reload() => setState(() => _future = _api.getAdminDashboard());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tổng quan hệ thống'),
        actions: [
          IconButton(onPressed: _reload, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: FutureBuilder<Map<String, dynamic>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _ErrorState(
              message: snapshot.error.toString(),
              onRetry: _reload,
            );
          }
          final data = snapshot.data ?? const <String, dynamic>{};
          final metrics = [
            _Metric(
              'Tài khoản',
              '${data['users'] ?? 0}',
              Icons.manage_accounts_outlined,
            ),
            _Metric(
              'Đang hoạt động',
              '${data['activeUsers'] ?? 0}',
              Icons.verified_user_outlined,
            ),
            _Metric(
              'Bị khóa',
              '${data['lockedUsers'] ?? 0}',
              Icons.lock_outline,
            ),
            _Metric(
              'Lịch sử sao lưu',
              '${data['backupRecords'] ?? 0}',
              Icons.history,
            ),
          ];
          return GridView.builder(
            padding: const EdgeInsets.all(16),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              childAspectRatio: 1.15,
            ),
            itemCount: metrics.length,
            itemBuilder: (context, index) =>
                _MetricCard(metric: metrics[index]),
          );
        },
      ),
    );
  }
}

class _Metric {
  const _Metric(this.label, this.value, this.icon);
  final String label;
  final String value;
  final IconData icon;
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({required this.metric});
  final _Metric metric;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Icon(metric.icon, color: AppColors.accentGold, size: 32),
            Text(
              metric.value,
              style: Theme.of(
                context,
              ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
            ),
            Text(metric.label),
          ],
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_outlined, size: 48),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }
}
