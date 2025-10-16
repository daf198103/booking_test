package com.book.bookhost.controller;

import com.book.bookhost.dto.BookingRequest;
import com.book.bookhost.model.Booking;
import com.book.bookhost.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Booking> create(@Valid @RequestBody BookingRequest req) {
        Booking b = service.createBooking(req);
        return ResponseEntity.ok(b);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rebook")
    public ResponseEntity<Booking> rebook(@PathVariable Long id, @Valid @RequestBody BookingRequest req) {
        return ResponseEntity.ok(service.rebook(id, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @Valid @RequestBody BookingRequest req) {
        return ResponseEntity.ok(service.updateBooking(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getBooking(id));
    }

    @GetMapping()
    public ResponseEntity<List<Booking>> getActiveBookingByGuestName(@RequestParam String guestName) {
        return ResponseEntity.ok(service.getActiveBookingByGuestName(guestName));
    }

}
