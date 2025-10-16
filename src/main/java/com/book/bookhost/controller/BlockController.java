package com.book.bookhost.controller;

import com.book.bookhost.dto.BlockRequest;
import com.book.bookhost.model.Block;
import com.book.bookhost.service.BlockingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockingService blkService;

    public BlockController(BlockingService blkService) {
       this.blkService = blkService;
    }

    @PostMapping
    public ResponseEntity<Block> create(@Valid @RequestBody BlockRequest req) {
        return ResponseEntity.ok(blkService.createBlock(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Block> update(@PathVariable Long id, @Valid @RequestBody BlockRequest req) {
        return ResponseEntity.ok(blkService.updateBlock(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        blkService.deleteBlock(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<List<Block>> get(@PathVariable String propertyId) {
        return ResponseEntity.ok(blkService.getBlocking(propertyId));
    }
}
