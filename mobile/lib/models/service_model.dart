class ServiceModel {
  final int maDV;
  final String tenDV;
  final double giaTien;
  final String? donVi;

  ServiceModel({
    required this.maDV,
    required this.tenDV,
    required this.giaTien,
    this.donVi,
  });

  factory ServiceModel.fromJson(Map<String, dynamic> json) {
    return ServiceModel(
      maDV: json['maDV'],
      tenDV: json['tenDV'] ?? "",
      giaTien: (json['giaTien'] as num?)?.toDouble() ?? 0.0,
      donVi: json['donVi'],
    );
  }
}