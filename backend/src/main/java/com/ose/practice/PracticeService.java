package com.ose.practice;

import com.ose.common.exception.NotFoundException;
import com.ose.mistake.MistakeService;
import com.ose.model.AppEnums;
import com.ose.model.KnowledgePoint;
import com.ose.model.MistakeRecord;
import com.ose.model.PracticeRecord;
import com.ose.model.PracticeSession;
import com.ose.model.Question;
import com.ose.question.QuestionDtos;
import com.ose.question.QuestionService;
import com.ose.repository.PracticeRecordRepository;
import com.ose.repository.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeRecordRepository practiceRecordRepository;
    private final QuestionService questionService;
    private final MistakeService mistakeService;

    @Transactional
    public PracticeDtos.PracticeSessionResponse createSession(PracticeDtos.PracticeSessionRequest request) {
        List<Question> source = selectQuestions(request);
        Collections.shuffle(source);
        int count = request.count() == null ? 5 : request.count();
        List<Question> selected = source.stream().limit(count).toList();
        PracticeSession session = PracticeSession.builder()
                .sessionType(request.sessionType())
                .questionType(request.questionType())
                .status(AppEnums.SessionStatus.IN_PROGRESS)
                .knowledgePoint(request.knowledgePointId() == null ? null : findKnowledgePoint(selected, request.knowledgePointId()))
                .questionCount(selected.size())
                .startedAt(LocalDateTime.now())
                .records(new ArrayList<>())
                .build();
        selected.forEach(question -> session.getRecords().add(PracticeRecord.builder()
                .session(session)
                .question(question)
                .favorite(false)
                .markedUnknown(false)
                .addedToReview(false)
                .durationSeconds(0)
                .build()));
        return toDto(practiceSessionRepository.save(session));
    }

    public PracticeDtos.PracticeSessionResponse getSession(Long id) {
        return toDto(practiceSessionRepository.findById(id).orElseThrow(() -> new NotFoundException("练习会话不存在")));
    }

    @Transactional
    public PracticeDtos.PracticeSessionResponse submit(Long sessionId, PracticeDtos.SubmitPracticeRequest request) {
        PracticeSession session = practiceSessionRepository.findById(sessionId).orElseThrow(() -> new NotFoundException("练习会话不存在"));
        Map<Long, PracticeRecord> recordMap = session.getRecords().stream().collect(Collectors.toMap(PracticeRecord::getId, item -> item));
        if (request.answers() != null) {
            request.answers().forEach(answer -> {
                PracticeRecord record = Optional.ofNullable(recordMap.get(answer.recordId()))
                        .orElseThrow(() -> new NotFoundException("练习记录不存在"));
                record.setUserAnswer(answer.answer());
                record.setDurationSeconds(answer.durationSeconds() == null ? 0 : answer.durationSeconds());
                record.setSubmittedAt(LocalDateTime.now());
                if (record.getQuestion().getType() == AppEnums.QuestionType.MORNING_SINGLE) {
                    boolean correct = Objects.equals(record.getQuestion().getCorrectAnswer(), answer.answer());
                    record.setAutoScore(correct ? record.getQuestion().getScore() : BigDecimal.ZERO);
                    record.setResult(correct ? AppEnums.PracticeResult.CORRECT : AppEnums.PracticeResult.WRONG);
                } else if (answer.subjectiveScore() != null) {
                    record.setSubjectiveScore(answer.subjectiveScore());
                    record.setResult(resolveSubjectiveResult(answer.subjectiveScore(), record.getQuestion().getScore()));
                }
                practiceRecordRepository.save(record);
                mistakeService.upsertFromPractice(record, answer.reasonType());
            });
        }
        session.setStatus(AppEnums.SessionStatus.SUBMITTED);
        session.setSubmittedAt(LocalDateTime.now());
        return toDto(practiceSessionRepository.save(session));
    }

    @Transactional
    public PracticeDtos.PracticeRecordView review(Long recordId, PracticeDtos.ReviewRecordRequest request) {
        PracticeRecord record = practiceRecordRepository.findById(recordId).orElseThrow(() -> new NotFoundException("练习记录不存在"));
        record.setSubjectiveScore(request.subjectiveScore());
        record.setResult(resolveSubjectiveResult(request.subjectiveScore(), record.getQuestion().getScore()));
        practiceRecordRepository.save(record);
        mistakeService.upsertFromPractice(record, request.reasonType());
        return toRecordDto(record);
    }

    @Transactional
    public PracticeDtos.PracticeRecordView updateFlags(Long recordId, PracticeDtos.UpdateFlagsRequest request) {
        PracticeRecord record = practiceRecordRepository.findById(recordId).orElseThrow(() -> new NotFoundException("练习记录不存在"));
        if (request.favorite() != null) {
            record.setFavorite(request.favorite());
        }
        if (request.markedUnknown() != null) {
            record.setMarkedUnknown(request.markedUnknown());
        }
        if (request.addedToReview() != null) {
            record.setAddedToReview(request.addedToReview());
        }
        practiceRecordRepository.save(record);
        mistakeService.upsertFromPractice(record, record.getMarkedUnknown() ? "CONCEPT" : "REVIEW");
        return toRecordDto(record);
    }

    public List<PracticeRecord> recentRecords() {
        return practiceRecordRepository.findTop10ByOrderByUpdatedAtDesc();
    }

    private List<Question> selectQuestions(PracticeDtos.PracticeSessionRequest request) {
        List<Question> candidates = questionService.activeByType(request.questionType());
        return switch (request.sessionType()) {
            case RANDOM -> candidates;
            case KNOWLEDGE -> candidates.stream()
                    .filter(question -> request.knowledgePointId() != null && question.getKnowledgePoints().stream().anyMatch(point -> point.getId().equals(request.knowledgePointId())))
                    .toList();
            case MISTAKE -> mistakeService.dueMistakes().stream()
                    .map(MistakeRecord::getQuestion)
                    .distinct()
                    .filter(question -> question.getType() == request.questionType())
                    .toList();
        };
    }

    private KnowledgePoint findKnowledgePoint(List<Question> selected, Long id) {
        return selected.stream().flatMap(question -> question.getKnowledgePoints().stream()).filter(point -> point.getId().equals(id)).findFirst().orElse(null);
    }

    private AppEnums.PracticeResult resolveSubjectiveResult(BigDecimal subjectiveScore, BigDecimal totalScore) {
        if (subjectiveScore == null) {
            return null;
        }
        BigDecimal ratio = subjectiveScore.divide(totalScore, 2, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.8")) >= 0) {
            return AppEnums.PracticeResult.CORRECT;
        }
        if (ratio.compareTo(BigDecimal.ZERO) > 0) {
            return AppEnums.PracticeResult.PARTIAL;
        }
        return AppEnums.PracticeResult.WRONG;
    }

    private PracticeDtos.PracticeSessionResponse toDto(PracticeSession session) {
        return new PracticeDtos.PracticeSessionResponse(
                session.getId(),
                session.getSessionType(),
                session.getQuestionType(),
                session.getStatus(),
                session.getStartedAt(),
                session.getSubmittedAt(),
                session.getQuestionCount(),
                session.getRecords().stream().map(this::toRecordDto).toList()
        );
    }

    private PracticeDtos.PracticeRecordView toRecordDto(PracticeRecord record) {
        Question question = record.getQuestion();
        return new PracticeDtos.PracticeRecordView(
                record.getId(),
                new PracticeDtos.PracticeQuestionView(
                        question.getId(),
                        question.getTitle(),
                        question.getContent(),
                        question.getType(),
                        question.getOptions().stream().map(option -> new QuestionDtos.QuestionOptionDto(option.getId(), option.getOptionKey(), option.getContent())).toList(),
                        question.getScore(),
                        question.getKnowledgePoints().stream().map(point -> new QuestionDtos.QuestionRelationDto(point.getId(), point.getCode(), point.getName())).toList(),
                        question.getReferenceAnswer()
                ),
                record.getUserAnswer(),
                record.getAutoScore(),
                record.getSubjectiveScore(),
                record.getResult(),
                record.getFavorite(),
                record.getMarkedUnknown(),
                record.getAddedToReview(),
                record.getDurationSeconds(),
                record.getSubmittedAt()
        );
    }
}
