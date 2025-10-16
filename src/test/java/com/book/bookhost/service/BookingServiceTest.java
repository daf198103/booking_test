package com.book.bookhost.service;

import com.book.bookhost.dto.BookingRequest;
import com.book.bookhost.exception.NotFoundException;
import com.book.bookhost.exception.ValidationException;
import com.book.bookhost.model.*;
import com.book.bookhost.repository.BookingRepository;
import com.book.bookhost.repository.BlockingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BlockingRepository blockRepository;

    @InjectMocks
    private BookingService bookingService;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void createBooking_successful() {
        BookingRequest request = new BookingRequest(
                "Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(1), today.plusDays(3)
        );

        when(bookingRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(blockRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(request);

        assertNotNull(booking);
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_startDateAfterEndDate_throwsException() {
        BookingRequest request = new BookingRequest(
                "Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(5), today.plusDays(1)
        );

        assertThrows(ValidationException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void createBooking_startDateBeforeToday_throwsException() {
        BookingRequest request = new BookingRequest(
                "Sarah Connor", "sarah@skynet.com", "prop1",
                today.minusDays(1), today.plusDays(1)
        );

        assertThrows(ValidationException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void createBooking_overlapWithExistingBooking_throwsException() {
        Booking existing = new Booking("Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(2), today.plusDays(4), BookingStatus.ACTIVE);
        existing.setId(100L);

        when(bookingRepository.findByPropertyId("prop1"))
                .thenReturn(List.of(existing));
        when(blockRepository.findByPropertyId("prop1"))
                .thenReturn(Collections.emptyList());

        BookingRequest request = new BookingRequest(
                "Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(3), today.plusDays(5)
        );

        assertThrows(ValidationException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void createBooking_overlapWithBlock_throwsException() {
        Block block = new Block("prop1", today.plusDays(1), today.plusDays(3), "Maintenance");
        when(bookingRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(blockRepository.findByPropertyId("prop1")).thenReturn(List.of(block));

        BookingRequest request = new BookingRequest(
                "Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(2), today.plusDays(4)
        );

        assertThrows(ValidationException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void updateBooking_successful() {
        Booking existing = new Booking("Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(1), today.plusDays(3), BookingStatus.ACTIVE);
        existing.setId(1L);

        BookingRequest request = new BookingRequest(
                "Sara Connor", "sarah@skynet.com", "prop1",
                today.plusDays(2), today.plusDays(5)
        );

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookingRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(blockRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking updated = bookingService.updateBooking(1L, request);

        assertEquals("Sara Connor", updated.getGuestName());
        assertEquals(request.endDate(), updated.getEndDate());
        verify(bookingRepository, times(1)).save(existing);
    }

    @Test
    void updateBooking_notFound_throwsException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
        BookingRequest request = new BookingRequest("A", "b", "prop1", today.plusDays(1), today.plusDays(2));
        assertThrows(NotFoundException.class, () -> bookingService.updateBooking(99L, request));
    }

    @Test
    void cancelBooking_successful() {
        Booking existing = new Booking("Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(1), today.plusDays(3), BookingStatus.ACTIVE);
        existing.setId(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existing));

        bookingService.cancelBooking(1L);

        assertEquals(BookingStatus.CANCELED, existing.getStatus());
        verify(bookingRepository).save(existing);
    }

    @Test
    void cancelBooking_notFound_throwsException() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bookingService.cancelBooking(10L));
    }

    @Test
    void rebook_successful() {
        Booking canceled = new Booking("Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(1), today.plusDays(3), BookingStatus.CANCELED);
        canceled.setId(5L);

        BookingRequest req = new BookingRequest("Sarah Connor", "sarah@skynet.com", "prop1", today.plusDays(4), today.plusDays(6));

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(canceled));
        when(bookingRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(blockRepository.findByPropertyId("prop1")).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking rebooked = bookingService.rebook(5L, req);

        assertEquals(BookingStatus.ACTIVE, rebooked.getStatus());
        assertEquals("Sarah Connor", rebooked.getGuestName());
    }

    @Test
    void rebook_nonCanceled_throwsException() {
        Booking active = new Booking("Sarah Connor", "sarah@skynet.com", "prop1",
                today.plusDays(1), today.plusDays(3), BookingStatus.ACTIVE);
        active.setId(5L);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(active));

        BookingRequest req = new BookingRequest("Sarah Connor", "sarah@skynet.com", "prop1", today.plusDays(4), today.plusDays(6));

        assertThrows(ValidationException.class, () -> bookingService.rebook(5L, req));
    }


    @Test
    void getBooking_successful() {
        Booking booking = new Booking("Sarah Connor", "sarah@skynet.com", "prop1", today.plusDays(1), today.plusDays(3), BookingStatus.ACTIVE);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        Booking result = bookingService.getBooking(1L);

        assertEquals("Sarah Connor", result.getGuestName());
    }

    @Test
    void getBooking_notFound_throwsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bookingService.getBooking(1L));
    }


    @Test
    void deleteBooking_successful() {
        Booking existing = new Booking("Sarah Connor", "sarah@skynet.com", "prop1", today.plusDays(1), today.plusDays(2), BookingStatus.ACTIVE);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existing));

        bookingService.deleteBooking(1L);

        assertEquals(BookingStatus.DELETED, existing.getStatus());
        verify(bookingRepository).save(existing);
    }

    @Test
    void deleteBooking_notFound_throwsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bookingService.deleteBooking(1L));
    }

    @Test
    void getActiveBookingByGuestName_successful() {
        Booking activeBooking = new Booking(
                "John Connor", "john@skynet.com", "prop1",
                today.plusDays(1), today.plusDays(3), BookingStatus.ACTIVE
        );

        when(bookingRepository.getActiveBookingByGuestName("John Connor"))
                .thenReturn(Optional.of(activeBooking));

        Booking result = bookingService.getActiveBookingByGuestName("John Connor");

        assertNotNull(result);
        assertEquals("John Connor", result.getGuestName());
        assertEquals(BookingStatus.ACTIVE, result.getStatus());
        verify(bookingRepository).getActiveBookingByGuestName("John Connor");
    }

    @Test
    void getActiveBookingByGuestName_notFound_throwsException() {
        when(bookingRepository.getActiveBookingByGuestName("Sarah Connor"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> bookingService.getActiveBookingByGuestName("Sarah Connor"));
    }

}
