import 'package:flutter/material.dart';
import 'package:may_hotel_app/core/constants/app_colors.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final HotelApiProvider _api = HotelApiProvider();
  List<dynamic> _notifications = <dynamic>[];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadNotifications();
  }

  Future<void> _loadNotifications() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final notifications = await _api.getUserNotifications();
      if (!mounted) return;
      setState(() {
        _notifications = notifications;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Không thể tải thông báo. Vui lòng thử lại.';
      });
    }
  }

  Future<void> _markAllAsRead() async {
    try {
      await _api.markAllUserNotificationsAsRead();
      await _loadNotifications();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể cập nhật thông báo.')),
        );
      }
    }
  }

  String _date(dynamic value) {
    if (value == null) return '';
    final parsed = DateTime.tryParse(value.toString());
    if (parsed == null) return value.toString();
    final local = parsed.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/'
        '${local.year} ${local.hour.toString().padLeft(2, '0')}:'
        '${local.minute.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final unread = _notifications.where((item) {
      return item is Map && item['isRead'] != true;
    }).length;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.accentGold,
        title: const Text(
          'Thông báo',
          style: TextStyle(
            color: AppColors.accentGold,
            fontWeight: FontWeight.bold,
          ),
        ),
        actions: [
          if (unread > 0)
            TextButton(
              onPressed: _markAllAsRead,
              child: const Text(
                'Đọc tất cả',
                style: TextStyle(color: AppColors.accentGold),
              ),
            ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadNotifications,
        color: AppColors.accentGold,
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
            ? ListView(
                children: [
                  Padding(
                    padding: const EdgeInsets.all(32),
                    child: Center(child: Text(_error!)),
                  ),
                ],
              )
            : _notifications.isEmpty
            ? ListView(
                children: const [
                  SizedBox(height: 180),
                  Icon(
                    Icons.notifications_none_outlined,
                    size: 64,
                    color: AppColors.accentGold,
                  ),
                  SizedBox(height: 12),
                  Center(child: Text('Chưa có thông báo')),
                ],
              )
            : ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: _notifications.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  final item = Map<String, dynamic>.from(
                    _notifications[index] as Map,
                  );
                  final isUnread = item['isRead'] != true;
                  return Container(
                    decoration: BoxDecoration(
                      color: isUnread
                          ? AppColors.accentGold.withValues(alpha: 0.1)
                          : Theme.of(context).colorScheme.surface,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(
                        color: isUnread
                            ? AppColors.accentGold.withValues(alpha: 0.55)
                            : Colors.grey.withValues(alpha: 0.18),
                      ),
                    ),
                    child: ListTile(
                      contentPadding: const EdgeInsets.all(14),
                      leading: CircleAvatar(
                        backgroundColor: AppColors.primary,
                        foregroundColor: AppColors.accentGold,
                        child: Icon(
                          isUnread
                              ? Icons.notifications_active_outlined
                              : Icons.notifications_none_outlined,
                        ),
                      ),
                      title: Text(
                        item['title']?.toString() ?? 'Thông báo',
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                      subtitle: Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Text(
                          '${item['message'] ?? ''}\n${_date(item['createdAt'])}',
                        ),
                      ),
                    ),
                  );
                },
              ),
      ),
    );
  }
}
