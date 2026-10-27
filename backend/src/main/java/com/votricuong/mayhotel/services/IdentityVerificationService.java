package com.votricuong.mayhotel.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
public class IdentityVerificationService {

    private final RestTemplate restTemplate;
    private final String aiServerUrl;
    private final String identityHashPepper;
    private final double faceMatchThreshold;

    public IdentityVerificationService(
            RestTemplate restTemplate,
            @Value("${ai.server.url:http://127.0.0.1:5000}") String aiServerUrl,
            @Value("${identity.hash-pepper:}") String identityHashPepper,
            @Value("${identity.face-match-threshold:60}") double faceMatchThreshold) {
        this.restTemplate = restTemplate;
        this.aiServerUrl = aiServerUrl.replaceAll("/+$", "");
        this.identityHashPepper = identityHashPepper;
        this.faceMatchThreshold = faceMatchThreshold;
    }

    public FaceMatchResult verifyFace(MultipartFile documentImage, MultipartFile selfieImage) {
        try {
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("cccd_image", filePart(documentImage, "identity-document.jpg"));
            form.add("selfie_image", filePart(selfieImage, "selfie.jpg"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    aiServerUrl + "/api/v1/ai/verify-cccd",
                    new HttpEntity<>(form, headers),
                    Map.class);
            if (response == null || !"success".equals(String.valueOf(response.get("status")))) {
                throw new IllegalStateException("AI không trả về kết quả xác thực hợp lệ");
            }
            Object rawFace = response.get("face_verification");
            if (!(rawFace instanceof Map<?, ?> face)) {
                throw new IllegalStateException("AI thiếu kết quả đối sánh khuôn mặt");
            }
            boolean verified = Boolean.TRUE.equals(face.get("verified"));
            double similarity = number(face.get("similarity"));
            return new FaceMatchResult(verified && similarity >= faceMatchThreshold, similarity);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể kết nối dịch vụ xác thực AI", exception);
        }
    }

    public String fingerprint(String identitySource) {
        if (identityHashPepper == null || identityHashPepper.isBlank()) {
            throw new IllegalStateException("Máy chủ chưa cấu hình IDENTITY_HASH_PEPPER");
        }
        if (identitySource == null || identitySource.isBlank()) {
            throw new IllegalArgumentException("Thiếu dữ liệu tạo dấu vân tay định danh");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(identityHashPepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(identitySource.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo dấu vân tay định danh", exception);
        }
    }

    public String imageFingerprint(MultipartFile image) {
        try {
            return fingerprint(Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(image.getBytes())));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo dấu vân tay ảnh giấy tờ", exception);
        }
    }

    public String last4(String identityValue) {
        if (identityValue == null) return "";
        String normalized = identityValue.replaceAll("[^A-Za-z0-9]", "");
        return normalized.length() <= 4 ? normalized : normalized.substring(normalized.length() - 4);
    }

    private HttpEntity<ByteArrayResource> filePart(MultipartFile file, String fallbackName) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh xác thực không được để trống");
        }
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() == null ? fallbackName : file.getOriginalFilename();
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        MediaType type = file.getContentType() == null
                ? MediaType.IMAGE_JPEG : MediaType.parseMediaType(file.getContentType());
        partHeaders.setContentType(type);
        return new HttpEntity<>(resource, partHeaders);
    }

    private double number(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0D;
        }
    }

    public record FaceMatchResult(boolean verified, double similarity) {}
}
