package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.services.RoomTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/room-types")
public class RoomTypeApiController {

    private final RoomTypeService roomTypeService;

    public RoomTypeApiController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public ResponseEntity<List<RoomType>> getAllRoomTypes() {
        return ResponseEntity.ok(roomTypeService.getAllRoomTypes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomType> getRoomTypeById(@PathVariable Long id) {
        Optional<RoomType> typeOpt = roomTypeService.getRoomTypeById(id);
        return typeOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRoomType(@RequestBody RoomType roomType) {
        try {
            RoomType createdRoomType = roomTypeService.createRoomType(roomType);
            return ResponseEntity.ok(createdRoomType);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoomType(@PathVariable Long id, @RequestBody RoomType roomTypeDetails) {
        try {
            RoomType updatedRoomType = roomTypeService.updateRoomType(id, roomTypeDetails);
            return ResponseEntity.ok(updatedRoomType);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoomType(@PathVariable Long id) {
        try {
            roomTypeService.deleteRoomType(id);
            return ResponseEntity.ok("Xóa loại phòng thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
