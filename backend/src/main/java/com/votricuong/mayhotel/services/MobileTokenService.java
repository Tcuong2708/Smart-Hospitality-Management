package com.votricuong.mayhotel.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votricuong.mayhotel.documents.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MobileTokenService {
    public static final long EXPIRES_IN_SECONDS = 8 * 60 * 60;
    private static final Logger log = LoggerFactory.getLogger(MobileTokenService.class);
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public MobileTokenService(ObjectMapper objectMapper,
                              @Value("${mobile.auth.jwt-secret:}") String configuredSecret) {
        this.objectMapper = objectMapper;
        if (configuredSecret == null || configuredSecret.isBlank()) {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            this.secret = generated;
            log.warn("MOBILE_JWT_SECRET chưa được cấu hình; token mobile sẽ hết hiệu lực khi backend khởi động lại");
        } else {
            this.secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String issue(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Không thể cấp token cho tài khoản không hợp lệ");
        }
        try {
            long now = Instant.now().getEpochSecond();
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getId());
            payload.put("roleId", user.getRoleId() == null ? 5L : user.getRoleId());
            payload.put("role", user.getRole());
            payload.put("iat", now);
            payload.put("exp", now + EXPIRES_IN_SECONDS);
            String body = encodeJson(payload);
            String unsigned = header + "." + body;
            return unsigned + "." + URL_ENCODER.encodeToString(sign(unsigned));
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo token đăng nhập", ex);
        }
    }

    public Claims verifyAuthorization(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new SecurityException("Thiếu access token");
        }
        String token = authorizationHeader.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new SecurityException("Access token không hợp lệ");
        }
        try {
            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsigned), URL_DECODER.decode(parts[2]))) {
                throw new SecurityException("Chữ ký access token không hợp lệ");
            }
            Map<String, Object> payload = objectMapper.readValue(
                    URL_DECODER.decode(parts[1]), new TypeReference<>() {});
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new SecurityException("Access token đã hết hạn");
            }
            return new Claims(
                    ((Number) payload.get("sub")).longValue(),
                    ((Number) payload.get("roleId")).longValue(),
                    String.valueOf(payload.get("role")),
                    expiresAt);
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException("Access token không hợp lệ", ex);
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    public record Claims(long userId, long roleId, String role, long expiresAt) {}
}
