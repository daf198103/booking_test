package com.book.bookhost.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;


public record BookingRequest(
        @NotBlank String guestName,
        @Email String guestEmail,
        @NotBlank String propertyId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {}
