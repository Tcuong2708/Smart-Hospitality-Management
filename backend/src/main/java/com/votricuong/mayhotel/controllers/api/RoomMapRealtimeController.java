package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.documents.User;
import com.votricuong.mayhotel.enums.RoomStatus;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import com.votricuong.mayhotel.services.RoomStatusEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/receptionist/room-map", "/admin/room-map"})
public class RoomMapRealtimeController {
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingAvailabilityService bookingAvailabilityService;
    private final RoomStatusEventService eventService;

    public RoomMapRealtimeController(RoomRepository roomRepository,
                                     RoomTypeRepository roomTypeRepository,
                                     BookingAvailabilityService bookingAvailabilityService,
                                     RoomStatusEventService eventService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingAvailabilityService = bookingAvailabilityService;
        this.eventService = eventService;
    }

    @GetMapping("/data")
    public ResponseEntity<?> data(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedDate,
            HttpServletRequest request) {
        if (!mayViewRoomMap(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Không có quyền xem sơ đồ phòng"));
        }
        LocalDate selectedDate = requestedDate == null ? LocalDate.now(HOTEL_ZONE) : requestedDate;
        List<Room> rooms = roomRepository.findAll();
        Map<Long, RoomStatus> statuses = bookingAvailabilityService.findRoomStatusesOn(selectedDate, rooms);
        Map<Long, RoomType> types = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, type -> type));

        List<Map<String, Object>> result = rooms.stream()
                .sorted(Comparator.comparing(Room::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(room -> toDto(room, types.get(room.getRoomTypeId()),
                        statuses.getOrDefault(room.getId(), RoomStatus.VACANT)))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(HttpServletRequest request) {
        if (!mayViewRoomMap(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền theo dõi sơ đồ phòng");
        }
        return eventService.subscribe();
    }

    private Map<String, Object> toDto(Room room,
                                      RoomType type,
                                      RoomStatus status) {
        String label = room.getName() == null ? String.valueOf(room.getId()) : room.getName().replaceAll("\\D+", "");
        if (label.isBlank()) label = String.valueOf(room.getId());
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", room.getId());
        dto.put("name", room.getName());
        dto.put("label", label);
        dto.put("maLoai", type == null ? "Chưa phân loại" : type.getName());
        dto.put("price", room.getPrice());
        dto.put("maTrangThai", status.getMapCode());
        dto.put("sucChua", type == null || type.getMaxOccupancy() == null ? 2 : type.getMaxOccupancy());
        dto.put("view", room.getView());
        dto.put("statusNote", room.getStatusNote());
        dto.put("statusUpdatedAt", room.getStatusUpdatedAt());
        return dto;
    }

    private boolean mayViewRoomMap(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getRoleId() == null) return false;
        return user.getRoleId() == 1L || user.getRoleId() == 2L || user.getRoleId() == 3L;
    }
}
