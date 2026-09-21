import 'package:flutter/material.dart';

class AuthState {
  final String role;
  final String username;
  final bool isLoggedIn;

  AuthState({this.role = 'customer', this.username = 'Guest', this.isLoggedIn = false});
}

class AuthStateService extends ChangeNotifier {
  static final AuthStateService _instance = AuthStateService._internal();
  factory AuthStateService() => _instance;
  AuthStateService._internal();

  AuthState _currentAuth = AuthState();

  AuthState get currentAuth => _currentAuth;

  void login(String role, String username) {
    _currentAuth = AuthState(role: role, username: username, isLoggedIn: true);
    notifyListeners();
  }

  void logout() {
    _currentAuth = AuthState(); // Reset to Guest
    notifyListeners();
  }
}
