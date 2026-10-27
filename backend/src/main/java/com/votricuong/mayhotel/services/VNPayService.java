package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.config.VNPayConfig;
import com.votricuong.mayhotel.documents.Invoice;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    public VNPayService(VNPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
    }

    public String createPaymentUrl(Invoice invoice, HttpServletRequest request) {
        requireConfigured();
        long payableAmount = OnlineBookingPolicy.payableDeposit(invoice);
        if (invoice == null || invoice.getId() == null || payableAmount <= 0) {
            throw new IllegalArgumentException("Hóa đơn thanh toán không hợp lệ.");
        }
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = invoice.getId() + "_" + System.currentTimeMillis();
        String vnp_IpAddr = getIpAddress(request);
        String vnp_TmnCode = vnPayConfig.getTmnCode();

        long amount = payableAmount * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Dat coc 10% phong MAY HOTEL - HD #" + invoice.getId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        TimeZone vnTz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(vnTz);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(vnTz); // VNPay dùng giờ VN; đặt rõ để server chạy UTC cũng không lệch 7 tiếng
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    // Xây dựng hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    // Xây dựng query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi encode param VNPay: ", e);
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getPayUrl() + "?" + queryUrl;
    }

    /**
     * Xác thực chữ ký phản hồi từ VNPAY (Return URL hoặc IPN Webhook)
     */
    public boolean validateSignature(Map<String, String> fields) {
        if (!isEnabled()) return false;
        String inputHash = fields.get("vnp_SecureHash");
        if (inputHash == null || inputHash.isEmpty()) {
            return false;
        }

        Map<String, String> vnp_Params = new HashMap<>(fields);
        vnp_Params.remove("vnp_SecureHashType");
        vnp_Params.remove("vnp_SecureHash");

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi validate hash VNPay: ", e);
            return false;
        }

        String actualHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return MessageDigest.isEqual(
                actualHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                inputHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    public boolean isEnabled() {
        return vnPayConfig.isEnabled()
                && hasText(vnPayConfig.getTmnCode())
                && hasText(vnPayConfig.getHashSecret())
                && hasText(vnPayConfig.getPayUrl())
                && hasText(vnPayConfig.getReturnUrl());
    }

    private void requireConfigured() {
        if (!isEnabled()) {
            throw new IllegalStateException("Thanh toán VNPay chưa được bật hoặc thiếu cấu hình bảo mật.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Thuật toán băm HMAC-SHA512 chuẩn VNPAY
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(result.length * 2);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Lỗi tính toán HMAC-SHA512: ", ex);
            return "";
        }
    }

    /**
     * Lấy địa chỉ IP của Client
     */
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
                ipAddress = "127.0.0.1";
            }
        } catch (Exception e) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }
}
