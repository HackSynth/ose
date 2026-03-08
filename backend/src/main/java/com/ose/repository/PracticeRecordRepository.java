package com.ose.repository;

import com.ose.model.AppEnums;
import com.ose.model.PracticeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeRecordRepository extends JpaRepository<PracticeRecord, Long> {
    List<PracticeRecord> findTop10ByOrderByUpdatedAtDesc();

    List<PracticeRecord> findByResult(AppEnums.PracticeResult result);
}
