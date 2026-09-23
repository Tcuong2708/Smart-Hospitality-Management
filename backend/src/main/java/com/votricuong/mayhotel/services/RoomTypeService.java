package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomTypeService {
    private final RoomTypeRepository roomTypeRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public RoomTypeService(RoomTypeRepository roomTypeRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.roomTypeRepository = roomTypeRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public Optional<RoomType> getRoomTypeById(Long id) {
        return roomTypeRepository.findById(id);
    }

    public RoomType createRoomType(RoomType roomType) {
        roomType.setId(sequenceGeneratorService.generateSequence("room_types_sequence"));
        return roomTypeRepository.save(roomType);
    }

    public RoomType updateRoomType(Long id, RoomType roomTypeDetails) throws Exception {
        Optional<RoomType> typeOpt = roomTypeRepository.findById(id);
        if (typeOpt.isEmpty()) {
            throw new Exception("Không tìm thấy loại phòng.");
        }
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
        roomTypeRepository.deleteById(id);
    }
}
