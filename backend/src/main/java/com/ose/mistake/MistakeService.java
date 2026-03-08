package com.ose.mistake;

import com.ose.common.exception.NotFoundException;
import com.ose.model.AppEnums;
import com.ose.model.MistakeRecord;
import com.ose.model.PracticeRecord;
import com.ose.repository.MistakeRecordRepository;
import com.ose.settings.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MistakeService {

    private final MistakeRecordRepository mistakeRecordRepository;
    private final SettingService settingService;

    public List<MistakeDtos.MistakeView> list(AppEnums.ReviewStatus status, Long knowledgePointId) {
        return mistakeRecordRepository.findAll().stream()
                .filter(item -> status == null || item.getReviewStatus() == status)
                .filter(item -> knowledgePointId == null || (item.getKnowledgePoint() != null && knowledgePointId.equals(item.getKnowledgePoint().getId())))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public MistakeDtos.MistakeView update(Long id, MistakeDtos.UpdateMistakeRequest request) {
        MistakeRecord record = mistakeRecordRepository.findById(id).orElseThrow(() -> new NotFoundException("错题记录不存在"));
        record.setReasonType(request.reasonType());
        if (request.note() != null) {
            record.setNote(request.note());
        }
        if (request.reviewStatus() != null) {
            record.setReviewStatus(request.reviewStatus());
            if (request.reviewStatus() == AppEnums.ReviewStatus.REVIEWED || request.reviewStatus() == AppEnums.ReviewStatus.MASTERED) {
                record.setReviewCount(record.getReviewCount() + 1);
            }
        }
        if (request.nextReviewAt() != null) {
            record.setNextReviewAt(request.nextReviewAt());
        } else if (request.reviewStatus() != null) {
            record.setNextReviewAt(nextReviewDate(record.getReviewCount()));
        }
        return toDto(mistakeRecordRepository.save(record));
    }

    @Transactional
    public void upsertFromPractice(PracticeRecord practiceRecord, String reasonType) {
        if (practiceRecord.getResult() == null) {
            return;
        }
        if (practiceRecord.getResult() == AppEnums.PracticeResult.CORRECT && !practiceRecord.getAddedToReview() && !practiceRecord.getMarkedUnknown()) {
            return;
        }
        MistakeRecord record = mistakeRecordRepository.findFirstByQuestionIdOrderByUpdatedAtDesc(practiceRecord.getQuestion().getId())
                .orElseGet(MistakeRecord::new);
        record.setQuestion(practiceRecord.getQuestion());
        record.setPracticeRecord(practiceRecord);
        record.setKnowledgePoint(practiceRecord.getQuestion().getKnowledgePoints().stream().findFirst().orElse(null));
        record.setReasonType(reasonType == null || reasonType.isBlank() ? "CONCEPT" : reasonType);
        record.setReviewStatus(AppEnums.ReviewStatus.NEW);
        record.setNextReviewAt(nextReviewDate(record.getReviewCount() == null ? 0 : record.getReviewCount()));
        record.setReviewCount(record.getReviewCount() == null ? 0 : record.getReviewCount());
        if (practiceRecord.getMarkedUnknown()) {
            record.setNote("用户标记为不会，建议优先复习");
        }
        mistakeRecordRepository.save(record);
    }

    public List<MistakeRecord> dueMistakes() {
        return mistakeRecordRepository.findByNextReviewAtLessThanEqualOrderByNextReviewAtAsc(LocalDate.now());
    }

    private LocalDate nextReviewDate(int reviewCount) {
        List<Integer> intervals = settingService.parseReviewIntervals(settingService.getOrCreateDefault());
        int index = Math.min(reviewCount, intervals.size() - 1);
        return LocalDate.now().plusDays(intervals.get(index));
    }

    private MistakeDtos.MistakeView toDto(MistakeRecord item) {
        return new MistakeDtos.MistakeView(
                item.getId(),
                item.getQuestion().getId(),
                item.getQuestion().getTitle(),
                item.getQuestion().getType().name(),
                item.getKnowledgePoint() == null ? null : item.getKnowledgePoint().getId(),
                item.getKnowledgePoint() == null ? null : item.getKnowledgePoint().getName(),
                item.getReasonType(),
                item.getReviewStatus(),
                item.getNextReviewAt(),
                item.getReviewCount(),
                item.getNote(),
                item.getPracticeRecord().getResult() == null ? null : item.getPracticeRecord().getResult().name()
        );
    }
}
