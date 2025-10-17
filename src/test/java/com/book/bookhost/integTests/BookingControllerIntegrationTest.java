package com.book.bookhost.integTests;

import com.book.bookhost.dto.BookingRequest;
import com.book.bookhost.model.Booking;
import com.book.bookhost.model.BookingStatus;
import com.book.bookhost.repository.BookingRepository;
import com.book.bookhost.repository.BlockingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BlockingRepository blockRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private LocalDate startDate;
    private LocalDate endDate;



    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        blockRepository.deleteAll();
        startDate = LocalDate.now().plusDays(1);
        endDate = LocalDate.now().plusDays(3);
    }

    @Test
    void createBooking_success() throws Exception {
        BookingRequest request = new BookingRequest("John Connor", "theone@test.com", "prop1", startDate, endDate);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestName", is("John Connor")))
                .andExpect(jsonPath("$.status", is(BookingStatus.ACTIVE.name())));
    }

    @Test
    void createBooking_invalidDates_shouldFail() throws Exception {
        BookingRequest request = new BookingRequest("John Connor", "theone@test.com", "prop1", endDate, startDate);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void cancelBooking_success() throws Exception {
        Booking booking = bookingRepository.save(new Booking("John Connor", "theone@test.com", "prop1", startDate, endDate, BookingStatus.ACTIVE));

        mockMvc.perform(post("/api/bookings/{id}/cancel", booking.getId()))
                .andExpect(status().isNoContent());

        Booking canceled = bookingRepository.findById(booking.getId()).orElseThrow();
        assert(canceled.getStatus() == BookingStatus.CANCELED);
    }

    @Test
    void cancelBooking_notFound() throws Exception {
        mockMvc.perform(post("/api/bookings/{id}/cancel", 1L))
                .andExpect(status().isNotFound());
    }


    @Test
    void rebookBooking_success() throws Exception {
        Booking booking = bookingRepository.save(new Booking("John Connor", "theone@test.com", "prop1", startDate, endDate, BookingStatus.CANCELED));

        BookingRequest request = new BookingRequest("John Connor", "theone@test.com", "prop1", startDate.plusDays(1), endDate.plusDays(1));

        mockMvc.perform(post("/api/bookings/{id}/rebook", booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestName", is("John Connor")))
                .andExpect(jsonPath("$.status", is(BookingStatus.ACTIVE.name())));
    }

    @Test
    void rebookBooking_notCanceled_shouldFail() throws Exception {
        Booking booking = bookingRepository.save(new Booking("John Connor", "theone@test.com", "prop1", startDate, endDate, BookingStatus.ACTIVE));

        BookingRequest request = new BookingRequest("John Connor", "theone@test.com", "prop1", startDate.plusDays(1), endDate.plusDays(1));

        mockMvc.perform(post("/api/bookings/{id}/rebook", booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void updateBooking_success() throws Exception {
        Booking booking = bookingRepository.save(new Booking("John", "theone@test.com", "prop1", startDate, endDate, BookingStatus.ACTIVE));

        BookingRequest request = new BookingRequest("Sarah Connor", "mom_of_theone@test.com", "prop1", startDate.plusDays(1), endDate.plusDays(1));

        mockMvc.perform(put("/api/bookings/{id}", booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestName", is("Sarah Connor")));
    }


    @Test
    void deleteBooking_success() throws Exception {
        Booking booking = bookingRepository.save(new Booking("John Connor", "theone@test.com", "prop1", startDate, endDate, BookingStatus.ACTIVE));

        mockMvc.perform(delete("/api/bookings/{id}", booking.getId()))
                .andExpect(status().isNoContent());

        Booking deleted = bookingRepository.findById(booking.getId()).orElseThrow();
        assert(deleted.getStatus() == BookingStatus.DELETED);
    }


    @Test
    void getBooking_success() throws Exception {
        Booking booking = bookingRepository.save(new Booking("John Connor", "theone@test.com", "prop1", startDate, endDate, BookingStatus.ACTIVE));

        mockMvc.perform(get("/api/bookings/{id}", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestName", is("John Connor")));
    }

    @Test
    void getBooking_notFound() throws Exception {
        mockMvc.perform(get("/api/bookings/{id}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBookingByName_success() throws Exception {
        Booking booking = bookingRepository.save(
                new Booking("John Connor", "theone@test.com", "prop1", startDate, endDate, BookingStatus.ACTIVE)
        );

        mockMvc.perform(get("/api/bookings/guestName")
                        .param("guestName", booking.getGuestName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guestName", is("John Connor")));
    }

    @Test
    void getBookingByName_notFound() throws Exception {
        // Given a booking exists for "John Connor"
        bookingRepository.save(new Booking(
                "John Connor", "theone@test.com", "prop1",
                startDate, endDate, BookingStatus.ACTIVE)
        );

        // When querying for someone else
        mockMvc.perform(get("/api/bookings/guestName")
                        .param("guestName", "Sarah Connor"))
                .andExpect(status().isNotFound());
    }

}
