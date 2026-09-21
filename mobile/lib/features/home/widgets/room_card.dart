import 'package:flutter/material.dart';
import '../../../core/constants/app_colors.dart';

import 'package:may_hotel_app/services/language_service.dart';

class RoomCard extends StatelessWidget {
  final String title;
  final String location;
  final String price;
  final String rating;
  final String? imageUrl;
  final VoidCallback? onTap;
  final VoidCallback? onAddTap;

  const RoomCard({
    Key? key,
    required this.title,
    required this.location,
    required this.price,
    required this.rating,
    this.imageUrl,
    this.onTap,
    this.onAddTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDark = theme.brightness == Brightness.dark;

    String cleanImageUrl = imageUrl ?? 'assets/images/room_sample.jpg';
    if (cleanImageUrl.startsWith('assets/images/http')) {
      cleanImageUrl = cleanImageUrl.replaceFirst('assets/images/', '');
    }
    final bool isNetworkImage = cleanImageUrl.startsWith('http');

    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(bottom: 20),
        decoration: BoxDecoration(
          color: colorScheme.surface,
          borderRadius: BorderRadius.circular(30),
          boxShadow: [
            BoxShadow(
              color: isDark ? Colors.black.withOpacity(0.3) : Colors.black.withOpacity(0.05),
              blurRadius: 15,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Stack(
              children: [
                ClipRRect(
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(30)),
                  child: isNetworkImage
                      ? Image.network(
                    cleanImageUrl,
                    height: 220,
                    width: double.infinity,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) => Container(
                      height: 220,
                      color: isDark ? Colors.grey[800] : Colors.grey[200],
                      child: Icon(Icons.hotel, size: 50, color: colorScheme.onSurfaceVariant),
                    ),
                  )
                      : Image.asset(
                    cleanImageUrl,
                    height: 220,
                    width: double.infinity,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) => Container(
                      height: 220,
                      color: isDark ? Colors.grey[800] : Colors.grey[200],
                      child: Icon(Icons.hotel, size: 50, color: colorScheme.onSurfaceVariant),
                    ),
                  ),
                ),
                // Nút Favorite
                Positioned(
                  top: 15,
                  right: 15,
                  child: Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: isDark ? Colors.grey[900]?.withOpacity(0.8) : Colors.white.withOpacity(0.9),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.favorite, color: Colors.red, size: 20),
                  ),
                ),
                // Badge Rating
                Positioned(
                  bottom: 15,
                  right: 15,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: isDark ? colorScheme.surface.withOpacity(0.9) : Colors.white.withOpacity(0.9),
                      borderRadius: BorderRadius.circular(15),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.star, color: AppColors.accentGold, size: 16),
                        const SizedBox(width: 4),
                        Text(
                            rating,
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              color: colorScheme.onSurface,
                            )
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),

            Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                      title,
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: colorScheme.onSurface,
                      )
                  ),
                  const SizedBox(height: 5),
                  Row(
                    children: [
                      Icon(Icons.location_on, size: 14, color: colorScheme.onSurfaceVariant),
                      const SizedBox(width: 4),
                      Text(
                          location,
                          style: TextStyle(
                              color: colorScheme.onSurfaceVariant,
                              fontSize: 13
                          )
                      ),
                    ],
                  ),
                  const SizedBox(height: 15),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // 🌟 ĐÃ SỬA: Loại bỏ dấu $, đổi đơn vị sang VNĐ và /đêm chuẩn cấu trúc Web
                      RichText(
                        text: TextSpan(
                          children: [
                            TextSpan(
                              text: "$price VNĐ",
                              style: TextStyle(
                                  color: colorScheme.primary,
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold
                              ),
                            ),
                            TextSpan(
                                text: " / ${LanguageService().translate('night')}",
                                style: TextStyle(color: colorScheme.onSurfaceVariant, fontSize: 13, fontWeight: FontWeight.normal)
                            ),
                          ],
                        ),
                      ),
                      // NÚT DẤU CỘNG ĐẶT PHÒNG
                      GestureDetector(
                        onTap: onAddTap,
                        child: Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: isDark ? colorScheme.onSurface : Colors.black,
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                              Icons.add,
                              color: isDark ? colorScheme.surface : Colors.white,
                              size: 20
                          ),
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
    );
  }
}