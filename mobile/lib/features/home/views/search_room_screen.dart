import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/constants/app_colors.dart';
import '../../../services/hotel_api_provider.dart';
import '../../../services/language_service.dart';
import '../../../services/auth_state_service.dart';
import '../../auth/views/login_screen.dart';
import '../widgets/room_card.dart';
import 'booking_screen.dart';

class SearchRoomScreen extends StatefulWidget {
  const SearchRoomScreen({Key? key}) : super(key: key);

  @override
  State<SearchRoomScreen> createState() => _SearchRoomScreenState();
}

class _SearchRoomScreenState extends State<SearchRoomScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  DateTimeRange? _selectedDateRange;
  List<dynamic> _searchResults = [];
  bool _isLoading = false;
  bool _hasSearched = false;
  int _adults = 2;
  int _children = 0;

  Future<void> _performSearch() async {
    if (_selectedDateRange == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Vui lòng chọn ngày nhận và trả phòng!"),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }

    setState(() {
      _isLoading = true;
      _hasSearched = true;
    });

    final results = await _apiProvider.searchRooms(_selectedDateRange!.start, _selectedDateRange!.end, _adults, _children);
    
    if (mounted) {
      setState(() {
        _searchResults = results;
        _isLoading = false;
      });
    }
  }

  void _navigateToBooking(String title, String price, String image, int id) {
    final authState = AuthStateService().currentAuth;
    final lang = LanguageService();

    if (!authState.isLoggedIn) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(lang.translate('booking_required'))),
      );
      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => const LoginScreen()),
      );
      return;
    }

    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => BookingScreen(
          roomTitle: title,
          roomPrice: price,
          imageUrl: image,
          maPhong: id,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        backgroundColor: colorScheme.surface,
        elevation: 0.5,
        centerTitle: true,
        title: Text(
          "Tra cứu phòng trống",
          style: TextStyle(
            color: colorScheme.onSurface,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
        systemOverlayStyle: SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
          statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
        ),
      ),
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: colorScheme.surface,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  blurRadius: 10,
                  offset: const Offset(0, 5),
                )
              ]
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  "Chọn thời gian lưu trú",
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: colorScheme.onSurface),
                ),
                const SizedBox(height: 15),
                InkWell(
                  onTap: () async {
                    final picked = await showDateRangePicker(
                      context: context,
                      firstDate: DateTime.now(),
                      lastDate: DateTime.now().add(const Duration(days: 365)),
                      builder: (context, child) {
                        return Theme(
                          data: Theme.of(context).copyWith(
                            colorScheme: colorScheme.copyWith(
                              primary: colorScheme.primary,
                              onPrimary: Colors.white,
                              surface: colorScheme.surface,
                              onSurface: colorScheme.onSurface,
                            ),
                          ),
                          child: child!,
                        );
                      },
                    );
                    if (picked != null) setState(() => _selectedDateRange = picked);
                  },
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                    decoration: BoxDecoration(
                      color: isDark ? Colors.grey.shade900 : Colors.grey.shade100,
                      borderRadius: BorderRadius.circular(15),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.calendar_month_outlined, color: isDark ? Colors.amber : colorScheme.primary, size: 22),
                        const SizedBox(width: 15),
                        Expanded(
                          child: Text(
                            _selectedDateRange == null
                                ? "Chọn ngày Check-in - Check-out"
                                : "${_selectedDateRange!.start.day}/${_selectedDateRange!.start.month} - ${_selectedDateRange!.end.day}/${_selectedDateRange!.end.month}",
                            style: TextStyle(
                                color: _selectedDateRange == null ? colorScheme.onSurface.withOpacity(0.4) : colorScheme.onSurface,
                                fontWeight: _selectedDateRange == null ? FontWeight.normal : FontWeight.bold,
                                fontSize: 14
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text("Người lớn", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: colorScheme.onSurface)),
                          const SizedBox(height: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12),
                            decoration: BoxDecoration(
                              color: isDark ? Colors.grey.shade900 : Colors.grey.shade100,
                              borderRadius: BorderRadius.circular(15),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                IconButton(
                                  icon: const Icon(Icons.remove_circle_outline),
                                  onPressed: () {
                                    if (_adults > 1) setState(() => _adults--);
                                  },
                                  color: colorScheme.primary,
                                ),
                                Text("$_adults", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                                IconButton(
                                  icon: const Icon(Icons.add_circle_outline),
                                  onPressed: () {
                                    if (_adults < 10) setState(() => _adults++);
                                  },
                                  color: colorScheme.primary,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 15),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text("Trẻ em", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: colorScheme.onSurface)),
                          const SizedBox(height: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12),
                            decoration: BoxDecoration(
                              color: isDark ? Colors.grey.shade900 : Colors.grey.shade100,
                              borderRadius: BorderRadius.circular(15),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                IconButton(
                                  icon: const Icon(Icons.remove_circle_outline),
                                  onPressed: () {
                                    if (_children > 0) setState(() => _children--);
                                  },
                                  color: colorScheme.primary,
                                ),
                                Text("$_children", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                                IconButton(
                                  icon: const Icon(Icons.add_circle_outline),
                                  onPressed: () {
                                    if (_children < 10) setState(() => _children++);
                                  },
                                  color: colorScheme.primary,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: _performSearch,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: colorScheme.primary,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                    ),
                    child: const Text("Tìm phòng", style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                  ),
                ),
              ],
            ),
          ),
          
          Expanded(
            child: _isLoading 
              ? const Center(child: CircularProgressIndicator())
              : (!_hasSearched)
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.search_rounded, size: 80, color: colorScheme.onSurface.withOpacity(0.1)),
                          const SizedBox(height: 16),
                          Text("Tra cứu phòng trống ngay", style: TextStyle(color: colorScheme.onSurface.withOpacity(0.5))),
                        ],
                      ),
                    )
                  : (_searchResults.isEmpty)
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.hotel_class_outlined, size: 80, color: colorScheme.onSurface.withOpacity(0.1)),
                              const SizedBox(height: 16),
                              Text("Không có phòng trống trong thời gian này", style: TextStyle(color: colorScheme.onSurface.withOpacity(0.5))),
                            ],
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.all(20),
                          itemCount: _searchResults.length,
                          itemBuilder: (context, index) {
                            final room = _searchResults[index];
                            String rawImg = room['imageUrl']?.toString() ?? "";
                            String processedImg = rawImg;
                            if (rawImg.isNotEmpty && !rawImg.startsWith('http')) {
                              final String baseUrl = dotenv.env['BASE_URL'] ?? "http://10.0.2.2:8080";
                              processedImg = "$baseUrl/images/$rawImg";
                            }
                            
                            return RoomCard(
                              title: room['name'],
                              location: "Sẵn sàng phục vụ",
                              price: room['price'].toString(),
                              rating: "5.0",
                              imageUrl: processedImg,
                              onAddTap: () {
                                _navigateToBooking(
                                  room['name'],
                                  room['price'].toString(),
                                  processedImg,
                                  room['id']
                                );
                              },
                            );
                          },
                        ),
          ),
        ],
      ),
    );
  }
}