package com.ose.repository;

import com.ose.model.StudyPlan;
import com.ose.model.StudyTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudyTaskRepository extends JpaRepository<StudyTask, Long> {
    List<StudyTask> findByPlanOrderByScheduledDateAscIdAsc(StudyPlan plan);

    List<StudyTask> findByScheduledDateBetween(LocalDate start, LocalDate end);

    List<StudyTask> findTop5ByOrderByUpdatedAtDesc();
}
