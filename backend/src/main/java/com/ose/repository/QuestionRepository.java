package com.ose.repository;

import com.ose.model.AppEnums;
import com.ose.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {
    List<Question> findByTypeAndActiveTrue(AppEnums.QuestionType type);

    Optional<Question> findFirstByTypeAndYearAndTitle(AppEnums.QuestionType type, Integer year, String title);
}
