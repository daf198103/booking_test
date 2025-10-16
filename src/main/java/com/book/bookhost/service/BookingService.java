package com.book.bookhost.service;

import com.book.bookhost.dto.BookingRequest;
import com.book.bookhost.exception.ValidationException;
import com.book.bookhost.exception.NotFoundException;
import com.book.bookhost.model.Booking;
import com.book.bookhost.model.BookingStatus;
import com.book.bookhost.repository.BlockingRepository;
import com.book.bookhost.repository.BookingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.book.bookhost.util.DateUtils.overlap;
import static com.book.bookhost.util.DateUtils.validateDates;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BlockingRepository blockRepository;

    public BookingService(BookingRepository bookingRepository, BlockingRepository blockRepository) {
        this.bookingRepository = bookingRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {
        validateDates(request.startDate(), request.endDate());
        ensureNoOverlapWithBookingsOrBlocks(request.propertyId(), request.startDate(), request.endDate(), null);
        Booking b = new Booking(request.guestName(), request.guestEmail(), request.propertyId(), request.startDate(), request.endDate(),BookingStatus.ACTIVE);
        return bookingRepository.save(b);
    }

    @Transactional
    public Booking updateBooking(Long id, BookingRequest request) {
        Booking existing = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        validateDates(request.startDate(), request.endDate());
        ensureNoOverlapWithBookingsOrBlocks(request.propertyId(), request.startDate(), request.endDate(), id);
        existing.setGuestName(request.guestName());
        existing.setGuestEmail(request.guestEmail());
        existing.setPropertyId(request.propertyId());
        existing.setStartDate(request.startDate());
        existing.setEndDate(request.endDate());
        return bookingRepository.save(existing);
    }

    @Transactional
    public void cancelBooking(Long id) {
        Booking existing = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (existing.getStatus() == BookingStatus.CANCELED) return;
        existing.setStatus(BookingStatus.CANCELED);
        bookingRepository.save(existing);
    }

    @Transactional
    public Booking rebook(Long id, BookingRequest request) {
        Booking existing = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (existing.getStatus() != BookingStatus.CANCELED) {
            throw new ValidationException("Only canceled bookings can be rebooked");
        }
        // reuse existing id but validate overlap
        validateDates(request.startDate(), request.endDate());
        ensureNoOverlapWithBookingsOrBlocks(request.propertyId(), request.startDate(), request.endDate(), id);
        existing.setGuestName(request.guestName());
        existing.setGuestEmail(request.guestEmail());
        existing.setPropertyId(request.propertyId());
        existing.setStartDate(request.startDate());
        existing.setEndDate(request.endDate());
        existing.setStatus(BookingStatus.ACTIVE);
        return bookingRepository.save(existing);
    }

    public Booking getBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    public Booking getActiveBookingByGuestName(String guestName) {

        return bookingRepository.getActiveBookingByGuestName(guestName).orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking existing = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        existing.setStatus(BookingStatus.DELETED);
        bookingRepository.save(existing);
    }

    private void ensureNoOverlapWithBookingsOrBlocks(String propertyId, LocalDate start, LocalDate end, Long ignoreBookingId) {
        // check bookings (only non-canceled)
        List<Booking> bookings = bookingRepository.findByPropertyId(propertyId);
        bookings.stream().filter(book -> book.getStatus() != BookingStatus.CANCELED)
                .filter(book -> !Objects.equals(book.getId(),ignoreBookingId))
                .forEach(book -> {
                    if(overlap(start,end,book.getStartDate(),book.getEndDate())){
                        throw new ValidationException("Requested dates overlap existing Booking id: " + book.getId());
                    }
                });

        // check blocks
        blockRepository.findByPropertyId(propertyId).stream()
                .forEach(block -> {
                    if (overlap(start, end, block.getStartDate(), block.getEndDate())) {
                        throw new ValidationException("Requested dates overlap block propertyId = " + block.getPropertyId());
                    }
                });
    }

}