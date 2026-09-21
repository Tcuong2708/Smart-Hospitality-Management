package com.votricuong.mayhotel.repositories;

import com.votricuong.mayhotel.documents.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomTypeRepository extends MongoRepository<Room.RoomType, Long> {
}
