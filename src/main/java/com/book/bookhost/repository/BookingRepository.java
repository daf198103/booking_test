package com.book.bookhost.repository;

import com.book.bookhost.model.Booking;
import com.book.bookhost.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByPropertyId(String propertyId);

    List<Booking> findByGuestName(String guestName);

    default Optional<Booking> getActiveBookingByGuestName(String guestName) {
        return findByGuestName(guestName).stream()
                .filter(bok -> bok.getGuestName().equalsIgnoreCase(guestName))
                .filter(book -> book.getStatus() != BookingStatus.CANCELED)
                .findFirst();
    }

}