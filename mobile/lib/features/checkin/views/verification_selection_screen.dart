import 'package:flutter/material.dart';
import 'identity_capture_screen.dart';
import 'ai_vision_verification_screen.dart';

class VerificationSelectionScreen extends StatefulWidget {
  final int totalGuests;
  const VerificationSelectionScreen({Key? key, required this.totalGuests}) : super(key: key);

  @override
  State<VerificationSelectionScreen> createState() => _VerificationSelectionScreenState();
}

class _VerificationSelectionScreenState extends State<VerificationSelectionScreen> {
  // Danh sách lưu trữ trạng thái xác minh của từng khách
  late List<Map<String, dynamic>?> _verifiedGuests;

  @override
  void initState() {
    super.initState();
    _verifiedGuests = List.generate(widget.totalGuests, (index) => null);
  }

  bool get _isAllVerified => _verifiedGuests.every((g) => g != null);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        title: const Text("Xác minh danh sách khách", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: colorScheme.surface,
        foregroundColor: colorScheme.onSurface,
        elevation: 0.5,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Text(
              "Phòng yêu cầu xác minh cho ${widget.totalGuests} khách",
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.blueGrey),
            ),
          ),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: widget.totalGuests,
              itemBuilder: (context, index) => _buildGuestStatusCard(index, colorScheme),
            ),
          ),
          _buildBottomAction(context, colorScheme),
        ],
      ),
    );
  }

  Widget _buildGuestStatusCard(int index, ColorScheme colorScheme) {
    bool isVerified = _verifiedGuests[index] != null;
    String method = _verifiedGuests[index]?['method'] ?? "";

    return Card(
      margin: const EdgeInsets.only(bottom: 15),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
      elevation: 2,
      child: ExpansionTile(
        initiallyExpanded: !isVerified,
        leading: CircleAvatar(
          backgroundColor: isVerified ? Colors.green : Colors.grey[300],
          child: Icon(
            isVerified ? Icons.check : Icons.person_outline,
            color: Colors.white,
          ),
        ),
        title: Text(
          "Khách hàng ${index + 1}",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: isVerified ? Colors.green : colorScheme.onSurface,
          ),
        ),
        subtitle: Text(
          isVerified ? "Đã xác minh qua $method" : "Chưa xác minh danh tính",
          style: TextStyle(fontSize: 12, color: isVerified ? Colors.green[700] : Colors.grey),
        ),
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                const Text("Chọn phương thức xác minh cho khách này:"),
                const SizedBox(height: 15),
                Row(
                  children: [
                    Expanded(
                      child: _buildSmallMethodButton(
                        context,
                        "Cách 1: NFC",
                        Icons.nfc,
                        Colors.blue,
                        () async {
                          final result = await Navigator.push(
                            context,
                            MaterialPageRoute(builder: (context) => const IdentityCaptureScreen(totalGuests: 1)),
                          );
                          if (result != null && result['status'] == 'success') {
                            setState(() {
                              _verifiedGuests[index] = {
                                'method': 'NFC',
                                'data': result['guestDataList'][0],
                              };
                            });
                          }
                        },
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _buildSmallMethodButton(
                        context,
                        "Cách 2: AI Vision",
                        Icons.remove_red_eye,
                        Colors.purple,
                        () async {
                          final result = await Navigator.push(
                            context,
                            MaterialPageRoute(builder: (context) => const AiVisionVerificationScreen(totalGuests: 1)),
                          );
                          if (result != null && result['status'] == 'success') {
                            setState(() {
                              _verifiedGuests[index] = {
                                'method': 'AI_VISION',
                                'data': result['rawData'],
                              };
                            });
                          }
                        },
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSmallMethodButton(BuildContext context, String label, IconData icon, Color color, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: color.withOpacity(0.5)),
        ),
        child: Column(
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }

  Widget _buildBottomAction(BuildContext context, ColorScheme colorScheme) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, -5))],
      ),
      child: ElevatedButton(
        onPressed: _isAllVerified ? () {
          Navigator.pop(context, {
            'status': 'success',
            'verifiedData': _verifiedGuests,
          });
        } : null,
        style: ElevatedButton.styleFrom(
          backgroundColor: colorScheme.primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(double.infinity, 55),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        ),
        child: Text(
          _isAllVerified ? "Xác nhận & Hoàn tất Check-in" : "Vui lòng xác minh hết tất cả khách",
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
    );
  }
}
