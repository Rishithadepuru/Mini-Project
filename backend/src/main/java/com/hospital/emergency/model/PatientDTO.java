package com.hospital.emergency.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {

    private Long patientId;
    private String name;
    private Integer age;
    private String symptoms;
    private Integer priorityLevel;
    private String priorityDescription;
    private String priorityColor;
    private String triageReason;
    private LocalDateTime arrivalTime;
    private Patient.PatientStatus status;
    private String contactNumber;
    private String address;
    private LocalDateTime treatedTime;
    private Integer queuePosition;

    public static PatientDTO fromPatient(Patient patient) {
        return PatientDTO.builder()
                .patientId(patient.getPatientId())
                .name(patient.getName())
                .age(patient.getAge())
                .symptoms(patient.getSymptoms())
                .priorityLevel(patient.getPriorityLevel())
                .priorityDescription(Patient.getPriorityDescription(patient.getPriorityLevel()))
                .priorityColor(patient.getPriorityColor())
                .triageReason(patient.getTriageReason())
                .arrivalTime(patient.getArrivalTime())
                .status(patient.getStatus())
                .contactNumber(patient.getContactNumber())
                .address(patient.getAddress())
                .treatedTime(patient.getTreatedTime())
                .build();
    }
}
