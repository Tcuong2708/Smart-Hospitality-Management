package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class RoomController extends BaseController {

    private final RoomRepository roomRepository;
    private final com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository;

    public RoomController(RoomRepository roomRepository, com.votricuong.mayhotel.repositories.RoomTypeRepository roomTypeRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @GetMapping({"/rooms", "/rooms/all"})
    public String listRooms(
            @RequestParam(value = "maLoai", required = false) Long maLoai,
            @RequestParam(value = "priceRange", defaultValue = "") String priceRange,
            @RequestParam(value = "searchString", defaultValue = "") String searchString,
            Model model) {

        setPageTitle(model, "Danh sách phòng nghỉ");
        setExtraCSS(model, "view/Rooms/list :: extra_css");

        // Fetch all rooms from MongoDB first (for MVP we filter in memory, 
        // in production we should write a custom @Query in RoomRepository)
        List<Room> allRooms = roomRepository.findAll();

        // 1. Filter by search string (name)
        if (searchString != null && !searchString.trim().isEmpty()) {
            String lowerSearch = searchString.toLowerCase();
            allRooms = allRooms.stream()
                    .filter(r -> r.getName() != null && r.getName().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }

        // 2. Filter by roomType id (maLoai)
        if (maLoai != null) {
            allRooms = allRooms.stream()
                    .filter(r -> maLoai.equals(r.getRoomTypeId()))
                    .collect(Collectors.toList());
        }

        // 3. Filter by price range
        if (priceRange.equals("lt500")) {
            allRooms = allRooms.stream().filter(r -> r.getPrice() <= 500000).collect(Collectors.toList());
        } else if (priceRange.equals("500-1000")) {
            allRooms = allRooms.stream().filter(r -> r.getPrice() > 500000 && r.getPrice() <= 1000000).collect(Collectors.toList());
        } else if (priceRange.equals("gt2000")) {
            allRooms = allRooms.stream().filter(r -> r.getPrice() > 2000000).collect(Collectors.toList());
        }

        // Nhóm các phòng theo Loại phòng (RoomType) thay vì show lẻ tẻ từng phòng
        java.util.Map<Long, List<Room>> roomsByType = allRooms.stream()
                .filter(r -> r.getRoomTypeId() != null)
                .collect(Collectors.groupingBy(Room::getRoomTypeId));

        java.util.Map<Long, RoomType> typeMap = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        List<com.votricuong.mayhotel.controllers.HomeController.RoomTypeDTO> loaiPhongs = roomsByType.entrySet().stream().map(entry -> {
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

            return com.votricuong.mayhotel.controllers.HomeController.RoomTypeDTO.builder()
                    .maLoai(entry.getKey())
                    .name(typeName)
                    .soNguoi(soNguoi)
                    .price(phongs.isEmpty() ? 0.0 : phongs.get(0).getPrice())
                    .phongs(phongs)
                    .phongsTrong(phongsTrong)
                    .build();
        }).collect(Collectors.toList());

        model.addAttribute("listPhong", loaiPhongs);

        // Get unique room types for the sidebar
        List<RoomType> uniqueRoomTypes = roomTypeRepository.findAll();
        model.addAttribute("listLoai", uniqueRoomTypes); 

        // Keep filter state
        model.addAttribute("currentMaLoai", maLoai);
        model.addAttribute("currentPriceRange", priceRange);
        model.addAttribute("searchString", searchString);

        return render(model, "view/Rooms/list");
    }

    @GetMapping("/rooms/detail/{roomTypeId}")
    public String roomDetailByType(@PathVariable("roomTypeId") Long roomTypeId, Model model) {
        // Tìm tất cả phòng thuộc loại phòng này
        List<Room> roomsOfType = roomRepository.findAll().stream()
                .filter(r -> roomTypeId.equals(r.getRoomTypeId()))
                .collect(Collectors.toList());

        if (roomsOfType.isEmpty()) {
            return "redirect:/rooms";
        }

        // Ưu tiên chọn 1 phòng còn trống để hiển thị (để khách có thể click Đặt Ngay)
        Room room = roomsOfType.stream()
                .filter(r -> "Vacant".equalsIgnoreCase(r.getStatus()) || "Còn phòng".equalsIgnoreCase(r.getStatus()))
                .findFirst()
                .orElse(roomsOfType.get(0)); // Nếu không còn phòng trống thì lấy đại 1 phòng để show thông tin

        if (room != null) {
            // Nạp thông tin loại phòng và hình ảnh từ bảng room_types
            com.votricuong.mayhotel.documents.RoomType type = null;
            if (room.getRoomTypeId() != null) {
                type = roomTypeRepository.findById(room.getRoomTypeId()).orElse(null);
                if (type != null) {
                    room.setRoomType(type);
                    if (room.getImageUrl() == null) {
                        room.setImageUrl(type.getImageUrl());
                    }
                }
            }

            String titleName = (type != null && type.getName() != null) ? type.getName() : "Phòng Tiêu Chuẩn";
            setPageTitle(model, "Chi tiết " + titleName);
            setExtraCSS(model, "view/Rooms/detail :: extra_css");
            setExtraJS(model, "view/Rooms/detail :: extra_js");
            
            model.addAttribute("phong", room);
            return render(model, "view/Rooms/detail");
        }
        return "redirect:/rooms";
    }
}
