package com.ose.repository;

import com.ose.model.AiGenerationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiGenerationRecordRepository extends JpaRepository<AiGenerationRecord, Long> {
    List<AiGenerationRecord> findTop50ByOrderByCreatedAtDesc();
}
