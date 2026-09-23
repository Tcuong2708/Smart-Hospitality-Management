package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.services.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
public class RoomApiController {

    private final RoomService roomService;

    public RoomApiController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        Optional<Room> roomOpt = roomService.getRoomById(id);
        return roomOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody Room room) {
        try {
            Room createdRoom = roomService.createRoom(room);
            return ResponseEntity.ok(createdRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody Room roomDetails) {
        try {
            Room updatedRoom = roomService.updateRoom(id, roomDetails);
            return ResponseEntity.ok(updatedRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateRoomStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            if (status == null || status.isEmpty()) throw new Exception("Trạng thái không hợp lệ");
            Room updatedRoom = roomService.updateRoomStatus(id, status);
            return ResponseEntity.ok(updatedRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.ok("Xóa phòng thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
