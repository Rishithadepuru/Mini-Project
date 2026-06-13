package com.hospital.emergency.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStats {

    private int totalInQueue;
    private int criticalCount;    // Priority 1
    private int seriousCount;     // Priority 2
    private int moderateCount;    // Priority 3
    private int minorCount;       // Priority 4
    private int normalCount;      // Priority 5
    private int totalTreated;
    private int totalPatients;
    private PatientDTO nextPatient;
}
