import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:may_hotel_app/services/hotel_api_provider.dart';
import 'package:may_hotel_app/services/auth_state_service.dart';
import 'package:may_hotel_app/services/language_service.dart';
import '../../auth/views/login_screen.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/utils/responsive.dart';
import '../../profile/views/personal_info_screen.dart';
import '../../profile/views/settings_screen.dart';
import '../../profile/views/help_center_screen.dart';
import '../../../../main.dart';

class ProfileScreen extends StatefulWidget {
  final String username;
  const ProfileScreen({Key? key, required this.username}) : super(key: key);

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final HotelApiProvider _apiProvider = HotelApiProvider();
  Map<String, dynamic>? _userProfile;
  bool _isLoading = true;

  static const Color navyPrimary = Color(0xFF0F2942);
  static const Color goldAccent = Color(0xFFD4AF37);

  @override
  void initState() {
    super.initState();
    _fetchUserProfileLive();
  }

  Future<void> _fetchUserProfileLive() async {
    if (!mounted) return;
    setState(() => _isLoading = true);
    try {
      final res = await _apiProvider.getUserProfile(widget.username);
      if (mounted) {
        setState(() {
          _userProfile = res;
          _isLoading = false;
        });
      }
    } catch (e) {
      debugPrint("❌ Lỗi bốc dữ liệu hồ sơ: $e");
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showCustomSnackBar(String message, Color color) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: color),
    );
  }

  void _handleCheckUpdate(BuildContext context) {
    final lang = LanguageService();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        title: Row(
          children: [
            const Icon(Icons.system_update_alt_rounded, color: goldAccent),
            const SizedBox(width: 10),
            Text(lang.translate('system_update')),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("${lang.translate('current_version')}: v2.0.1"),
            const SizedBox(height: 8),
            Text(lang.translate('latest_version'), style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(lang.translate('close')),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bool isDarkMode = theme.brightness == Brightness.dark;
    final Color textColor = colorScheme.onSurface;
    final lang = LanguageService();

    return ListenableBuilder(
      listenable: lang,
      builder: (context, _) {
        final authState = AuthStateService().currentAuth;
        final bool isLoggedIn = authState.isLoggedIn;

        String hoTenHienThi = isLoggedIn 
            ? (_userProfile?['hoTen']?.toString() ?? authState.username) 
            : lang.translate('not_logged_in');
        String emailHienThi = isLoggedIn 
            ? (_userProfile?['email']?.toString() ?? "${widget.username}@mayhotel.com") 
            : lang.translate('tap_to_login');

        return Scaffold(
          backgroundColor: theme.scaffoldBackgroundColor,
          appBar: AppBar(
            backgroundColor: Colors.transparent,
            elevation: 0,
            toolbarHeight: 0,
            systemOverlayStyle: SystemUiOverlayStyle(
              statusBarColor: Colors.transparent,
              statusBarIconBrightness: isDarkMode ? Brightness.light : Brightness.dark,
              statusBarBrightness: isDarkMode ? Brightness.dark : Brightness.light,
              systemNavigationBarColor: Colors.transparent,
              systemNavigationBarIconBrightness: isDarkMode ? Brightness.light : Brightness.dark,
              systemNavigationBarContrastEnforced: false,
            ),
          ),
          body: (_isLoading && isLoggedIn)
              ? Center(child: CircularProgressIndicator(color: colorScheme.primary))
              : Column(
                  children: [
                    SizedBox(height: Responsive.height(context) * 0.04),
                    GestureDetector(
                      onTap: isLoggedIn ? null : () => Navigator.push(context, MaterialPageRoute(builder: (context) => const LoginScreen())),
                      child: Column(
                        children: [
                          Center(
                            child: Stack(
                              children: [
                                ClipRRect(
                                  borderRadius: BorderRadius.circular(50),
                                  child: Image.asset(
                                    "assets/images/d2.jpg",
                                    width: 100,
                                    height: 100,
                                    fit: BoxFit.cover,
                                    color: isLoggedIn ? null : Colors.grey.withValues(alpha: 0.5),
                                    colorBlendMode: isLoggedIn ? null : BlendMode.saturation,
                                    errorBuilder: (context, error, stackTrace) => Container(
                                      width: 100,
                                      height: 100,
                                      color: isDarkMode ? const Color(0xFF1A222D) : Colors.grey[200],
                                      child: Icon(Icons.person, color: isDarkMode ? goldAccent : Colors.grey),
                                    ),
                                  ),
                                ),
                                if (isLoggedIn)
                                  Positioned(
                                    bottom: 0,
                                    right: 0,
                                    child: Container(
                                      padding: const EdgeInsets.all(5),
                                      decoration: const BoxDecoration(color: goldAccent, shape: BoxShape.circle),
                                      child: const Icon(Icons.camera_alt, color: navyPrimary, size: 16),
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          SizedBox(height: Responsive.sp(context, 15)),
                          Text(
                            hoTenHienThi, 
                            style: TextStyle(
                              fontSize: 22, 
                              fontWeight: FontWeight.bold, 
                              color: !isLoggedIn ? Colors.grey : (isDarkMode ? goldAccent : textColor)
                            )
                          ),
                          Text(
                            emailHienThi, 
                            style: TextStyle(
                              color: !isLoggedIn ? colorScheme.primary : (isDarkMode ? Colors.white60 : textColor.withValues(alpha: 0.6)),
                              fontWeight: !isLoggedIn ? FontWeight.bold : FontWeight.normal,
                            )
                          ),
                        ],
                      ),
                    ),
                    SizedBox(height: Responsive.sp(context, 30)),
                    Expanded(
                      child: Container(
                        padding: EdgeInsets.symmetric(horizontal: Responsive.sp(context, 20)),
                        decoration: BoxDecoration(
                          color: isDarkMode ? const Color(0xFF1A222D) : Colors.white,
                          borderRadius: const BorderRadius.vertical(top: Radius.circular(35)),
                          boxShadow: [
                            if (!isDarkMode) BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 20, offset: const Offset(0, -5)),
                          ],
                        ),
                        child: ListView(
                          physics: const BouncingScrollPhysics(),
                          children: [
                            if (isLoggedIn)
                              _buildProfileItem(context, Icons.person_outline, lang.translate('personal_info'), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (context) => PersonalInfoScreen(username: widget.username)))),
                            
                            _buildProfileItem(context, Icons.settings_outlined, lang.translate('settings'), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (context) => SettingsScreen(username: widget.username)))),
                            
                            _buildProfileItem(context, Icons.system_update_alt_rounded, lang.translate('check_update'), onTap: () => _handleCheckUpdate(context)),
                            
                            _buildProfileItem(context, Icons.help_outline, lang.translate('help'), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (context) => const HelpCenterScreen()))),
                            const Divider(),
                            if (isLoggedIn)
                              _buildProfileItem(
                                context,
                                Icons.logout,
                                lang.translate('logout'),
                                color: Colors.red,
                                onTap: () {
                                  AuthStateService().logout();
                                  _showCustomSnackBar(lang.translate('logged_out_msg'), Colors.blue);
                                },
                              )
                            else
                              _buildProfileItem(
                                context,
                                Icons.login,
                                lang.translate('login_now'),
                                color: colorScheme.primary,
                                onTap: () => Navigator.push(context, MaterialPageRoute(builder: (context) => const LoginScreen())),
                              ),
                          ],
                        ),
                      ),
                    )
                  ],
                ),
        );
      }
    );
  }

  Widget _buildProfileItem(BuildContext context, IconData icon, String title, {Color? color, VoidCallback? onTap}) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isDark = Theme.of(context).brightness == Brightness.dark;
    final Color textColor = colorScheme.onSurface;

    return ListTile(
      leading: Icon(icon, color: color ?? (isDark ? goldAccent : navyPrimary)),
      title: Text(title, style: TextStyle(color: color ?? textColor, fontWeight: FontWeight.w500, fontSize: Responsive.sp(context, 16))),
      trailing: Icon(Icons.arrow_forward_ios, size: 16, color: isDark ? Colors.white30 : textColor.withValues(alpha: 0.3)),
      onTap: onTap,
    );
  }
}
