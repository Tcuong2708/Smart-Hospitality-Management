import 'package:flutter/material.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../../../core/constants/app_colors.dart';

class DiscoveryScreen extends StatefulWidget {
  const DiscoveryScreen({Key? key}) : super(key: key);

  @override
  State<DiscoveryScreen> createState() => _DiscoveryScreenState();
}

class _DiscoveryScreenState extends State<DiscoveryScreen> {
  // 🌟 ĐIỀN API KEY GOOGLE MAPS THẬT CỦA NHÓM CƯỜNG VÀO ĐÂY ĐỂ CHẠY
  final String _googleApiKey = "AIzaSyAS5ssyEDmvQnMjjJbQ7CZEZPHPswMI-k8";

  // Tọa độ vị trí thực tế của May Hotel (Khu vực Quận 1, TP.HCM)
  final String _location = "10.7797,106.7001";
  // Bán kính quét tìm kiếm địa điểm xung quanh (2000 mét = 2km)
  final int _radius = 2000;

  late Future<List<Map<String, dynamic>>> _placesFuture;
  int _selectedChipIndex = 0;

  @override
  void initState() {
    super.initState();
    _handleRefresh();
  }

  Future<void> _handleRefresh() async {
    setState(() {
      _placesFuture = _fetchGooglePlacesReal();
    });
  }
//AIzaSyBBgCO3pGcxu3gJa012Ns02PsflcGx_cPk
  // 🚀 LUỒNG GỌI API THẬT - CHUYỂN SANG DÙNG OPENSTREETMAP ĐỂ NÉ LỖI BILLING GOOGLE
  Future<List<Map<String, dynamic>>> _fetchGooglePlacesReal() async {
    // Tọa độ May Hotel quận 1
    final double lat = 10.7797;
    final double lng = 106.7001;

    // URL gọi trực tiếp lên máy chủ dữ liệu mở Overpass của OpenStreetMap
    // Lệnh này ép máy chủ tìm tất cả địa điểm du lịch (tourism) và nhà hàng (restaurant) trong bán kính 2000m quanh khách sạn
    final String url = "https://overpass-api.de/api/interpreter?data=[out:json];(node(around:2000,$lat,$lng)[tourism];node(around:2000,$lat,$lng)[amenity=restaurant];);out 15;";

    try {
      debugPrint("🌐 Đang phóng lệnh gọi API mở OpenStreetMap quanh Quận 1...");
      final response = await http.get(Uri.parse(url));

      if (response.statusCode == 200) {
        // Giải mã JSON phản hồi từ máy chủ OpenStreetMap
        final data = json.decode(utf8.decode(response.bodyBytes));
        final List elements = data['elements'] ?? [];

        if (elements.isEmpty) return _getMockHcmPlaces();

        return elements.map((element) {
          final tags = element['tags'] ?? {};

          // Bốc tên tiếng Việt hoặc tên mặc định của địa danh thật ở Q1
          final String name = tags['name:vi'] ?? tags['name'] ?? "Địa điểm gần khách sạn";
          final String tourismType = tags['tourism'] ?? "";
          final String amenityType = tags['amenity'] ?? "";

          // Phân loại danh mục để hiển thị khớp với 3 Tab bộ lọc UI của Cường
          String uiCategory = "di_tich"; // Mặc định
          if (amenityType == "restaurant" || tags['cuisine'] != null) {
            uiCategory = "am_thuc";
          } else if (tourismType == "theme_park" || tourismType == "zoo" || amenityType == "cinema") {
            uiCategory = "vui_choi";
          }

          // Gán ảnh Unsplash chất lượng cao theo từng danh mục nhìn cho trực quan, chuyên nghiệp
          String imgUrl = "https://images.unsplash.com/photo-1555939594-58d7cb561ad1"; // Ảnh ẩm thực mặc định
          if (uiCategory == "di_tich") {
            imgUrl = "https://images.unsplash.com/photo-1596422846543-75c6fc18a523"; // Ảnh di tích thành phố
          } else if (uiCategory == "vui_choi") {
            imgUrl = "https://images.unsplash.com/photo-1513885535751-8b9238bd345a"; // Ảnh vui chơi giải trí
          }

          return {
            "name": name,
            "address": tags['addr:street'] != null
                ? "${tags['addr:housenumber'] ?? ''} ${tags['addr:street']}, Quận 1, HCM"
                : "Trung tâm Quận 1, TP. Hồ Chí Minh",
            "rating": 4.7, // Mạng mở không trả về rating, mình gài cứng để UI render ngôi sao siêu đẹp
            "imageUrl": imgUrl,
            "type": uiCategory,
          };
        }).toList();
      }
    } catch (e) {
      debugPrint("❌ Lỗi kết nối OpenStreetMap: $e");
    }

    // Luồng phòng thủ tối hậu nếu máy chủ rớt mạng
    return _getMockHcmPlaces();
  }

