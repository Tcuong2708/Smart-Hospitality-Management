import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:cccd_vietnam/dmrtd.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:may_hotel_app/services/nfc_service.dart';

class MrzOcrService {
  static Future<RealCccdAccessData?> extract(File image) async {
    final recognizer = TextRecognizer(script: TextRecognitionScript.latin);
    try {
      final result = await recognizer.processImage(InputImage.fromFile(image));
      final lines = result.blocks
          .expand((block) => block.lines)
          .map((line) => _normalize(line.text))
          .where((line) => line.length >= 28)
          .toList();

      for (var index = 0; index + 2 < lines.length; index++) {
        final candidate = lines
            .skip(index)
            .take(3)
            .map((line) => line.padRight(30, '<').substring(0, 30))
            .join();
        try {
          final mrz = MRZ(Uint8List.fromList(ascii.encode(candidate)));
          return RealCccdAccessData(
            documentNumber: mrz.documentNumber,
            dateOfBirth: mrz.dateOfBirth,
            dateOfExpiry: mrz.dateOfExpiry,
          );
        } catch (_) {
          // Try the next three-line window; OCR may include surrounding text.
        }
      }
      return null;
    } finally {
      await recognizer.close();
    }
  }

  static String _normalize(String value) => value
      .toUpperCase()
      .replaceAll(RegExp(r'\s+'), '')
      .replaceAll(RegExp(r'[^A-Z0-9<]'), '');
}
