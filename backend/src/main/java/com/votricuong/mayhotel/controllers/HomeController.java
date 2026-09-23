package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.RoomRepository;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController extends BaseController {

    private final RoomRepository roomRepository;
    private final com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository;

    public HomeController(RoomRepository roomRepository, com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    // DTO for Thymeleaf backwards compatibility
    @Data
    @Builder
    public static class RoomTypeDTO {
        private Long maLoai;
        private String name;
        private Integer soNguoi;
        private Double price;
        private List<Room> phongs;
        private List<Room> phongsTrong;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Room> allRooms = roomRepository.findAll();

        // Group rooms by RoomType ID
        Map<Long, List<Room>> roomsByType = allRooms.stream()
                .filter(r -> r.getRoomTypeId() != null)
                .collect(Collectors.groupingBy(Room::getRoomTypeId));

        java.util.Map<Long, RoomType> typeMap = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        List<RoomTypeDTO> loaiPhongs = roomsByType.entrySet().stream().map(entry -> {
            RoomType type = typeMap.get(entry.getKey());
            String typeName = (type != null && type.getName() != null) ? type.getName() : "Phòng tiêu chuẩn";
            Integer soNguoi = (type != null && type.getMaxOccupancy() != null) ? type.getMaxOccupancy() : Integer.valueOf(2);
            
            List<Room> phongs = entry.getValue();
            // Gán ảnh mặc định từ loại phòng cho từng phòng nếu phòng chưa có ảnh
            phongs.forEach(room -> {
                if (room.getImageUrl() == null && type != null) {
                    room.setImageUrl(type.getImageUrl());
                }
            });

            List<Room> phongsTrong = phongs.stream()
                    .filter(r -> "Vacant".equalsIgnoreCase(r.getStatus()) || "Còn phòng".equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());

            return RoomTypeDTO.builder()
                    .maLoai(entry.getKey())
                    .name(typeName)
                    .soNguoi(soNguoi)
                    .price(phongs.isEmpty() ? 0.0 : phongs.get(0).getPrice())
                    .phongs(phongs)
                    .phongsTrong(phongsTrong)
                    .build();
        }).collect(Collectors.toList());

        // Sắp xếp các loại phòng theo số lượng phòng đã có người đặt (nhiều người đặt nhất lên đầu)
        // Số phòng đã đặt = Tổng số phòng - Số phòng trống
        loaiPhongs.sort((a, b) -> {
            int bookedA = a.getPhongs().size() - a.getPhongsTrong().size();
            int bookedB = b.getPhongs().size() - b.getPhongsTrong().size();
            return Integer.compare(bookedB, bookedA); // Giảm dần
        });

        // Chỉ lấy tối đa 4 loại phòng nổi bật nhất để show trang chủ cho đẹp
        List<RoomTypeDTO> featuredRooms = loaiPhongs.stream().limit(4).collect(Collectors.toList());

        model.addAttribute("listLoaiPhong", featuredRooms); 
        setExtraCSS(model, "view/Home/index :: extra_css");
        setExtraJS(model, "view/Home/index :: extra_js");
        return render(model, "view/Home/index");
    }

    @GetMapping("/info")
    public String info(Model model) {
        setPageTitle(model, "Thông tin khách sạn");
        setExtraCSS(model, "view/Home/info :: extra_css");
        setExtraJS(model, "view/Home/info :: extra_js");
        return render(model, "view/Home/info");
    }

    @GetMapping("/home/detail/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Room room = roomRepository.findById(id).orElse(null);
        if (room == null) {
            return "redirect:/";
        }
        model.addAttribute("phong", room);
        
        boolean isVacant = "Vacant".equalsIgnoreCase(room.getStatus());
        model.addAttribute("conPhong", isVacant);
        
        model.addAttribute("pageTitle", "Chi tiết phòng " + room.getName());
        return render(model, "view/Home/detail");
    }
}
