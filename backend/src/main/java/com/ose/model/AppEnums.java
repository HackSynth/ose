package com.ose.model;

public final class AppEnums {
    private AppEnums() {
    }

    public enum Role {
        ADMIN
    }

    public enum QuestionType {
        MORNING_SINGLE,
        AFTERNOON_CASE
    }

    public enum PlanPhase {
        FOUNDATION,
        INTENSIVE,
        SPRINT
    }

    public enum PlanStatus {
        ACTIVE,
        ARCHIVED
    }

    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        DONE,
        DELAYED,
        MISSED
    }

    public enum TaskType {
        KNOWLEDGE,
        PRACTICE,
        REVIEW,
        EXAM,
        NOTE
    }

    public enum SessionType {
        KNOWLEDGE,
        RANDOM,
        MISTAKE
    }

    public enum SessionStatus {
        IN_PROGRESS,
        SUBMITTED
    }

    public enum PracticeResult {
        CORRECT,
        WRONG,
        PARTIAL
    }

    public enum ReviewStatus {
        NEW,
        READY,
        REVIEWED,
        MASTERED
    }

    public enum ExamType {
        MORNING,
        AFTERNOON
    }

    public enum AttemptStatus {
        IN_PROGRESS,
        SUBMITTED
    }

    public enum NoteLinkType {
        KNOWLEDGE,
        QUESTION,
        EXAM
    }
}
