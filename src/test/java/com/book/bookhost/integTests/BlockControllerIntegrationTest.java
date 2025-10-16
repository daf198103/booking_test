package com.book.bookhost.integTests;

import com.book.bookhost.dto.BlockRequest;
import com.book.bookhost.model.Block;
import com.book.bookhost.model.Booking;
import com.book.bookhost.model.BookingStatus;
import com.book.bookhost.repository.BlockingRepository;
import com.book.bookhost.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class BlockControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlockingRepository blockRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        blockRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void testCreateBlockSuccessfully() throws Exception {
        BlockRequest req = new BlockRequest("PROP1", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Maintenance");

        mockMvc.perform(post("/api/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyId").value("PROP1"))
                .andExpect(jsonPath("$.reason").value("Maintenance"));

        List<Block> blocks = blockRepository.findByPropertyId("PROP1");
        assertThat(blocks).hasSize(1);
    }

    @Test
    void testCreateBlockOverlapWithBookingShouldFail() throws Exception {
        Booking booking = new Booking("T-800", "terminator@example.com", "PROP1", LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), BookingStatus.ACTIVE);
        bookingRepository.save(booking);

        BlockRequest req = new BlockRequest("PROP1", LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), "Maintenance");

        mockMvc.perform(post("/api/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Block dates overlap existing booking id = " + booking.getId())));
    }

    @Test
    void testUpdateBlockSuccessfully() throws Exception {
        Block block = blockRepository.save(new Block("PROP1", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Changing carpet"));
        BlockRequest updateReq = new BlockRequest("PROP1", LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), "Cleaning up");

        mockMvc.perform(put("/api/blocks/" + block.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Cleaning up"));

        Block updated = blockRepository.findById(block.getId()).orElseThrow();
        assertThat(updated.getReason()).isEqualTo("Cleaning up");
    }

    @Test
    void testDeleteBlockSuccessfully() throws Exception {
        Block block = blockRepository.save(new Block("PROP1", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Maintenance"));

        mockMvc.perform(delete("/api/blocks/" + block.getId()))
                .andExpect(status().isNoContent());

        assertThat(blockRepository.findById(block.getId())).isEmpty();
    }

    @Test
    void testGetBlocksSuccessfully() throws Exception {
        blockRepository.save(new Block("PROP1", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Maintenance"));
        blockRepository.save(new Block("PROP1", LocalDate.now().plusDays(3), LocalDate.now().plusDays(4), "Cleaning"));

        mockMvc.perform(get("/api/blocks/PROP1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetBlocksNotFound() throws Exception {
        mockMvc.perform(get("/api/blocks/PROP2"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Blocks not found")));
    }
}
