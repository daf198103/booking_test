package com.book.bookhost.service;

import com.book.bookhost.dto.BlockRequest;
import com.book.bookhost.exception.NotFoundException;
import com.book.bookhost.exception.ValidationException;
import com.book.bookhost.model.Block;
import com.book.bookhost.model.Booking;
import com.book.bookhost.model.BookingStatus;
import com.book.bookhost.repository.BlockingRepository;
import com.book.bookhost.repository.BookingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.book.bookhost.util.DateUtils.overlap;
import static com.book.bookhost.util.DateUtils.validateDates;

@Service
public class BlockingService {

    private final BlockingRepository blockRepository;
    private final BookingRepository bookingRepository;

    public BlockingService(BlockingRepository blockRepository, BookingRepository bookingRepository) {
        this.blockRepository = blockRepository;
        this.bookingRepository = bookingRepository;
    }

    // Block operations
    @Transactional
    public Block createBlock(BlockRequest request) {
        validateDates(request.startDate(), request.endDate());

        // ensure block doesn't overlap non-canceled bookings
        List<Booking> bookings = bookingRepository.findByPropertyId(request.propertyId());
        bookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELED)
                .forEach(b -> {
                    if (overlap(request.startDate(), request.endDate(), b.getStartDate(), b.getEndDate())) {
                        throw new ValidationException("Block dates overlap existing booking id = " + b.getId());
                    }
                });

        Block block = new Block(request.propertyId(), request.startDate(), request.endDate(), request.reason());
        return blockRepository.save(block);
    }

    @Transactional
    public Block updateBlock(Long id, BlockRequest request) {
        Block existing = blockRepository.findById(id).orElseThrow(() -> new NotFoundException("Block not found"));
        validateDates(request.startDate(), request.endDate());

        // check bookings overlap
        bookingRepository.findByPropertyId(request.propertyId()).stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELED)
                .forEach(b -> {
                    if (overlap(request.startDate(), request.endDate(), b.getStartDate(), b.getEndDate())) {
                        throw new ValidationException("Block dates overlap existing booking id=" + b.getId());
                    }
                });

        existing.setPropertyId(request.propertyId());
        existing.setStartDate(request.startDate());
        existing.setEndDate(request.endDate());
        existing.setReason(request.reason());
        return blockRepository.save(existing);
    }

    public void deleteBlock(Long id) {
        Block existing = blockRepository.findById(id).orElseThrow(() -> new NotFoundException("Block not found"));
        blockRepository.delete(existing);
    }

    public List<Block> getBlocking(String propertyId) {
        List<Block> listOfBlocks = blockRepository.findByPropertyId(propertyId);
        if(listOfBlocks.isEmpty()){
            throw new NotFoundException("Blocks not found");
        }
        return listOfBlocks;
    }
}
