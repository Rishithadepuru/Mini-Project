package com.hospital.emergency.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient implements Comparable<Patient> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patientId;

    @NotBlank(message = "Patient name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 150, message = "Age cannot exceed 150")
    @Column(nullable = false)
    private Integer age;

    @NotBlank(message = "Symptoms are required")
    @Column(nullable = false, length = 500)
    private String symptoms;

    @Min(value = 1, message = "Priority level must be between 1 and 5")
    @Max(value = 5, message = "Priority level must be between 1 and 5")
    @Column(nullable = false)
    private Integer priorityLevel;

    @Column(length = 255)
    private String triageReason;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatientStatus status;

    @Column(length = 100)
    private String contactNumber;

    @Column(length = 200)
    private String address;

    private LocalDateTime treatedTime;

    // Enum for patient status
    public enum PatientStatus {
        WAITING, IN_TREATMENT, TREATED, REMOVED
    }

    // Priority level descriptions
    public static String getPriorityDescription(int level) {
        return switch (level) {
            case 1 -> "Critical";
            case 2 -> "Serious";
            case 3 -> "Moderate";
            case 4 -> "Minor";
            case 5 -> "Normal";
            default -> "Unknown";
        };
    }

    /**
     * Core Java Logic: Implementing Comparable for PriorityQueue ordering.
     * Lower priority number = higher urgency (1 = Critical, 5 = Normal).
     * If two patients share the same priority, the one who arrived first gets served first (FIFO).
     */
    @Override
    public int compareTo(Patient other) {
        // Primary sort: by priority level (ascending — 1 is most critical)
        int priorityComparison = Integer.compare(this.priorityLevel, other.priorityLevel);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        // Secondary sort: by arrival time (ascending — earlier arrival served first)
        return this.arrivalTime.compareTo(other.arrivalTime);
    }

    @PrePersist
    protected void onCreate() {
        if (this.arrivalTime == null) {
            this.arrivalTime = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = PatientStatus.WAITING;
        }
    }

    // Helper to get priority color for frontend
    public String getPriorityColor() {
        return switch (this.priorityLevel) {
            case 1 -> "danger";    // Red - Critical
            case 2 -> "warning";   // Orange - Serious
            case 3 -> "info";      // Yellow - Moderate
            case 4 -> "success";   // Green - Minor
            case 5 -> "secondary"; // Grey - Normal
            default -> "secondary";
        };
    }
}
