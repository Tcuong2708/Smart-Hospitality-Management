package com.votricuong.mayhotel.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CloudinaryService {
    public static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp");

    private final Cloudinary cloudinary;
    private final boolean enabled;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryService(
            Cloudinary cloudinary,
            @Value("${app.features.cloudinary.enabled:false}") boolean enabled,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = cloudinary;
        this.enabled = enabled;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public boolean isConfigured() {
        return enabled && hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    public UploadResult uploadImage(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) return null;
        requireConfigured();
        byte[] bytes = readAndValidate(file);
        String folder = normalizeFolder(folderName);
        try {
            Map<?, ?> response = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "overwrite", false,
                    "unique_filename", true,
                    "use_filename", false,
                    "quality", "auto:good",
                    "fetch_format", "auto"));
            String secureUrl = stringValue(response.get("secure_url"));
            String publicId = stringValue(response.get("public_id"));
            if (!hasText(secureUrl) || !hasText(publicId)) {
                throw new IllegalStateException("Cloudinary không trả về URL hoặc mã ảnh hợp lệ");
            }
            return new UploadResult(secureUrl, publicId);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể tải ảnh lên Cloudinary", ex);
        }
    }

    public boolean deleteImage(String publicId) {
        if (!hasText(publicId)) return true;
        if (!isConfigured()) {
            log.warn("Không thể xóa ảnh Cloudinary {} vì dịch vụ chưa được cấu hình", publicId);
            return false;
        }
        try {
            Map<?, ?> response = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String result = stringValue(response.get("result"));
            return "ok".equalsIgnoreCase(result) || "not found".equalsIgnoreCase(result);
        } catch (IOException | RuntimeException ex) {
            log.warn("Xóa ảnh Cloudinary {} thất bại", publicId, ex);
            return false;
        }
    }

    public String extractPublicId(String imageUrl) {
        if (!hasText(imageUrl) || !imageUrl.contains("res.cloudinary.com") || !imageUrl.contains("/upload/")) {
            return null;
        }
        try {
            String cleanUrl = imageUrl.split("\\?", 2)[0];
            String path = cleanUrl.substring(cleanUrl.indexOf("/upload/") + "/upload/".length());
            if (path.matches("^v\\d+/.*")) path = path.substring(path.indexOf('/') + 1);
            int extensionIndex = path.lastIndexOf('.');
            return extensionIndex > 0 ? path.substring(0, extensionIndex) : path;
        } catch (RuntimeException ex) {
            log.warn("Không thể tách public_id từ URL Cloudinary", ex);
            return null;
        }
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 5 MB");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG, GIF hoặc WebP");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Phần mở rộng ảnh không hợp lệ");
        }
        try {
            byte[] bytes = file.getBytes();
            if (!matchesSignature(bytes, extension)) {
                throw new IllegalArgumentException("Nội dung tệp không đúng định dạng ảnh");
            }
            return bytes;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể đọc tệp ảnh", ex);
        }
    }

    private boolean matchesSignature(byte[] bytes, String extension) {
        if (bytes == null || bytes.length < 12) return false;
        return switch (extension) {
            case "jpg", "jpeg" -> (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            case "png" -> Arrays.equals(Arrays.copyOf(bytes, 8),
                    new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "gif" -> new String(bytes, 0, 6, StandardCharsets.US_ASCII).matches("GIF8[79]a");
            case "webp" -> "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                    && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII));
            default -> false;
        };
    }

    private String extensionOf(String filename) {
        if (!hasText(filename) || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeFolder(String folderName) {
        if (!hasText(folderName) || !folderName.matches("[a-zA-Z0-9/_-]+")) {
            throw new IllegalArgumentException("Thư mục lưu ảnh không hợp lệ");
        }
        return folderName.replaceAll("^/+|/+$", "");
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary chưa được cấu hình");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record UploadResult(String secureUrl, String publicId) {}
}
