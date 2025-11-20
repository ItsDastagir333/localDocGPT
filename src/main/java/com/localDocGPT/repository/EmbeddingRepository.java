package com.localDocGPT.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.localDocGPT.model.EmbeddingEntity;

@Repository
public interface EmbeddingRepository extends JpaRepository<EmbeddingEntity, Long> {
    // Custom method declarations - Spring Data JPA will implement these automatically
    boolean existsByFileName(String fileName);
    
}