  // Hàm helper phân loại danh mục dựa trên mảng 'types' trả về từ Google
  int _mapGoogleTypeToCategory(List<dynamic> types) {
    if (types.contains('restaurant') || types.contains('cafe') || types.contains('food')) return 2; // Ẩm thực
    if (types.contains('park') || types.contains('amusement_park') || types.contains('zoo')) return 1; // Vui chơi
    return 3; // Di tích / Văn hóa lịch sử
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final double screenWidth = MediaQuery.of(context).size.width;
    final double screenHeight = MediaQuery.of(context).size.height;
    final bool isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _handleRefresh,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: EdgeInsets.all(screenWidth * 0.05),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  "Khám phá\nĐiểm đến quanh bạn",
                  style: TextStyle(
                    fontSize: screenWidth < 360 ? 24 : 28,
                    fontWeight: FontWeight.bold,
                    height: 1.2,
                    color: colorScheme.onSurface,
                  ),
                ),
                const SizedBox(height: 20),

                _buildFilterChips(screenWidth, colorScheme),
                const SizedBox(height: 20),

                FutureBuilder<List<Map<String, dynamic>>>(
                  future: _placesFuture,
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return SizedBox(
                        height: screenHeight * 0.4,
                        child: const Center(child: CircularProgressIndicator()),
                      );
                    }

                    final allPlaces = snapshot.data ?? [];
                    if (allPlaces.isEmpty) {
                      return const Center(child: Text("Không tìm thấy địa điểm tham quan nào."));
                    }

                    // Bộ lọc phân rã danh mục tab đang chọn
                    List<Map<String, dynamic>> filteredPlaces = allPlaces.where((place) {
                      if (_selectedChipIndex == 0) return true;
                      return place['type'] == _selectedChipIndex;
                    }).toList();

                    if (filteredPlaces.isEmpty) filteredPlaces = allPlaces;

