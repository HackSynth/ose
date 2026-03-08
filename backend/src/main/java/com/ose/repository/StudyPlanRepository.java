package com.ose.repository;

import com.ose.model.AppEnums;
import com.ose.model.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findFirstByStatusOrderByCreatedAtDesc(AppEnums.PlanStatus status);
}
