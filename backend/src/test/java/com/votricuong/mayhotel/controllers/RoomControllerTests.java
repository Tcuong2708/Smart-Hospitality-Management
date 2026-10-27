package com.votricuong.mayhotel.controllers;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.dto.RatingSnapshot;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.ReviewService;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomControllerTests {

    private RoomRepository roomRepository;
    private RoomTypeRepository roomTypeRepository;
    private RoomController controller;
    private BookingAvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        roomRepository = mock(RoomRepository.class);
        roomTypeRepository = mock(RoomTypeRepository.class);
        ReviewService reviewService = mock(ReviewService.class);
        availabilityService = mock(BookingAvailabilityService.class);
        when(reviewService.snapshot()).thenReturn(RatingSnapshot.empty());
        controller = new RoomController(roomRepository, roomTypeRepository, reviewService, availabilityService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicCatalogueGroupsPhysicalRoomsAndUsesCategoryImage() {
        RoomType deluxe = RoomType.builder().id(5L).name("Phòng Deluxe").imageUrl("d1.jpg").maxOccupancy(2).build();
        RoomType suite = RoomType.builder().id(6L).name("Phòng Suite").imageUrl("/images/su1.jpg").maxOccupancy(2).build();
        Room busyDeluxe = Room.builder().id(705L).name("P705").roomTypeId(5L).price(1_200_000D)
                .imageUrl("Deluxe1.jpg").status("Đang ở").build();
        Room vacantDeluxe = Room.builder().id(718L).name("P718").roomTypeId(5L).price(1_200_000D)
                .status("Trống").build();
        Room busySuite = Room.builder().id(801L).name("P801").roomTypeId(6L).price(1_500_000D)
                .status("Đang ở").build();

        when(roomTypeRepository.findAll()).thenReturn(List.of(deluxe, suite));
        when(roomRepository.findAll()).thenReturn(List.of(busyDeluxe, vacantDeluxe, busySuite));
        when(availabilityService.findAvailableRooms(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of(vacantDeluxe));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.listRooms(
                null, null, null, null, null, null, 0, 0, "all", "default", model);

        assertThat(view).isEqualTo("view/rooms/list");
        List<Room> publicRooms = (List<Room>) model.get("rooms");
        assertThat(publicRooms).hasSize(1);
        assertThat(publicRooms.get(0).getRoomType().getName()).isEqualTo("Phòng Deluxe");
        assertThat(publicRooms.get(0).getImageUrl()).isEqualTo("/images/d1.jpg");
        assertThat(publicRooms.get(0).getStatus()).isEqualTo("Còn phòng");
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicKeywordDoesNotMatchPhysicalRoomCode() {
        RoomType deluxe = RoomType.builder().id(5L).name("Phòng Deluxe").imageUrl("d1.jpg").build();
        Room physicalRoom = Room.builder().id(705L).name("P705").roomTypeId(5L).price(1_200_000D)
                .status("Trống").build();
        when(roomTypeRepository.findAll()).thenReturn(List.of(deluxe));
        when(roomRepository.findAll()).thenReturn(List.of(physicalRoom));
        when(availabilityService.findAvailableRooms(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of(physicalRoom));

        ExtendedModelMap model = new ExtendedModelMap();
        controller.listRooms(
                "P705", null, null, null, null, null, 0, 0, "all", "default", model);

        assertThat((List<Room>) model.get("rooms")).isEmpty();
    }
}
