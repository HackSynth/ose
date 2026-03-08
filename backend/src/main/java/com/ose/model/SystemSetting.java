package com.ose.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_settings")
public class SystemSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate examDate;

    @Column(nullable = false)
    private Integer passingScore;

    @Column(nullable = false)
    private Integer weeklyStudyHours;

    @Column(nullable = false, length = 255)
    private String learningPreference;

    @Column(nullable = false, length = 64)
    private String reviewIntervals;

    @Column(nullable = false)
    private Integer dailySessionMinutes;
}