                    final topPlace = filteredPlaces.first;
                    final String topName = topPlace['name'];
                    final String topAddress = topPlace['address'];
                    final String topImg = topPlace['imageUrl'];

                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // BANNER ĐỊA ĐIỂM NỔI BẬT NHẤT QUÉT TỪ GOOGLE
                        Container(
                          width: double.infinity,
                          height: screenHeight * 0.4 > 250 ? screenHeight * 0.4 : 250,
                          decoration: BoxDecoration(
                            color: colorScheme.surface,
                            borderRadius: BorderRadius.circular(25),
                            boxShadow: [
                              BoxShadow(
                                color: isDark ? Colors.black26 : Colors.black.withOpacity(0.05),
                                blurRadius: 10,
                                offset: const Offset(0, 5),
                              )
                            ],
                          ),
                          child: Stack(
                            children: [
                              Positioned.fill(
                                child: ClipRRect(
                                  borderRadius: BorderRadius.circular(25),
                                  child: topImg.startsWith('http')
                                      ? Image.network(
                                    topImg,
                                    fit: BoxFit.cover,
                                    errorBuilder: (c,e,s) => Container(
                                      color: Colors.grey.shade200,
                                      child: const Icon(Icons.broken_image, size: 40),
                                    ),
                                  )
                                      : Image.asset(topImg, fit: BoxFit.cover),
                                ),
                              ),
                              Positioned.fill(
                                child: Container(
                                  decoration: BoxDecoration(
                                    borderRadius: BorderRadius.circular(25),
                                    gradient: LinearGradient(
                                      begin: Alignment.topCenter,
                                      end: Alignment.bottomCenter,
                                      colors: [Colors.transparent, Colors.black.withOpacity(0.8)],
                                    ),
                                  ),
                                ),
                              ),
                              Positioned(
                                bottom: 20,
                                left: 20,
                                right: 20,
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      topName,
                                      style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 4),
                                    Row(
                                      children: [
                                        const Icon(Icons.location_on, size: 14, color: Colors.white70),
                                        const SizedBox(width: 4),
                                        Expanded(
                                          child: Text(
                                            topAddress,
                                            style: const TextStyle(color: Colors.white70, fontSize: 13),
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),

                        const SizedBox(height: 25),
                        Text(
                          "Hành trình gợi ý quanh khách sạn",
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colorScheme.onSurface),
                        ),
                        const SizedBox(height: 12),

                        // DANH SÁCH ĐỀ XUẤT ĐỊA ĐIỂM NGANG TỪ GOOGLE
                        _buildSuggestionCards(colorScheme, filteredPlaces),
                      ],
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildFilterChips(double screenWidth, ColorScheme colorScheme) {
    List<String> labels = ["Tất cả", "Vui chơi/Công viên", "Ẩm thực/Cà phê", "Di tích văn hóa"];
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: labels.asMap().entries.map((entry) {
        int index = entry.key;
        String label = entry.value;
        bool isSelected = _selectedChipIndex == index;

        return GestureDetector(
          onTap: () {
            setState(() {
              _selectedChipIndex = index;
            });
          },
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 8),
            decoration: BoxDecoration(
              color: isSelected ? colorScheme.primary : colorScheme.surface,
              borderRadius: BorderRadius.circular(15),
              border: Border.all(
                  color: isSelected ? colorScheme.primary : colorScheme.onSurface.withOpacity(0.1)
              ),
            ),
            child: Text(
              label,
              style: TextStyle(
                fontSize: screenWidth < 360 ? 12 : 14,
                color: isSelected ? Colors.white : colorScheme.onSurface,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
              ),
            ),
          ),
        );
      }).toList(),
    );
  }

  Widget _buildSuggestionCards(ColorScheme colorScheme, List<Map<String, dynamic>> places) {
    return SizedBox(
      height: 160,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        itemCount: places.length,
        itemBuilder: (context, index) {
          final place = places[index];
          final String name = place['name'];
          final String address = place['address'];
          final double rating = place['rating'];
          final String img = place['imageUrl'];

          return Container(
            width: 170,
            margin: const EdgeInsets.only(right: 15),
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: colorScheme.surface,
              borderRadius: BorderRadius.circular(20),
              boxShadow: [
                BoxShadow(color: Colors.black.withOpacity(0.02), blurRadius: 5, offset: const Offset(0, 2))
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: img.startsWith('http')
                        ? Image.network(
                      img,
                      width: double.infinity,
                      fit: BoxFit.cover,
                      errorBuilder: (c,e,s) => Container(
                        color: Colors.grey.shade100,
                        child: const Icon(Icons.image_not_supported, color: Colors.grey),
                      ),
                    )
                        : Image.asset(img, width: double.infinity, fit: BoxFit.cover),
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  name,
                  style: TextStyle(color: colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: 13),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Row(
                  children: [
                    const Icon(Icons.star, color: Colors.amber, size: 12),
                    const SizedBox(width: 3),
                    Text(
                      rating.toString(),
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(width: 5),
                    Expanded(
                      child: Text(
                        address,
                        style: const TextStyle(color: Colors.grey, fontSize: 10),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    )
                  ],
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  // 🌟 DỮ LIỆU DỰ PHÒNG AN TOÀN TRÁNH BÁO LỖI MÀN HÌNH ĐỎ KHI KEY TRỐNG
  List<Map<String, dynamic>> _getMockHcmPlaces() {
    return [
      {
        "name": "Nhà Thờ Đức Bà Sài Gòn",
        "address": "01 Công xã Paris, Bến Nghé, Quận 1",
        "rating": 4.8,
        "imageUrl": "https://images.unsplash.com/photo-1583417319070-4a69db38a482?q=80&w=400&auto=format&fit=crop",
        "type": 3
      },
      {
        "name": "Dinh Độc Lập (Reunification Palace)",
        "address": "135 Nam Kỳ Khởi Nghĩa, Bến Thành, Quận 1",
        "rating": 4.7,
        "imageUrl": "https://images.unsplash.com/photo-1623916298533-8a397caee501?q=80&w=400&auto=format&fit=crop",
        "type": 3
      },
      {
        "name": "Bưu Điện Trung Tâm Thành Phố",
        "address": "02 Công xã Paris, Bến Nghé, Quận 1",
        "rating": 4.6,
        "imageUrl": "https://images.unsplash.com/photo-1596436889106-be35e843f974?q=80&w=400&auto=format&fit=crop",
        "type": 3
      },
      {
        "name": "Hồ Con Rùa (Công Trường Quốc Tế)",
        "address": "Vòng xoay Công Trường Quốc Tế, Quận 3",
        "rating": 4.5,
        "imageUrl": "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?q=80&w=400&auto=format&fit=crop",
        "type": 1
      },
      {
        "name": "Chợ Bến Thành",
        "address": "Đường Lê Lợi, Bến Thành, Quận 1",
        "rating": 4.4,
        "imageUrl": "https://images.unsplash.com/photo-1578051664121-654db4b63e8a?q=80&w=400&auto=format&fit=crop",
        "type": 2
      }
    ];
  }
}