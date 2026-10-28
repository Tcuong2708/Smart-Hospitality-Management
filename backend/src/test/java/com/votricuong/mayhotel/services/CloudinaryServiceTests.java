package com.votricuong.mayhotel.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryServiceTests {
    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        service = new CloudinaryService(cloudinary, true, "may-hotel", "123", "secret");
    }

    @Test
    void uploadsAValidatedImageAndReturnsCloudinaryIdentifiers() throws Exception {
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/may-hotel/image/upload/v1/may_hotel/rooms/a.png",
                "public_id", "may_hotel/rooms/a"));

        CloudinaryService.UploadResult result = service.uploadImage(
                new MockMultipartFile("imageFile", "room.png", "image/png", pngBytes()),
                "may_hotel/rooms");

        assertThat(result.secureUrl()).startsWith("https://res.cloudinary.com/");
        assertThat(result.publicId()).isEqualTo("may_hotel/rooms/a");
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void rejectsAFileWhoseContentDoesNotMatchItsImageExtension() {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "imageFile", "room.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> service.uploadImage(fakeImage, "may_hotel/rooms"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nội dung tệp");
    }

    @Test
    void refusesUploadsWhenCloudinaryIsNotConfigured() {
        CloudinaryService disabled = new CloudinaryService(cloudinary, false, "", "", "");

        assertThatThrownBy(() -> disabled.uploadImage(
                new MockMultipartFile("imageFile", "room.png", "image/png", pngBytes()),
                "may_hotel/rooms"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chưa được cấu hình");
    }

    @Test
    void extractsAndDeletesThePublicId() throws Exception {
        when(uploader.destroy("may_hotel/rooms/a", Map.of())).thenReturn(Map.of("result", "ok"));
        String publicId = service.extractPublicId(
                "https://res.cloudinary.com/may-hotel/image/upload/v1720/may_hotel/rooms/a.webp?x=1");

        assertThat(publicId).isEqualTo("may_hotel/rooms/a");
        assertThat(service.deleteImage(publicId)).isTrue();
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
    }
}
