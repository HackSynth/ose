package com.ose.repository;

import com.ose.model.AppEnums;
import com.ose.model.MistakeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MistakeRecordRepository extends JpaRepository<MistakeRecord, Long> {
    List<MistakeRecord> findByReviewStatusOrderByNextReviewAtAsc(AppEnums.ReviewStatus reviewStatus);

    List<MistakeRecord> findByNextReviewAtLessThanEqualOrderByNextReviewAtAsc(LocalDate date);

    Optional<MistakeRecord> findFirstByQuestionIdOrderByUpdatedAtDesc(Long questionId);

    List<MistakeRecord> findTop10ByOrderByUpdatedAtDesc();
}
