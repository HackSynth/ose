package com.ose.repository;

import com.ose.model.MockExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockExamAttemptRepository extends JpaRepository<MockExamAttempt, Long> {
    List<MockExamAttempt> findTop10ByOrderByUpdatedAtDesc();
}
