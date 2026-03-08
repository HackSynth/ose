package com.ose.repository;

import com.ose.model.KnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, Long> {
    List<KnowledgePoint> findAllByOrderByLevelAscSortOrderAsc();

    Optional<KnowledgePoint> findByCode(String code);

    Optional<KnowledgePoint> findFirstByNameIgnoreCase(String name);
}
