package com.book.bookhost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BlockRequest(
        @NotBlank String propertyId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason
) {}