package com.votricuong.mayhotel.controllers.api;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.Service;
import com.votricuong.mayhotel.dto.ApiResponse;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.ServiceRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ai-data")
@CrossOrigin(origins = "*") // Allow Python AI to call this API
public class AiDataApiController {

    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;

    public AiDataApiController(RoomRepository roomRepository, ServiceRepository serviceRepository) {
        this.roomRepository = roomRepository;
        this.serviceRepository = serviceRepository;
    }

    // 1. Get all room types and their pricing
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<Object>> getAllRooms() {
        try {
            List<Room> allRooms = roomRepository.findAll();
            
            // Group by room type for AI to easily understand what types of rooms exist
            List<Map<String, Object>> roomTypesData = allRooms.stream()
                .filter(r -> r.getRoomType() != null)
                .map(Room::getRoomType)
                .distinct()
                .map(rt -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("maLoai", rt.getId());
                    map.put("tenLoai", rt.getName());
                    map.put("soNguoiToiDa", rt.getMaxOccupancy());
                    
                    // Find a sample room of this type to get price and description
                    Room sample = allRooms.stream().filter(r -> r.getRoomType().getId().equals(rt.getId())).findFirst().orElse(null);
                    if (sample != null) {
                        map.put("giaTien", sample.getPrice());
                        map.put("moTa", sample.getDetail());
                        map.put("hinhAnh", sample.getImageUrl() != null ? sample.getImageUrl() : "");
                        
                        long soPhongTrong = allRooms.stream()
                            .filter(r -> r.getRoomType().getId().equals(rt.getId()) && "Vacant".equalsIgnoreCase(r.getStatus()))
                            .count();
                        map.put("soPhongDangTrong", soPhongTrong);
                    }
                    return map;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phòng thành công", roomTypesData));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi lấy dữ liệu: " + e.getMessage()));
        }
    }

    // 2. Check availability
    @GetMapping("/check-availability")
    public ResponseEntity<ApiResponse<Object>> checkAvailability(
            @RequestParam(value = "ngayNhan", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayNhan,
            @RequestParam(value = "ngayTra", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate ngayTra) {
        
        // For MVP, we simply return current vacant rooms
        // Future scope: Calculate based on Invoice checkInDate/checkOutDate overlaps
        try {
            List<Room> vacantRooms = roomRepository.findAll().stream()
                    .filter(r -> "Vacant".equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());
            
            Map<String, Object> data = new HashMap<>();
            data.put("tongSoPhongTrong", vacantRooms.size());
            data.put("chiTiet", vacantRooms);
            
            return ResponseEntity.ok(ApiResponse.success("Kiểm tra phòng trống thành công", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi kiểm tra phòng: " + e.getMessage()));
        }
    }

    // 3. Get all services
    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<Service>>> getServices() {
        try {
            List<Service> services = serviceRepository.findAll();
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách dịch vụ thành công", services));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi lấy dịch vụ: " + e.getMessage()));
        }
    }
}
