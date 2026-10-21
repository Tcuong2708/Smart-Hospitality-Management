import 'package:flutter/material.dart';
import 'hotel_api_client.dart';

class AuthState {
  final String role;
  final String username;
  final int? userId;
  final bool isLoggedIn;

  AuthState({
    this.role = 'guest',
    this.username = 'Guest',
    this.userId,
    this.isLoggedIn = false,
  });
}

class AuthStateService extends ChangeNotifier {
  static final AuthStateService _instance = AuthStateService._internal();
  factory AuthStateService() => _instance;
  AuthStateService._internal();

  AuthState _currentAuth = AuthState();

  AuthState get currentAuth => _currentAuth;

  void login(String role, String username, {int? userId}) {
    _currentAuth = AuthState(
      role: role,
      username: username,
      userId: userId,
      isLoggedIn: true,
    );
    notifyListeners();
  }

  void logout() {
    HotelApiClient.instance.clearAccessToken();
    _currentAuth = AuthState(); // Reset to Guest
    notifyListeners();
  }
}
