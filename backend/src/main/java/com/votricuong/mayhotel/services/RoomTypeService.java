package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomTypeService {
    private final RoomTypeRepository roomTypeRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final RoomRepository roomRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository, SequenceGeneratorService sequenceGeneratorService,
                           RoomRepository roomRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.roomRepository = roomRepository;
    }

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public Optional<RoomType> getRoomTypeById(Long id) {
        return roomTypeRepository.findById(id);
    }

    public RoomType createRoomType(RoomType roomType) {
        validate(roomType, null);
        roomType.setId(sequenceGeneratorService.generateSequence("room_types_sequence"));
        return roomTypeRepository.save(roomType);
    }

    public RoomType updateRoomType(Long id, RoomType roomTypeDetails) throws Exception {
        Optional<RoomType> typeOpt = roomTypeRepository.findById(id);
        if (typeOpt.isEmpty()) {
            throw new Exception("Không tìm thấy loại phòng.");
        }
        validate(roomTypeDetails, id);
        RoomType roomType = typeOpt.get();
        roomType.setName(roomTypeDetails.getName());
        roomType.setMaxOccupancy(roomTypeDetails.getMaxOccupancy());
        roomType.setImageUrl(roomTypeDetails.getImageUrl());
        roomType.setQuantity(roomTypeDetails.getQuantity());

        return roomTypeRepository.save(roomType);
    }

    public void deleteRoomType(Long id) throws Exception {
        if (!roomTypeRepository.existsById(id)) {
            throw new Exception("Không tìm thấy loại phòng.");
        }
        if (!roomRepository.findByRoomTypeId(id).isEmpty()) {
            throw new Exception("Không thể xóa loại phòng đang được sử dụng bởi phòng");
        }
        roomTypeRepository.deleteById(id);
    }

    private void validate(RoomType roomType, Long excludedId) {
        if (roomType == null) throw new IllegalArgumentException("Dữ liệu loại phòng không được để trống");
        roomType.setName(com.votricuong.mayhotel.utils.InputValidator.requiredText(roomType.getName(), "Tên loại phòng", 100));
        roomType.setMaxOccupancy(com.votricuong.mayhotel.utils.InputValidator.range(
                roomType.getMaxOccupancy(), "Sức chứa", 1, 20));
        if (roomType.getQuantity() != null) {
            com.votricuong.mayhotel.utils.InputValidator.range(roomType.getQuantity(), "Số lượng phòng", 0, 10000);
        }
        boolean duplicate = roomTypeRepository.findAll().stream().anyMatch(existing ->
                existing.getId() != null && !existing.getId().equals(excludedId)
                        && existing.getName() != null && existing.getName().equalsIgnoreCase(roomType.getName()));
        if (duplicate) throw new IllegalArgumentException("Tên loại phòng đã tồn tại");
    }
}
