import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';
import '../../../services/hotel_api_provider.dart';

// Khai báo lớp cấu trúc tin nhắn cục bộ
class ChatMessage {
  final String text;
  final bool isUser;
  final DateTime timestamp;

  ChatMessage({required this.text, required this.isUser, required this.timestamp});
}

class ChatScreen extends StatefulWidget {
  const ChatScreen({Key? key}) : super(key: key);

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final HotelApiProvider apiProvider = HotelApiProvider();
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  final List<ChatMessage> _messages = [];
  bool _isBotThinking = false;

  @override
  void initState() {
    super.initState();
    _messages.add(ChatMessage(
      text: "Xin chào! Tôi là May Bot - Trợ lý ảo của khách sạn. Bạn cần hỗ trợ thông tin gì ạ?",
      isUser: false,
      timestamp: DateTime.now(),
    ));
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  // Xử lý luồng gửi tin nhắn liên thông sang API Web Chatbot
  void _handleSendMessage() async {
    final String text = _messageController.text.trim();
    if (text.isEmpty) return;

    _messageController.clear();
    setState(() {
      _messages.add(ChatMessage(text: text, isUser: true, timestamp: DateTime.now()));
      _isBotThinking = true; // Kích hoạt hiệu ứng Bot đang suy nghĩ
    });
    _scrollToBottom();

    // Gọi API ném sang C# Web Chatbot backend
    final String botReply = await apiProvider.sendChatMessage(text);

    if (mounted) {
      setState(() {
        _messages.add(ChatMessage(text: botReply, isUser: false, timestamp: DateTime.now()));
        _isBotThinking = false; // Tắt hiệu ứng suy nghĩ
      });
      _scrollToBottom();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final bool isDark = theme.brightness == Brightness.dark;

    // ĐỒNG BỘ TRỤC MÀU LUXURY THEO APP_COLORS CLASS CỦA CƯỜNG
    final Color dynamicScaffoldBg = isDark ? AppColors.primaryDark : AppColors.background;
    final Color dynamicAppBarBg = isDark ? const Color(0xFF111111) : AppColors.primary;
    final Color dynamicInputBg = isDark ? const Color(0xFF242424) : Colors.white;
    final Color dynamicTextColor = isDark ? Colors.white : AppColors.primary;

    return Scaffold(
      backgroundColor: dynamicScaffoldBg,
      appBar: AppBar(
        backgroundColor: dynamicAppBarBg,
        elevation: 0,
        centerTitle: true,
        title: const Text(
          "Trợ lý ảo May Bot",
          style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.accentGold),
        ),
      ),
      body: Column(
        children: [
          // 1. Vùng hiển thị toàn bộ nội dung cuộc trò chuyện
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 16),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final msg = _messages[index];
                return _buildChatBubble(msg, isDark);
              },
            ),
          ),

          // Hiển thị chấm ba chấm động khi AI đang bốc dữ liệu câu trả lời
          if (_isBotThinking)
            Padding(
              padding: const EdgeInsets.only(left: 16, bottom: 8),
              child: Row(
                children: [
                  Text(
                    "May Bot đang suy nghĩ...",
                    style: const TextStyle(
                      color: AppColors.textSub,
                      fontSize: 13,
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                ],
              ),
            ),

          // 2. Thanh nhập liệu gác cổng ở đáy màn hình
          Container(
            padding: const EdgeInsets.all(12),
            color: isDark ? const Color(0xFF1A1A1A) : Colors.white,
            child: SafeArea(
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _messageController,
                      style: TextStyle(color: dynamicTextColor),
                      decoration: InputDecoration(
                        hintText: "Nhập câu hỏi của bạn...",
                        hintStyle: const TextStyle(color: AppColors.textSub),
                        filled: true,
                        fillColor: dynamicInputBg,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: BorderSide(color: isDark ? Colors.white10 : Colors.grey[300]!),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: const BorderSide(color: AppColors.accentGold, width: 1.5),
                        ),
                      ),
                      onSubmitted: (_) => _handleSendMessage(),
                    ),
                  ),
                  const SizedBox(width: 8),

                  // Nút bấm gửi tin nhắn phát quang màu Vàng Amber
                  GestureDetector(
                    onTap: _handleSendMessage,
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: const BoxDecoration(
                        color: AppColors.accentGold,
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.send, color: AppColors.primary, size: 20),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  // Widget vẽ bong bóng tin nhắn biệt lập Ngày/Đêm tương phản cao
  Widget _buildChatBubble(ChatMessage msg, bool isDark) {
    return Align(
      alignment: msg.isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.75),
        decoration: BoxDecoration(
          // Người dùng: Nền đen (Sáng) hoặc nền Vàng (Tối). Bot: Nền trắng (Sáng) hoặc Carbon (Tối)
          color: msg.isUser
              ? (isDark ? AppColors.accentGold : AppColors.primary)
              : (isDark ? const Color(0xFF242424) : Colors.white),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(msg.isUser ? 16 : 2),
            bottomRight: Radius.circular(msg.isUser ? 2 : 16),
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(isDark ? 0.2 : 0.03),
              blurRadius: 4,
              offset: const Offset(0, 2),
            )
          ],
        ),
        child: Text(
          msg.text,
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w400,
            // Khóa chặt màu tương phản: Đảm bảo chữ luôn nổi rực rỡ chống tàng hình
            color: msg.isUser
                ? (isDark ? AppColors.primary : Colors.white)
                : (isDark ? Colors.white : AppColors.primary),
          ),
        ),
      ),
    );
  }
}