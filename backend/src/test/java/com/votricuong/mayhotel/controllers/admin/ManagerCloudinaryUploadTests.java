package com.votricuong.mayhotel.controllers.admin;

import com.votricuong.mayhotel.documents.Room;
import com.votricuong.mayhotel.documents.RoomType;
import com.votricuong.mayhotel.repositories.BookingDetailRepository;
import com.votricuong.mayhotel.repositories.RoomRepository;
import com.votricuong.mayhotel.repositories.RoomTypeRepository;
import com.votricuong.mayhotel.services.BookingAvailabilityService;
import com.votricuong.mayhotel.services.CloudinaryService;
import com.votricuong.mayhotel.services.RoomTypeService;
import com.votricuong.mayhotel.services.SequenceGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerCloudinaryUploadTests {
    private RoomRepository roomRepository;
    private RoomTypeRepository roomTypeRepository;
    private SequenceGeneratorService sequenceGenerator;
    private CloudinaryService cloudinaryService;
    private ManagerRoomController roomController;

    @BeforeEach
    void setUp() {
        roomRepository = mock(RoomRepository.class);
        roomTypeRepository = mock(RoomTypeRepository.class);
        sequenceGenerator = mock(SequenceGeneratorService.class);
        cloudinaryService = mock(CloudinaryService.class);
        roomController = new ManagerRoomController();
        ReflectionTestUtils.setField(roomController, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(roomController, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(roomController, "sequenceGenerator", sequenceGenerator);
        ReflectionTestUtils.setField(roomController, "bookingDetailRepository", mock(BookingDetailRepository.class));
        ReflectionTestUtils.setField(roomController, "bookingAvailabilityService", mock(BookingAvailabilityService.class));
        ReflectionTestUtils.setField(roomController, "cloudinaryService", cloudinaryService);
    }

    @Test
    void createRoomDeletesTheNewCloudinaryAssetWhenMongoSaveFails() {
        CloudinaryService.UploadResult uploaded = new CloudinaryService.UploadResult(
                "https://res.cloudinary.com/hotel/image/upload/v1/may_hotel/rooms/new.png",
                "may_hotel/rooms/new");
        when(roomTypeRepository.existsById(1L)).thenReturn(true);
        when(roomRepository.findAll()).thenReturn(List.of());
        when(sequenceGenerator.generateSequence("rooms_sequence")).thenReturn(101L);
        when(cloudinaryService.uploadImage(any(), any())).thenReturn(uploaded);
        when(roomRepository.save(any(Room.class))).thenThrow(new IllegalStateException("MongoDB unavailable"));

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        roomController.storeRoom(validRoom(null), imageFile(), redirect);

        verify(cloudinaryService).deleteImage("may_hotel/rooms/new");
        assertThat(redirect.getFlashAttributes().get("error").toString())
                .contains("MongoDB unavailable");
    }

    @Test
    void editRoomSavesTheNewUrlBeforeDeletingTheOldCloudinaryAsset() {
        String oldUrl = "https://res.cloudinary.com/hotel/image/upload/v1/may_hotel/rooms/old.png";
        Room existing = validRoom(101L);
        existing.setImageUrl(oldUrl);
        CloudinaryService.UploadResult uploaded = new CloudinaryService.UploadResult(
                "https://res.cloudinary.com/hotel/image/upload/v1/may_hotel/rooms/new.png",
                "may_hotel/rooms/new");
        when(roomRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(roomTypeRepository.existsById(1L)).thenReturn(true);
        when(roomRepository.findAll()).thenReturn(List.of(existing));
        when(cloudinaryService.uploadImage(any(), any())).thenReturn(uploaded);
        when(cloudinaryService.extractPublicId(oldUrl)).thenReturn("may_hotel/rooms/old");
        when(cloudinaryService.deleteImage("may_hotel/rooms/old")).thenReturn(true);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roomController.editRoom(validRoom(101L), imageFile(), new RedirectAttributesModelMap());

        InOrder order = inOrder(roomRepository, cloudinaryService);
        order.verify(roomRepository).save(any(Room.class));
        order.verify(cloudinaryService).deleteImage("may_hotel/rooms/old");
        assertThat(existing.getImageUrl()).isEqualTo(uploaded.secureUrl());
    }

    @Test
    void createCategoryDeletesTheNewAssetWhenMongoSaveFails() {
        ManagerCategoryController categoryController = new ManagerCategoryController();
        ReflectionTestUtils.setField(categoryController, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(categoryController, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(categoryController, "sequenceGenerator", sequenceGenerator);
        ReflectionTestUtils.setField(categoryController, "roomTypeService", mock(RoomTypeService.class));
        ReflectionTestUtils.setField(categoryController, "bookingAvailabilityService", mock(BookingAvailabilityService.class));
        ReflectionTestUtils.setField(categoryController, "cloudinaryService", cloudinaryService);
        CloudinaryService.UploadResult uploaded = new CloudinaryService.UploadResult(
                "https://res.cloudinary.com/hotel/image/upload/v1/may_hotel/categories/new.png",
                "may_hotel/categories/new");
        when(roomTypeRepository.findAll()).thenReturn(List.of());
        when(sequenceGenerator.generateSequence("room_types_sequence")).thenReturn(8L);
        when(cloudinaryService.uploadImage(any(), any())).thenReturn(uploaded);
        when(roomTypeRepository.save(any(RoomType.class))).thenThrow(new IllegalStateException("MongoDB unavailable"));

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        categoryController.storeCategory(
                RoomType.builder().name("Phòng mới").maxOccupancy(2).build(),
                imageFile(), redirect);

        verify(cloudinaryService).deleteImage("may_hotel/categories/new");
        assertThat(redirect.getFlashAttributes().get("error").toString())
                .contains("MongoDB unavailable");
    }

    private Room validRoom(Long id) {
        return Room.builder()
                .id(id)
                .name("P101")
                .price(500_000.0)
                .roomTypeId(1L)
                .maxExtraBeds(0)
                .status("Trống")
                .build();
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile(
                "imageFile", "room.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
    }
}
