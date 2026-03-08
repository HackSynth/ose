package com.ose.knowledge;

import com.ose.common.exception.NotFoundException;
import com.ose.model.KnowledgePoint;
import com.ose.repository.KnowledgePointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgePointRepository knowledgePointRepository;

    public List<KnowledgeDtos.KnowledgeTreeItem> tree() {
        List<KnowledgePoint> points = knowledgePointRepository.findAllByOrderByLevelAscSortOrderAsc();
        Map<Long, KnowledgeDtos.KnowledgeTreeItem> dtoMap = new LinkedHashMap<>();
        Map<Long, List<KnowledgeDtos.KnowledgeTreeItem>> childMap = new LinkedHashMap<>();
        for (KnowledgePoint point : points) {
            dtoMap.put(point.getId(), new KnowledgeDtos.KnowledgeTreeItem(
                    point.getId(),
                    point.getCode(),
                    point.getName(),
                    point.getLevel(),
                    point.getMasteryLevel(),
                    point.getWeight(),
                    point.getNote(),
                    point.getParent() == null ? null : point.getParent().getId(),
                    new ArrayList<>()
            ));
            childMap.put(point.getId(), new ArrayList<>());
        }
        List<KnowledgeDtos.KnowledgeTreeItem> roots = new ArrayList<>();
        for (KnowledgePoint point : points) {
            KnowledgeDtos.KnowledgeTreeItem current = dtoMap.get(point.getId());
            if (point.getParent() != null) {
                childMap.get(point.getParent().getId()).add(current);
            } else {
                roots.add(current);
            }
        }
        return roots.stream().map(item -> attachChildren(item, childMap)).toList();
    }

    @Transactional
    public KnowledgeDtos.KnowledgeTreeItem create(KnowledgeDtos.KnowledgeRequest request) {
        KnowledgePoint point = new KnowledgePoint();
        apply(point, request);
        return toDto(knowledgePointRepository.save(point));
    }

    @Transactional
    public KnowledgeDtos.KnowledgeTreeItem update(Long id, KnowledgeDtos.KnowledgeRequest request) {
        KnowledgePoint point = knowledgePointRepository.findById(id).orElseThrow(() -> new NotFoundException("知识点不存在"));
        apply(point, request);
        return toDto(knowledgePointRepository.save(point));
    }

    @Transactional
    public void delete(Long id) {
        knowledgePointRepository.deleteById(id);
    }

    public List<KnowledgePoint> allEntities() {
        return knowledgePointRepository.findAllByOrderByLevelAscSortOrderAsc();
    }

    private void apply(KnowledgePoint point, KnowledgeDtos.KnowledgeRequest request) {
        point.setCode(request.code());
        point.setName(request.name());
        point.setLevel(request.level());
        point.setMasteryLevel(request.masteryLevel());
        point.setWeight(request.weight());
        point.setNote(request.note());
        point.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        if (request.parentId() != null) {
            point.setParent(knowledgePointRepository.findById(request.parentId())
                    .orElseThrow(() -> new NotFoundException("父知识点不存在")));
        } else {
            point.setParent(null);
        }
    }

    private KnowledgeDtos.KnowledgeTreeItem toDto(KnowledgePoint point) {
        return new KnowledgeDtos.KnowledgeTreeItem(
                point.getId(),
                point.getCode(),
                point.getName(),
                point.getLevel(),
                point.getMasteryLevel(),
                point.getWeight(),
                point.getNote(),
                point.getParent() == null ? null : point.getParent().getId(),
                List.of()
        );
    }

    private KnowledgeDtos.KnowledgeTreeItem attachChildren(KnowledgeDtos.KnowledgeTreeItem item,
                                                            Map<Long, List<KnowledgeDtos.KnowledgeTreeItem>> childMap) {
        List<KnowledgeDtos.KnowledgeTreeItem> children = childMap.getOrDefault(item.id(), List.of())
                .stream()
                .map(child -> attachChildren(child, childMap))
                .toList();
        return new KnowledgeDtos.KnowledgeTreeItem(
                item.id(), item.code(), item.name(), item.level(), item.masteryLevel(), item.weight(), item.note(), item.parentId(), children
        );
    }
}
