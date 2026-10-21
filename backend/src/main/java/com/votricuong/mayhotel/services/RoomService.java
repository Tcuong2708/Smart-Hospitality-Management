package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final RoomTypeRepository roomTypeRepository;
    private final com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository;

    public RoomService(RoomRepository roomRepository, SequenceGeneratorService sequenceGeneratorService,
                       RoomTypeRepository roomTypeRepository,
                       com.votricuong.mayhotel.repositories.BookingDetailRepository bookingDetailRepository) {
        this.roomRepository = roomRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingDetailRepository = bookingDetailRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public Room createRoom(Room room) {
        validate(room, null);
        room.setId(sequenceGeneratorService.generateSequence("rooms_sequence"));
        if (room.getStatus() == null || room.getStatus().isEmpty()) {
            room.setStatus("Trống");
        }
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room roomDetails) throws Exception {
        Optional<Room> roomOpt = roomRepository.findById(id);
        if (roomOpt.isEmpty()) {
            throw new Exception("Không tìm thấy phòng.");
        }
        validate(roomDetails, id);
        Room room = roomOpt.get();
        room.setName(roomDetails.getName());
        room.setPrice(roomDetails.getPrice());
        room.setDetail(roomDetails.getDetail());
        room.setImageUrl(roomDetails.getImageUrl());
        room.setRoomTypeId(roomDetails.getRoomTypeId());
        room.setStatus(roomDetails.getStatus());
        room.setNote(roomDetails.getNote());
        room.setMaxExtraBeds(roomDetails.getMaxExtraBeds());

        return roomRepository.save(room);
    }
    
    public Room updateRoomStatus(Long id, String status) throws Exception {
        Optional<Room> roomOpt = roomRepository.findById(id);
        if (roomOpt.isEmpty()) {
            throw new Exception("Không tìm thấy phòng.");
        }
        Room room = roomOpt.get();
        room.setStatus(com.votricuong.mayhotel.enums.RoomStatus.fromApi(status).name());
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) throws Exception {
        if (!roomRepository.existsById(id)) {
            throw new Exception("Không tìm thấy phòng.");
        }
        if (bookingDetailRepository.existsByRoomId(id)) {
            throw new Exception("Không thể xóa phòng đã phát sinh đặt phòng; hãy chuyển sang trạng thái bảo trì");
        }
        roomRepository.deleteById(id);
    }

    private void validate(Room room, Long excludedId) {
        if (room == null) throw new IllegalArgumentException("Dữ liệu phòng không được để trống");
        room.setName(com.votricuong.mayhotel.utils.InputValidator.requiredText(room.getName(), "Tên phòng", 50));
        room.setPrice(com.votricuong.mayhotel.utils.InputValidator.positiveMoney(room.getPrice(), "Giá phòng"));
        room.setDetail(com.votricuong.mayhotel.utils.InputValidator.optionalText(room.getDetail(), "Mô tả phòng", 1000));
        room.setNote(com.votricuong.mayhotel.utils.InputValidator.optionalText(room.getNote(), "Ghi chú phòng", 500));
        com.votricuong.mayhotel.utils.InputValidator.range(
                room.getMaxExtraBeds() == null ? 0 : room.getMaxExtraBeds(), "Số giường phụ", 0, 10);
        if (room.getRoomTypeId() == null || !roomTypeRepository.existsById(room.getRoomTypeId())) {
            throw new IllegalArgumentException("Loại phòng không tồn tại");
        }
        boolean duplicate = roomRepository.findAll().stream().anyMatch(existing ->
                existing.getId() != null && !existing.getId().equals(excludedId)
                        && existing.getName() != null && existing.getName().equalsIgnoreCase(room.getName()));
        if (duplicate) throw new IllegalArgumentException("Tên phòng đã tồn tại");
    }
}
