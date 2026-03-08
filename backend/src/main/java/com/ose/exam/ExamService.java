package com.ose.exam;

import com.ose.common.exception.NotFoundException;
import com.ose.model.AppEnums;
import com.ose.model.MockExam;
import com.ose.model.MockExamAttempt;
import com.ose.model.MockExamAttemptAnswer;
import com.ose.model.MockExamQuestion;
import com.ose.model.Question;
import com.ose.repository.MockExamAttemptRepository;
import com.ose.repository.MockExamRepository;
import com.ose.question.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExamService {

    private final MockExamRepository mockExamRepository;
    private final MockExamAttemptRepository mockExamAttemptRepository;
    private final QuestionService questionService;

    public List<ExamDtos.ExamSummary> listExams() {
        return mockExamRepository.findAll().stream().map(this::toSummary).toList();
    }

    public List<ExamDtos.AttemptView> listAttempts() {
        return mockExamAttemptRepository.findAll().stream()
                .sorted(Comparator.comparing(MockExamAttempt::getUpdatedAt).reversed())
                .map(this::toAttemptView)
                .toList();
    }

    @Transactional
    public ExamDtos.ExamSummary createExam(ExamDtos.CreateExamRequest request) {
        MockExam exam = MockExam.builder()
                .name(request.name())
                .type(request.type())
                .durationMinutes(request.durationMinutes())
                .description(request.description())
                .questions(new ArrayList<>())
                .totalScore(BigDecimal.ZERO)
                .build();
        List<Question> questions = questionService.findAllByIds(request.questionIds());
        BigDecimal total = BigDecimal.ZERO;
        int order = 1;
        for (Question question : questions) {
            exam.getQuestions().add(MockExamQuestion.builder()
                    .mockExam(exam)
                    .question(question)
                    .sortOrder(order++)
                    .score(question.getScore())
                    .build());
            total = total.add(question.getScore());
        }
        exam.setTotalScore(total);
        return toSummary(mockExamRepository.save(exam));
    }

    @Transactional
    public ExamDtos.AttemptView startAttempt(Long examId) {
        MockExam exam = mockExamRepository.findById(examId).orElseThrow(() -> new NotFoundException("模拟卷不存在"));
        MockExamAttempt attempt = MockExamAttempt.builder()
                .mockExam(exam)
                .status(AppEnums.AttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .objectiveScore(BigDecimal.ZERO)
                .subjectiveScore(BigDecimal.ZERO)
                .totalScore(BigDecimal.ZERO)
                .durationSeconds(0)
                .answers(new ArrayList<>())
                .build();
        exam.getQuestions().forEach(item -> attempt.getAnswers().add(MockExamAttemptAnswer.builder()
                .attempt(attempt)
                .question(item.getQuestion())
                .build()));
        return toAttemptView(mockExamAttemptRepository.save(attempt));
    }

    public ExamDtos.AttemptView getAttempt(Long attemptId) {
        return toAttemptView(mockExamAttemptRepository.findById(attemptId).orElseThrow(() -> new NotFoundException("模拟记录不存在")));
    }

    @Transactional
    public ExamDtos.AttemptView submitAttempt(Long attemptId, ExamDtos.SubmitAttemptRequest request) {
        MockExamAttempt attempt = mockExamAttemptRepository.findById(attemptId).orElseThrow(() -> new NotFoundException("模拟记录不存在"));
        Map<Long, MockExamAttemptAnswer> answerMap = attempt.getAnswers().stream().collect(Collectors.toMap(item -> item.getQuestion().getId(), Function.identity()));
        BigDecimal objectiveScore = BigDecimal.ZERO;
        BigDecimal subjectiveScore = BigDecimal.ZERO;
        if (request.answers() != null) {
            for (ExamDtos.AttemptAnswerInput item : request.answers()) {
                MockExamAttemptAnswer answer = answerMap.get(item.questionId());
                if (answer == null) {
                    continue;
                }
                answer.setAnswerText(item.answerText());
                answer.setFeedback(item.feedback());
                if (answer.getQuestion().getType() == AppEnums.QuestionType.MORNING_SINGLE) {
                    boolean correct = answer.getQuestion().getCorrectAnswer() != null && answer.getQuestion().getCorrectAnswer().equals(item.answerText());
                    answer.setAutoScore(correct ? answer.getQuestion().getScore() : BigDecimal.ZERO);
                    answer.setResult(correct ? AppEnums.PracticeResult.CORRECT : AppEnums.PracticeResult.WRONG);
                    objectiveScore = objectiveScore.add(answer.getAutoScore());
                } else if (item.subjectiveScore() != null) {
                    answer.setSubjectiveScore(item.subjectiveScore());
                    answer.setResult(resolveSubjectiveResult(item.subjectiveScore(), answer.getQuestion().getScore()));
                    subjectiveScore = subjectiveScore.add(item.subjectiveScore());
                }
            }
        }
        attempt.setStatus(AppEnums.AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setDurationSeconds(request.durationSeconds() == null ? 0 : request.durationSeconds());
        attempt.setSelfReviewSummary(request.selfReviewSummary());
        attempt.setObjectiveScore(objectiveScore);
        attempt.setSubjectiveScore(subjectiveScore);
        attempt.setTotalScore(objectiveScore.add(subjectiveScore));
        return toAttemptView(mockExamAttemptRepository.save(attempt));
    }

    @Transactional
    public ExamDtos.AttemptView scoreAfternoon(Long attemptId, ExamDtos.ScoreAfternoonRequest request) {
        MockExamAttempt attempt = mockExamAttemptRepository.findById(attemptId).orElseThrow(() -> new NotFoundException("模拟记录不存在"));
        Map<Long, MockExamAttemptAnswer> answerMap = attempt.getAnswers().stream().collect(Collectors.toMap(item -> item.getQuestion().getId(), Function.identity()));
        BigDecimal subjectiveScore = BigDecimal.ZERO;
        if (request.answers() != null) {
            for (ExamDtos.AttemptAnswerInput item : request.answers()) {
                MockExamAttemptAnswer answer = answerMap.get(item.questionId());
                if (answer == null || answer.getQuestion().getType() != AppEnums.QuestionType.AFTERNOON_CASE) {
                    continue;
                }
                answer.setSubjectiveScore(item.subjectiveScore());
                answer.setResult(resolveSubjectiveResult(item.subjectiveScore(), answer.getQuestion().getScore()));
                answer.setFeedback(item.feedback());
                subjectiveScore = subjectiveScore.add(item.subjectiveScore() == null ? BigDecimal.ZERO : item.subjectiveScore());
            }
        }
        attempt.setSelfReviewSummary(request.selfReviewSummary());
        attempt.setSubjectiveScore(subjectiveScore);
        attempt.setTotalScore(attempt.getObjectiveScore().add(subjectiveScore));
        return toAttemptView(mockExamAttemptRepository.save(attempt));
    }

    public List<MockExamAttempt> recentAttempts() {
        return mockExamAttemptRepository.findTop10ByOrderByUpdatedAtDesc();
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

    private String buildSystemReviewSummary(MockExamAttempt attempt) {
        long total = attempt.getAnswers().size();
        long correct = attempt.getAnswers().stream().filter(answer -> answer.getResult() == AppEnums.PracticeResult.CORRECT).count();
        long partial = attempt.getAnswers().stream().filter(answer -> answer.getResult() == AppEnums.PracticeResult.PARTIAL).count();
        long wrong = attempt.getAnswers().stream().filter(answer -> answer.getResult() == AppEnums.PracticeResult.WRONG).count();
        String weakAreas = attempt.getAnswers().stream()
                .filter(answer -> answer.getResult() == AppEnums.PracticeResult.WRONG || answer.getResult() == AppEnums.PracticeResult.PARTIAL)
                .flatMap(answer -> answer.getQuestion().getKnowledgePoints().stream())
                .map(point -> point.getName())
                .distinct()
                .limit(3)
                .reduce((left, right) -> left + "、" + right)
                .orElse("无明显薄弱知识点");
        if (attempt.getMockExam().getType() == AppEnums.ExamType.MORNING) {
            return "本次上午卷共 " + total + " 题，答对 " + correct + " 题，部分得分 " + partial + " 题，失分 " + wrong
                    + " 题；建议优先复盘：" + weakAreas + "。";
        }
        return "本次下午卷主观累计得分 " + attempt.getSubjectiveScore() + " / " + attempt.getMockExam().getTotalScore()
                + "；建议围绕 " + weakAreas + " 补充答题模板、要点覆盖与时间分配。";
    }

    private ExamDtos.ExamSummary toSummary(MockExam exam) {
        return new ExamDtos.ExamSummary(
                exam.getId(),
                exam.getName(),
                exam.getType(),
                exam.getDurationMinutes(),
                exam.getTotalScore(),
                exam.getDescription(),
                exam.getQuestions().size()
        );
    }

    private ExamDtos.AttemptView toAttemptView(MockExamAttempt attempt) {
        return new ExamDtos.AttemptView(
                attempt.getId(),
                attempt.getMockExam().getId(),
                attempt.getMockExam().getName(),
                attempt.getMockExam().getType(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getObjectiveScore(),
                attempt.getSubjectiveScore(),
                attempt.getTotalScore(),
                attempt.getDurationSeconds(),
                attempt.getSelfReviewSummary(),
                buildSystemReviewSummary(attempt),
                attempt.getAnswers().stream().map(answer -> new ExamDtos.AttemptAnswerView(
                        answer.getId(),
                        answer.getQuestion().getId(),
                        answer.getQuestion().getTitle(),
                        answer.getQuestion().getContent(),
                        answer.getQuestion().getType(),
                        answer.getQuestion().getOptions().stream().map(option -> new com.ose.question.QuestionDtos.QuestionOptionDto(option.getId(), option.getOptionKey(), option.getContent())).toList(),
                        answer.getAnswerText(),
                        answer.getAutoScore(),
                        answer.getSubjectiveScore(),
                        answer.getResult(),
                        answer.getQuestion().getReferenceAnswer(),
                        answer.getFeedback(),
                        answer.getQuestion().getScore()
                )).toList()
        );
    }
}
