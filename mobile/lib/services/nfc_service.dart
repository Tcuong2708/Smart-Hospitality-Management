import 'dart:convert';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:flutter/material.dart';

class NfcService {

  // Kiểm tra phần cứng NFC trên máy thật
  static Future<bool> isNfcAvailable() async {
    return await NfcManager.instance.isAvailable();
  }

  // 🎴 HÀM ĐỌC THÔNG TIN CCCD BẰNG THANG DÒ THUỘC TÍNH TỰ ĐỘNG
  static Future<void> startCccdScanning({
    required Function(String cccdRawText) onSuccess,
    required Function(String error) onError,
  }) async {

    bool available = await isNfcAvailable();
    if (!available) {
      onError("Thiết bị chưa bật hoặc không hỗ trợ tính năng NFC.");
      return;
    }

    debugPrint("🎴 NFC: Đang kích hoạt thang dò thuộc tính tự động...");

    NfcManager.instance.startSession(
      pollingOptions: {
        NfcPollingOption.iso14443, // Tần số bắt sóng các dòng thẻ chip, thẻ trắng thông dụng
      },
      onDiscovered: (NfcTag tag) async {
        // 🟩 BỌC TRONG MASTER TRY-CATCH ĐỂ KHÔNG BAO GIỜ BỊ LỌT LỖI GÂY TREO CỔNG
        try {
          final dynamic tagData = tag.data;
          if (tagData == null) {
            onError("Không thể bốc tách dữ liệu từ phần cứng.");
            await NfcManager.instance.stopSession();
            return;
          }

          // Đi vào phân hệ NDEF của chip thẻ
          final dynamic ndefData = tagData.ndef;
          if (ndefData == null) {
            onError("Thẻ trắng này chưa được định dạng cấu trúc dữ liệu NDEF Text.");
            await NfcManager.instance.stopSession();
            return;
          }

          // 🚀 1. THANG DÒ THUỘC TÍNH (PROPERTY LADDER) CHO NDEF MESSAGE
          dynamic messageData;
          List<String> debugLogs = [];

          // Cách 1: Thử ndefMessage (Bản chuẩn Pigeon của nfc_manager mới)
          try { messageData = ndefData.ndefMessage; if(messageData != null) debugLogs.add("✅ Thành công với: ndefMessage"); } catch (e) { debugLogs.add("❌ ndefMessage lỗi: $e"); }

          // Cách 2: Thử cachedNdefMessage
          if (messageData == null) {
            try { messageData = ndefData.cachedNdefMessage; if(messageData != null) debugLogs.add("✅ Thành công với: cachedNdefMessage"); } catch (e) { debugLogs.add("❌ cachedNdefMessage lỗi: $e"); }
          }

          // Cách 3: Thử message
          if (messageData == null) {
            try { messageData = ndefData.message; if(messageData != null) debugLogs.add("✅ Thành công với: message"); } catch (e) { debugLogs.add("❌ message lỗi: $e"); }
          }

          // Cách 4: Thử cachedMessage
          if (messageData == null) {
            try { messageData = ndefData.cachedMessage; if(messageData != null) debugLogs.add("✅ Thành công với: cachedMessage"); } catch (e) { debugLogs.add("❌ cachedMessage lỗi: $e"); }
          }

          // HÚT VÁNG: Nếu cả 4 cách dò đều thất bại, lập tức xả log ra Terminal để xem danh tính getter
          if (messageData == null) {
            debugPrint("📊 [NFC Diagnostic Matrix Logs]:\n${debugLogs.join('\n')}");
            onError("Cấu trúc thẻ Pigeon chưa khớp thuộc tính. Vui lòng xem logcat Terminal.");
            await NfcManager.instance.stopSession();
            return;
          }

          // 🚀 2. THANG DÒ DANH SÁCH BẢN GHI (RECORDS)
          dynamic recordsData;
          try { recordsData = messageData.records; } catch (_) {}
          if (recordsData == null) {
            try { recordsData = messageData.ndefRecords; } catch (_) {}
          }

          if (recordsData == null || (recordsData is List && recordsData.isEmpty)) {
            onError("Thẻ hoàn toàn trống, chưa ghi thông tin CCCD.");
            await NfcManager.instance.stopSession();
            return;
          }

          final List<dynamic> recordsList = List<dynamic>.from(recordsData);
          final dynamic firstRecord = recordsList.first;

          // 🚀 3. THANG DÒ MẢNG BYTE NỘI DUNG (PAYLOAD)
          List<int> payload = [];
          try { payload = List<int>.from(firstRecord.payload); } catch (_) {}
          if (payload.isEmpty) {
            try { payload = List<int>.from(firstRecord.bytes); } catch (_) {}
          }

          if (payload.isEmpty) {
            onError("Dữ liệu payload bên trong thẻ trắng bị rỗng.");
            await NfcManager.instance.stopSession();
            return;
          }

          // Tiến hành dịch mảng Byte sang chuỗi String tiếng Việt có dấu
          String decodedText = _parseNdefTextPayload(payload);

          if (decodedText.trim().isEmpty) {
            onError("Nội dung văn bản bên trong thẻ trắng bị rỗng.");
          } else {
            debugPrint("🎴 NFC: Đã bốc dữ liệu thành công -> $decodedText");
            onSuccess(decodedText);
          }

        } catch (e) {
          debugPrint("❌ NFC Lỗi kết nối vật lý tầng sâu: $e");
          onError("❌ Lỗi: Thẻ bị di chuyển hoặc rút ra quá nhanh! Vui lòng giữ cố định thẻ trong 2 giây.");
        } finally {
          // 🚀 NÚT THẮT CHÍ MẠNG: Đóng Session sạch sẽ bất kể thành công hay thất bại
          // Giúp cổng phần cứng mở khóa, lần quét sau liên tục sẽ KHÔNG bao giờ bị văng sang ứng dụng khác!
          await NfcManager.instance.stopSession();
          debugPrint("🎴 NFC: Đã giải phóng cổng kết nối an toàn.");
        }
      },
    );
  }

  // Hàm phụ trợ giải mã mảng Byte thô của định dạng văn bản chuẩn NDEF Text
  static String _parseNdefTextPayload(List<int> payload) {
    if (payload.isEmpty) return "";
    int languageCodeLength = payload[0] & 0x3F;
    return utf8.decode(payload.sublist(1 + languageCodeLength));
  }
}