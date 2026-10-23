package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.enums.RoomStatus;
import com.votricuong.mayhotel.exceptions.RoomStatusConflictException;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.MobileTokenService;
import com.votricuong.mayhotel.services.RoomStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/mobile/housekeeping")
public class HousekeepingApiController {
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final MobileTokenService tokenService;
    private final RoomStatusService roomStatusService;

    public HousekeepingApiController(RoomRepository roomRepository,
                                     RoomTypeRepository roomTypeRepository,
                                     MobileTokenService tokenService,
                                     RoomStatusService roomStatusService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.tokenService = tokenService;
        this.roomStatusService = roomStatusService;
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return withHousekeeper(authorization, claims -> {
            Map<Long, RoomType> types = roomTypeRepository.findAll().stream()
                    .collect(Collectors.toMap(RoomType::getId, type -> type));
            List<Map<String, Object>> rooms = roomRepository.findAll().stream()
                    .sorted(Comparator.comparing(Room::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .map(room -> toDto(room, types.get(room.getRoomTypeId())))
                    .toList();
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-store, no-cache, must-revalidate")
                    .header("X-Hotel-Date", LocalDate.now(HOTEL_ZONE).toString())
                    .header("X-Generated-At", ZonedDateTime.now(HOTEL_ZONE).toString())
                    .body(ApiResponse.success("Lấy sơ đồ phòng thành công", rooms));
        });
    }

    @PatchMapping("/rooms/{roomId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long roomId,
                                          @RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestBody Map<String, Object> payload) {
        return withHousekeeper(authorization, claims -> {
            try {
                RoomStatus expected = RoomStatus.fromApi(stringValue(payload.get("expectedStatus")));
                RoomStatus target = RoomStatus.fromApi(stringValue(payload.get("status")));
                Room updated = roomStatusService.updateByHousekeeper(
                        roomId,
                        expected,
                        target,
                        stringValue(payload.get("statusNote")),
                        claims.userId());
                RoomType type = updated.getRoomTypeId() == null
                        ? null
                        : roomTypeRepository.findById(updated.getRoomTypeId()).orElse(null);
                return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái phòng thành công", toDto(updated, type)));
            } catch (RoomStatusConflictException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
            }
        });
    }

    private ResponseEntity<?> withHousekeeper(
            String authorization,
            java.util.function.Function<MobileTokenService.Claims, ResponseEntity<?>> action) {
        try {
            MobileTokenService.Claims claims = tokenService.verifyAuthorization(authorization);
            if (claims.roleId() != 4L && !"HOUSEKEEPER".equalsIgnoreCase(claims.role())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Chức năng chỉ dành cho nhân viên buồng phòng"));
            }
            return action.apply(claims);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
        }
    }

    private Map<String, Object> toDto(Room room, RoomType roomType) {
        RoomStatus status = RoomStatus.fromStored(room.getStatus());
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", room.getId());
        dto.put("name", room.getName());
        dto.put("roomType", roomType == null ? "Chưa phân loại" : roomType.getName());
        dto.put("capacity", roomType == null || roomType.getMaxOccupancy() == null ? 2 : roomType.getMaxOccupancy());
        dto.put("status", status.name());
        dto.put("statusLabel", status.getLabel());
        dto.put("maTrangThai", status.getMapCode());
        dto.put("statusNote", room.getStatusNote());
        dto.put("statusUpdatedAt", room.getStatusUpdatedAt());
        dto.put("statusUpdatedBy", room.getStatusUpdatedBy());
        dto.put("hasCleaningOrder", roomStatusService.hasActiveHousekeepingOrder(room));
        dto.put("cleaningRequestedAt", room.getHousekeepingOrderCreatedAt() == null
                && status == RoomStatus.CLEANING ? room.getStatusUpdatedAt() : room.getHousekeepingOrderCreatedAt());
        return dto;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
