package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.enums.RoomStatus;
import com.votricuong.mayhotel.exceptions.RoomStatusConflictException;
import com.votricuong.mayhotel.repositories.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomStatusServiceTests {
    private RoomRepository roomRepository;
    private BookingAvailabilityService availabilityService;
    private MongoTemplate mongoTemplate;
    private RoomStatusEventService eventService;
    private RoomStatusService service;

    @BeforeEach
    void setUp() {
        roomRepository = mock(RoomRepository.class);
        availabilityService = mock(BookingAvailabilityService.class);
        mongoTemplate = mock(MongoTemplate.class);
        eventService = mock(RoomStatusEventService.class);
        service = new RoomStatusService(roomRepository, availabilityService, mongoTemplate, eventService);
    }

    @Test
    void completesCleaningAndPublishesTheNewVacantState() {
        Room current = Room.builder().id(101L).name("P101").status("Cleaning").build();
        Room updated = Room.builder().id(101L).name("P101").status("VACANT").statusUpdatedBy(104L).build();
        when(roomRepository.findById(101L)).thenReturn(Optional.of(current));
        when(availabilityService.isRoomOccupiedOn(any(), any(LocalDate.class))).thenReturn(false);
        when(mongoTemplate.findAndModify(any(), any(), any(), org.mockito.ArgumentMatchers.eq(Room.class)))
                .thenReturn(updated);

        Room result = service.updateByHousekeeper(
                101L, RoomStatus.CLEANING, RoomStatus.VACANT, "Đã kiểm tra", 104L);

        assertThat(result.getStatus()).isEqualTo("VACANT");
        verify(eventService).publish(any());
    }

    @Test
    void requiresANoteWhenReportingMaintenance() {
        assertThatThrownBy(() -> service.updateByHousekeeper(
                101L, RoomStatus.CLEANING, RoomStatus.MAINTENANCE, "  ", 104L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bảo trì");
    }

    @Test
    void rejectsAStaleExpectedStatus() {
        when(roomRepository.findById(101L)).thenReturn(Optional.of(
                Room.builder().id(101L).status("MAINTENANCE").build()));
        when(availabilityService.isRoomOccupiedOn(any(), any(LocalDate.class))).thenReturn(false);

        assertThatThrownBy(() -> service.updateByHousekeeper(
                101L, RoomStatus.CLEANING, RoomStatus.VACANT, null, 104L))
                .isInstanceOf(RoomStatusConflictException.class);
    }
}
