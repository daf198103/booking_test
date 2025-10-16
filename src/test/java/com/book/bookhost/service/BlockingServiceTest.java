package com.book.bookhost.service;

import com.book.bookhost.dto.BlockRequest;
import com.book.bookhost.exception.NotFoundException;
import com.book.bookhost.exception.ValidationException;
import com.book.bookhost.model.*;
import com.book.bookhost.repository.BlockingRepository;
import com.book.bookhost.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockingServiceTest {

    @Mock
    private BlockingRepository blockRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BlockingService blockingService;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void createBlock_successful() {
        BlockRequest request = new BlockRequest("prop1", today.plusDays(1), today.plusDays(3), "Maintenance");

        when(bookingRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(blockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Block block = blockingService.createBlock(request);

        assertNotNull(block);
        assertEquals("prop1", block.getPropertyId());
        assertEquals("Maintenance", block.getReason());
        verify(blockRepository).save(any(Block.class));
    }

    @Test
    void createBlock_startAfterEnd_throwsValidationException() {
        BlockRequest request = new BlockRequest("prop1", today.plusDays(5), today.plusDays(2), "Invalid");

        assertThrows(ValidationException.class, () -> blockingService.createBlock(request));
    }

    @Test
    void createBlock_overlapWithExistingBooking_throwsValidationException() {
        BlockRequest request = new BlockRequest("prop1", today.plusDays(2), today.plusDays(5), "Maintenance");

        Booking overlapping = new Booking("T-800", "t800@skynet.com", "prop1",
                today.plusDays(3), today.plusDays(6), BookingStatus.ACTIVE);

        when(bookingRepository.findByPropertyId("prop1")).thenReturn(List.of(overlapping));

        assertThrows(ValidationException.class, () -> blockingService.createBlock(request));
    }

    @Test
    void createBlock_overlapWithCanceledBooking_allowed() {
        BlockRequest request = new BlockRequest("prop1", today.plusDays(1), today.plusDays(3), "Allowed");

        Booking canceled = new Booking("T-800", "t800@skynet.com", "prop1",
                today.plusDays(1), today.plusDays(3), BookingStatus.CANCELED);

        when(bookingRepository.findByPropertyId("prop1")).thenReturn(List.of(canceled));
        when(blockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Block block = blockingService.createBlock(request);

        assertNotNull(block);
        assertEquals("Allowed", block.getReason());
    }

    @Test
    void updateBlock_successful() {
        Block existing = new Block("prop1", today.plusDays(1), today.plusDays(2), "Old reason");
        existing.setId(1L);

        BlockRequest request = new BlockRequest("prop1", today.plusDays(3), today.plusDays(5), "Updated reason");

        when(blockRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookingRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(blockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Block updated = blockingService.updateBlock(1L, request);

        assertEquals("Updated reason", updated.getReason());
        assertEquals(today.plusDays(5), updated.getEndDate());
        verify(blockRepository).save(existing);
    }

    @Test
    void updateBlock_notFound_throwsNotFoundException() {
        BlockRequest request = new BlockRequest("prop1", today.plusDays(1), today.plusDays(3), "X");
        when(blockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> blockingService.updateBlock(99L, request));
    }

    @Test
    void updateBlock_invalidDates_throwsValidationException() {
        Block existing = new Block("prop1", today.plusDays(1), today.plusDays(2), "maintenance");
        when(blockRepository.findById(1L)).thenReturn(Optional.of(existing));

        BlockRequest request = new BlockRequest("prop1", today.plusDays(5), today.plusDays(1), "air conditioner");

        assertThrows(ValidationException.class, () -> blockingService.updateBlock(1L, request));
    }

    @Test
    void updateBlock_overlapWithBooking_throwsValidationException() {
        Block existing = new Block("prop1", today.plusDays(1), today.plusDays(2), "Change Carpet");
        when(blockRepository.findById(1L)).thenReturn(Optional.of(existing));

        Booking overlapping = new Booking("T-800", "t800@skynet.com", "prop1",
                today.plusDays(3), today.plusDays(5), BookingStatus.ACTIVE);

        when(bookingRepository.findByPropertyId("prop1")).thenReturn(List.of(overlapping));

        BlockRequest request = new BlockRequest("prop1", today.plusDays(4), today.plusDays(6), "Overlap");

        assertThrows(ValidationException.class, () -> blockingService.updateBlock(1L, request));
    }


    @Test
    void deleteBlock_successful() {
        Block block = new Block("prop1", today.plusDays(1), today.plusDays(2), "Maintenance");
        when(blockRepository.findById(1L)).thenReturn(Optional.of(block));

        blockingService.deleteBlock(1L);

        verify(blockRepository).delete(block);
    }

    @Test
    void deleteBlock_notFound_throwsNotFoundException() {
        when(blockRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> blockingService.deleteBlock(1L));
    }

    @Test
    void getBlocking_successful() {
        Block block = new Block("prop1", today.plusDays(1), today.plusDays(3), "Maintenance");
        when(blockRepository.findByPropertyId("prop1")).thenReturn(List.of(block));

        List<Block> result = blockingService.getBlocking("prop1");

        assertEquals(1, result.size());
        assertEquals("prop1", result.get(0).getPropertyId());
    }

    @Test
    void getBlocking_notFound_throwsNotFoundException() {
        when(blockRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        assertThrows(NotFoundException.class, () -> blockingService.getBlocking("prop1"));
    }
}
