package com.book.bookhost.repository;

import com.book.bookhost.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockingRepository extends JpaRepository<Block, Long> {
    List<Block> findByPropertyId(String propertyId);
}