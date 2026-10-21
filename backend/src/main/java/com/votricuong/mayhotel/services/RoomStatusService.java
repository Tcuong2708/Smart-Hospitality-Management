package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.enums.RoomStatus;
import com.votricuong.mayhotel.exceptions.RoomStatusConflictException;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RoomStatusService {
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RoomRepository roomRepository;
    private final BookingAvailabilityService bookingAvailabilityService;
    private final MongoTemplate mongoTemplate;
    private final RoomStatusEventService eventService;

    public RoomStatusService(RoomRepository roomRepository,
                             BookingAvailabilityService bookingAvailabilityService,
                             MongoTemplate mongoTemplate,
                             RoomStatusEventService eventService) {
        this.roomRepository = roomRepository;
        this.bookingAvailabilityService = bookingAvailabilityService;
        this.mongoTemplate = mongoTemplate;
        this.eventService = eventService;
    }

    public Room markCleaningAfterCheckout(Long roomId, Long updatedBy) {
        if (roomId == null) {
            throw new IllegalArgumentException("Phòng checkout không hợp lệ");
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng #" + roomId));
        return updateAtomically(room, RoomStatus.CLEANING, "Chờ nhân viên buồng phòng kiểm tra", updatedBy);
    }

    public Room updateByHousekeeper(Long roomId,
                                    RoomStatus expectedStatus,
                                    RoomStatus targetStatus,
                                    String statusNote,
                                    Long employeeId) {
        if (expectedStatus == null) {
            throw new IllegalArgumentException("Thiếu trạng thái hiện tại của phòng");
        }
        if (targetStatus != RoomStatus.VACANT && targetStatus != RoomStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Nhân viên buồng phòng chỉ được cập nhật Trống hoặc Bảo trì");
        }
        boolean allowed = expectedStatus == RoomStatus.CLEANING
                || (expectedStatus == RoomStatus.MAINTENANCE && targetStatus == RoomStatus.VACANT);
        if (!allowed) {
            throw new RoomStatusConflictException("Phòng không còn ở trạng thái có thể xử lý");
        }

        String normalizedNote = normalizeNote(statusNote);
        if (targetStatus == RoomStatus.MAINTENANCE && normalizedNote == null) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung cần bảo trì");
        }
        if (bookingAvailabilityService.isRoomOccupiedOn(roomId, LocalDate.now(HOTEL_ZONE))) {
            throw new RoomStatusConflictException("Phòng vẫn có khách đang lưu trú, không thể cập nhật trạng thái");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng #" + roomId));
        RoomStatus actualStatus = RoomStatus.fromStored(room.getStatus());
        if (actualStatus != expectedStatus) {
            throw new RoomStatusConflictException("Trạng thái phòng đã thay đổi, vui lòng tải lại sơ đồ");
        }
        return updateAtomically(room, targetStatus, normalizedNote, employeeId);
    }

    private Room updateAtomically(Room room, RoomStatus target, String note, Long updatedBy) {
        Date updatedAt = new Date();
        Query query = Query.query(Criteria.where("_id").is(room.getId())
                .and("status").is(room.getStatus()));
        Update update = new Update()
                .set("status", target.name())
                .set("status_note", note)
                .set("status_updated_at", updatedAt)
                .set("status_updated_by", updatedBy);
        Room updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Room.class);
        if (updated == null) {
            throw new RoomStatusConflictException("Trạng thái phòng đã thay đổi, vui lòng tải lại sơ đồ");
        }
        eventService.publish(toEvent(updated, target));
        return updated;
    }

    private Map<String, Object> toEvent(Room room, RoomStatus status) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("roomId", room.getId());
        event.put("roomName", room.getName());
        event.put("status", status.name());
        event.put("statusLabel", status.getLabel());
        event.put("statusNote", room.getStatusNote());
        event.put("updatedAt", room.getStatusUpdatedAt());
        event.put("updatedBy", room.getStatusUpdatedBy());
        return event;
    }

    private String normalizeNote(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Nội dung cập nhật không được vượt quá 500 ký tự");
        }
        return normalized;
    }
}
