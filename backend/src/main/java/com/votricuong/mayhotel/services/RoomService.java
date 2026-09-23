package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public RoomService(RoomRepository roomRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.roomRepository = roomRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public Room createRoom(Room room) {
        room.setId(sequenceGeneratorService.generateSequence("rooms_sequence"));
        if (room.getStatus() == null || room.getStatus().isEmpty()) {
            room.setStatus("Phòng trống");
        }
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room roomDetails) throws Exception {
        Optional<Room> roomOpt = roomRepository.findById(id);
        if (roomOpt.isEmpty()) {
            throw new Exception("Không tìm thấy phòng.");
        }
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
        room.setStatus(status);
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) throws Exception {
        if (!roomRepository.existsById(id)) {
            throw new Exception("Không tìm thấy phòng.");
        }
        roomRepository.deleteById(id);
    }
}
