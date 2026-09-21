class UserModel {
  final int? id;
  final String username;
  final String role;

  UserModel({this.id, required this.username, required this.role});
  
  factory UserModel.fromMap(Map<String, dynamic> map) {
    return UserModel(
      id: map['id'],
      username: map['username'],
      role: map['role'],
    );
  }
}