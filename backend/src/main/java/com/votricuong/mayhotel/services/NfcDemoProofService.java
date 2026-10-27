package com.votricuong.mayhotel.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class NfcDemoProofService {

    private final ObjectMapper objectMapper;
    private final String publicKeyBase64;

    public NfcDemoProofService(
            ObjectMapper objectMapper,
            @Value("${nfc.demo.public-key-base64:}") String publicKeyBase64) {
        this.objectMapper = objectMapper;
        this.publicKeyBase64 = publicKeyBase64;
    }

    public DemoProof verify(String rawPayload) {
        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            throw new IllegalStateException("Máy chủ chưa cấu hình NFC_DEMO_PUBLIC_KEY_BASE64");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(rawPayload, Map.class);
            String version = required(payload, "version");
            String tokenId = required(payload, "tokenId");
            String last4 = required(payload, "last4");
            String issuer = required(payload, "issuer");
            long issuedAt = Long.parseLong(required(payload, "issuedAt"));
            long expiresAt = Long.parseLong(required(payload, "expiresAt"));
            String encodedSignature = required(payload, "signature");

            if (!"1".equals(version) || !"MAY_HOTEL_DEMO".equals(issuer)) {
                throw new IllegalArgumentException("Thẻ NFC mô phỏng không đúng nhà phát hành hoặc phiên bản");
            }
            long now = Instant.now().getEpochSecond();
            if (issuedAt > now + 300 || expiresAt < now || expiresAt <= issuedAt) {
                throw new IllegalArgumentException("Thẻ NFC mô phỏng đã hết hạn hoặc có thời gian không hợp lệ");
            }
            if (!last4.matches("[A-Za-z0-9]{4}")) {
                throw new IllegalArgumentException("Bốn ký tự cuối trên thẻ NFC không hợp lệ");
            }

            String canonical = String.join("|", version, tokenId, last4, String.valueOf(issuedAt),
                    String.valueOf(expiresAt), issuer);
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(cleanPem(publicKeyBase64))));
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(canonical.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(encodedSignature))) {
                throw new IllegalArgumentException("Chữ ký thẻ NFC mô phỏng không hợp lệ");
            }
            return new DemoProof(tokenId, last4, expiresAt);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Payload NFC mô phỏng không hợp lệ", exception);
        }
    }

    private String required(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Payload NFC thiếu trường " + field);
        }
        return String.valueOf(value).trim();
    }

    private String cleanPem(String value) {
        return value.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    public record DemoProof(String tokenId, String last4, long expiresAt) {}
}
