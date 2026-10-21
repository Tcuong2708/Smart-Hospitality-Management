import 'package:flutter/material.dart';

import '../services/management_api_service.dart';

class AdminBackupScreen extends StatelessWidget {
  const AdminBackupScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sao lưu và phục hồi')),
      body: FutureBuilder<Map<String, dynamic>>(
        future: ManagementApiService().getBackupStatus(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError)
            return Center(
              child: Text('Không tải được trạng thái: ${snapshot.error}'),
            );
          final data = snapshot.data ?? const <String, dynamic>{};
          final history = data['history'] as List? ?? const [];
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Row(
                        children: [
                          Icon(Icons.info_outline),
                          SizedBox(width: 8),
                          Text('Trạng thái backend'),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Text(
                        data['message']?.toString() ??
                            'Chưa có thông tin cấu hình.',
                      ),
                      const SizedBox(height: 12),
                      const Text(
                        'Thao tác sao lưu/phục hồi được khóa cho đến khi máy chủ cấu hình mongodump, mongorestore và thư mục lưu an toàn.',
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text('Lịch sử', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              if (history.isEmpty)
                const Card(
                  child: ListTile(title: Text('Chưa có bản ghi sao lưu')),
                )
              else
                ...history.whereType<Map>().map((raw) {
                  final item = Map<String, dynamic>.from(raw);
                  return Card(
                    child: ListTile(
                      leading: const Icon(Icons.storage_outlined),
                      title: Text(
                        item['fileName']?.toString() ?? 'Bản sao lưu',
                      ),
                      subtitle: Text(
                        '${item['backupDate'] ?? ''} • ${item['sizeMb'] ?? 0} MB',
                      ),
                      trailing: Text(item['status']?.toString() ?? ''),
                    ),
                  );
                }),
            ],
          );
        },
      ),
    );
  }
}
