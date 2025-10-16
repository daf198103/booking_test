package com.book.bookhost.util;

import com.book.bookhost.exception.ValidationException;
import com.book.bookhost.model.Booking;
import com.book.bookhost.model.BookingStatus;
import com.book.bookhost.repository.BlockingRepository;
import com.book.bookhost.repository.BookingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class DateUtils {


    public DateUtils(){
    }

    public static boolean overlap(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return !(aEnd.isBefore(bStart) || aStart.isAfter(bEnd) );
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static void validateDates(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new ValidationException("Start Date must be before End Date");
        }

        if (start.isBefore(today())) {
            throw new ValidationException("Start Date cannot be before Today");
        }

        if (end.isBefore(today())) {
            throw new ValidationException("End Date cannot be before Today");
        }
    }

}
