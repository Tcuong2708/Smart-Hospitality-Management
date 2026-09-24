package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.controllers.BaseController;
import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager")
public class ManagerRoomController extends BaseController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    @GetMapping("/rooms")
    public String roomsIndex(Model model) {
        setPageTitle(model, "Quản Lý Phòng - Manager");
        
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        Map<Long, RoomType> typeMap = roomTypes.stream()
                .collect(Collectors.toMap(RoomType::getId, t -> t));

        List<Room> rooms = roomRepository.findAll();
        for (Room room : rooms) {
            if (room.getRoomTypeId() != null && typeMap.containsKey(room.getRoomTypeId())) {
                room.setRoomType(typeMap.get(room.getRoomTypeId()));
            }
        }
        
        model.addAttribute("rooms", rooms);
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", roomTypes);
        return render(model, "view/Admin/Room/index");
    }

    @GetMapping("/rooms/create")
    public String createRoom(Model model) {
        setPageTitle(model, "Thêm Phòng Mới - Manager");
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", roomTypeRepository.findAll());
        return render(model, "view/Admin/Room/create");
    }

    @PostMapping("/rooms/create")
    public String storeRoom(Room room, RedirectAttributes redirect) {
        room.setId(sequenceGenerator.generateSequence("rooms_sequence"));
        roomRepository.save(room);
        redirect.addFlashAttribute("success", "Thêm phòng thành công!");
        return "redirect:/manager/rooms";
    }

    @GetMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirect) {
        roomRepository.deleteById(id);
        redirect.addFlashAttribute("success", "Xóa phòng thành công!");
        return "redirect:/manager/rooms";
    }

    @PostMapping("/rooms/edit")
    public String editRoom(Room room, RedirectAttributes redirect) {
        if (room.getId() != null) {
            Room existing = roomRepository.findById(room.getId()).orElse(null);
            if (existing != null) {
                // Update fields
                existing.setName(room.getName());
                existing.setPrice(room.getPrice());
                existing.setImageUrl(room.getImageUrl());
                existing.setRoomTypeId(room.getRoomTypeId());
                existing.setMaxExtraBeds(room.getMaxExtraBeds());
                existing.setStatus(room.getStatus());
                existing.setDetail(room.getDetail());
                existing.setNote(room.getNote());
                
                roomRepository.save(existing);
                redirect.addFlashAttribute("success", "Cập nhật phòng thành công!");
            }
        }
        return "redirect:/manager/rooms";
    }

    @GetMapping("/rooms/details/{id}")
    public String detailsRoom(@PathVariable Long id, Model model) {
        setPageTitle(model, "Chi Tiết Phòng - Manager");
        Room room = roomRepository.findById(id).orElse(null);
        if (room != null && room.getRoomTypeId() != null) {
            room.setRoomType(roomTypeRepository.findById(room.getRoomTypeId()).orElse(null));
        }
        model.addAttribute("room", room);
        return render(model, "view/Admin/Room/details");
    }
}
