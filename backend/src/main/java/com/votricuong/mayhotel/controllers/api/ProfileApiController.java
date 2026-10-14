package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.dto.ProfileResponse;
import com.votricuong.mayhotel.dto.ProfileUpdateRequest;
import com.votricuong.mayhotel.services.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileApiController {
    private final ProfileService profileService;

    public ProfileApiController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@PathVariable Long userId) {
        return profileService.findProfile(userId)
                .map(profile -> ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ thành công", profile)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Không tìm thấy tài khoản.")));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @PathVariable Long userId,
            @RequestBody ProfileUpdateRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Cập nhật hồ sơ thành công",
                    profileService.updateProfile(userId, request)));
        } catch (ProfileService.ProfileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
