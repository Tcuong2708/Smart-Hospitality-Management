package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends MongoRepository<Room, Long> {
    List<Room> findByStatus(String status);
    List<Room> findByRoomTypeId(Long roomTypeId);
}
