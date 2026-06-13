package com.hospital.emergency.service;

import com.hospital.emergency.model.Patient;
import com.hospital.emergency.model.PatientDTO;
import com.hospital.emergency.model.QueueStats;
import com.hospital.emergency.repository.PatientRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Emergency Queue Service
 *
 * KEY JAVA DATA STRUCTURE: PriorityQueue<Patient>
 *
 * The Java PriorityQueue is an unbounded priority queue based on a priority heap.
 * It orders elements based on:
 * 1. Priority Level (1=Critical → 5=Normal) — lower number = higher urgency
 * 2. Arrival Time (FIFO tie-breaking when priorities are equal)
 *
 * This is achieved via the Comparator defined below, using the natural ordering
 * from Patient's Comparable<Patient> implementation.
 */
@Service
@Slf4j
public class EmergencyQueueService {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * =====================================================================
     * CORE DATA STRUCTURE: Java PriorityQueue
     * =====================================================================
     * Comparator chain:
     *  - Primary key  : priorityLevel (ascending — 1 is most critical)
     *  - Secondary key: arrivalTime   (ascending — earlier patient served first)
     *
     * This ensures O(log n) insertion and O(log n) removal of the head.
     * peek() is O(1) — constant time to check the next patient.
     * =====================================================================
     */
    private final PriorityQueue<Patient> emergencyQueue = new PriorityQueue<>(
            Comparator.comparingInt(Patient::getPriorityLevel)
                      .thenComparing(Patient::getArrivalTime)
    );

    /**
     * On application startup, reload all WAITING patients from the database
     * back into the in-memory PriorityQueue so the queue survives restarts.
     */
    @PostConstruct
    public void initializeQueue() {
        log.info("Initializing Emergency Queue from database...");
        List<Patient> waitingPatients = patientRepository
                .findByStatusOrderByPriorityLevelAscArrivalTimeAsc(Patient.PatientStatus.WAITING);
        emergencyQueue.addAll(waitingPatients);
        log.info("Queue initialized with {} waiting patients.", emergencyQueue.size());
    }

    // =========================================================================
    // ADD PATIENT
    // =========================================================================

    /**
     * Registers a new patient and adds them to the PriorityQueue.
     * Persists the record to MySQL via JPA.
     */
    @Transactional
    public PatientDTO addPatient(Patient patient) {
        log.debug("Adding patient: {}", patient.getName());

        TriageResult triage = analyzeSymptoms(patient.getSymptoms());
        patient.setPriorityLevel(triage.priorityLevel());
        patient.setTriageReason(triage.reason());
        patient.setArrivalTime(LocalDateTime.now());
        patient.setStatus(Patient.PatientStatus.WAITING);

        // Persist to database
        Patient savedPatient = patientRepository.save(patient);

        // Add to in-memory PriorityQueue — O(log n)
        emergencyQueue.offer(savedPatient);

        log.info("Patient [{}] added to queue with Priority {}", savedPatient.getName(), savedPatient.getPriorityLevel());

        PatientDTO dto = PatientDTO.fromPatient(savedPatient);
        dto.setQueuePosition(getQueuePosition(savedPatient.getPatientId()));
        return dto;
    }

    // =========================================================================
    // VIEW QUEUE (ordered by priority)
    // =========================================================================

    /**
     * Returns the current queue as an ordered list.
     * The PriorityQueue does NOT guarantee iteration order, so we create a
     * sorted copy for display purposes.
     */
    public List<PatientDTO> getQueue() {
        // Convert PriorityQueue to a sorted list for ordered display
        List<Patient> sortedQueue = new ArrayList<>(emergencyQueue);
        sortedQueue.sort(Comparator.comparingInt(Patient::getPriorityLevel)
                                   .thenComparing(Patient::getArrivalTime));

        List<PatientDTO> result = new ArrayList<>();
        int position = 1;
        for (Patient p : sortedQueue) {
            PatientDTO dto = PatientDTO.fromPatient(p);
            dto.setQueuePosition(position++);
            result.add(dto);
        }
        return result;
    }

    // =========================================================================
    // GET NEXT PATIENT (peek) — O(1)
    // =========================================================================

    /**
     * Returns but does NOT remove the highest-priority patient from the queue.
     * PriorityQueue.peek() is O(1).
     */
    public Optional<PatientDTO> peekNextPatient() {
        Patient next = emergencyQueue.peek();
        if (next == null) return Optional.empty();
        PatientDTO dto = PatientDTO.fromPatient(next);
        dto.setQueuePosition(1);
        return Optional.of(dto);
    }

    // =========================================================================
    // CALL NEXT PATIENT (poll + mark as In Treatment) — O(log n)
    // =========================================================================

    /**
     * Removes and returns the highest-priority patient from the queue.
     * PriorityQueue.poll() is O(log n) — it removes the head and re-heapifies.
     * Updates the patient status to TREATED in the database.
     */
    @Transactional
    public Optional<PatientDTO> callNextPatient() {
        // poll() removes the head element — the highest priority patient
        Patient nextPatient = emergencyQueue.poll();

        if (nextPatient == null) {
            log.warn("Queue is empty — no patient to call.");
            return Optional.empty();
        }

        log.info("Calling next patient: [{}] Priority {}", nextPatient.getName(), nextPatient.getPriorityLevel());

        // Update status to TREATED in DB
        nextPatient.setStatus(Patient.PatientStatus.TREATED);
        nextPatient.setTreatedTime(LocalDateTime.now());
        Patient updated = patientRepository.save(nextPatient);

        return Optional.of(PatientDTO.fromPatient(updated));
    }

    // =========================================================================
    // REMOVE PATIENT (by ID) — O(n)
    // =========================================================================

    /**
     * Removes a specific patient from the queue by ID.
     * PriorityQueue.remove(Object) is O(n) since it must scan for the element.
     */
    @Transactional
    public boolean removePatient(Long patientId) {
        Optional<Patient> optionalPatient = patientRepository.findById(patientId);
        if (optionalPatient.isEmpty()) {
            log.warn("Patient ID {} not found.", patientId);
            return false;
        }

        Patient patient = optionalPatient.get();

        if (patient.getStatus() != Patient.PatientStatus.WAITING) {
            log.warn("Patient {} is not in WAITING status, cannot remove from queue.", patientId);
            return false;
        }

        // Remove from in-memory PriorityQueue — O(n)
        emergencyQueue.removeIf(p -> p.getPatientId().equals(patientId));

        // Update status in DB
        patient.setStatus(Patient.PatientStatus.REMOVED);
        patientRepository.save(patient);

        log.info("Patient ID {} removed from queue.", patientId);
        return true;
    }

    // =========================================================================
    // UPDATE PRIORITY — O(n)
    // =========================================================================

    /**
     * Updates the priority level of a waiting patient.
     * Since PriorityQueue doesn't support priority updates in-place, we:
     *   1. Remove the patient (O(n))
     *   2. Update the priority
     *   3. Re-insert (O(log n))
     * This is the standard approach for Java PriorityQueue priority updates.
     */
    @Transactional
    public Optional<PatientDTO> updatePriority(Long patientId, Integer newPriority) {
        Optional<Patient> optionalPatient = patientRepository.findById(patientId);
        if (optionalPatient.isEmpty()) return Optional.empty();

        Patient patient = optionalPatient.get();
        if (patient.getStatus() != Patient.PatientStatus.WAITING) return Optional.empty();

        // Step 1: Remove from PriorityQueue
        emergencyQueue.removeIf(p -> p.getPatientId().equals(patientId));

        // Step 2: Update priority
        int oldPriority = patient.getPriorityLevel();
        patient.setPriorityLevel(newPriority);
        Patient updated = patientRepository.save(patient);

        // Step 3: Re-insert into PriorityQueue with new priority
        emergencyQueue.offer(updated);

        log.info("Updated patient [{}] priority: {} → {}", patient.getName(), oldPriority, newPriority);

        PatientDTO dto = PatientDTO.fromPatient(updated);
        dto.setQueuePosition(getQueuePosition(patientId));
        return Optional.of(dto);
    }

    // =========================================================================
    // PATIENT HISTORY
    // =========================================================================

    public List<PatientDTO> getPatientHistory() {
        return patientRepository.findPatientHistory()
                .stream()
                .map(PatientDTO::fromPatient)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // SEARCH PATIENT
    // =========================================================================

    public List<PatientDTO> searchPatients(String name) {
        return patientRepository.searchByName(name)
                .stream()
                .map(p -> {
                    PatientDTO dto = PatientDTO.fromPatient(p);
                    if (p.getStatus() == Patient.PatientStatus.WAITING) {
                        dto.setQueuePosition(getQueuePosition(p.getPatientId()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // FILTER BY PRIORITY
    // =========================================================================

    public List<PatientDTO> getQueueByPriority(Integer priority) {
        List<Patient> patients = priority == null
                ? patientRepository.findByStatusOrderByPriorityLevelAscArrivalTimeAsc(Patient.PatientStatus.WAITING)
                : patientRepository.findWaitingPatientsByPriority(priority);

        int[] position = {1};
        return patients.stream().map(p -> {
            PatientDTO dto = PatientDTO.fromPatient(p);
            dto.setQueuePosition(position[0]++);
            return dto;
        }).collect(Collectors.toList());
    }

    // =========================================================================
    // QUEUE STATISTICS
    // =========================================================================

    public QueueStats getQueueStats() {
        Patient nextRaw = emergencyQueue.peek();
        PatientDTO nextDTO = nextRaw != null ? PatientDTO.fromPatient(nextRaw) : null;

        return QueueStats.builder()
                .totalInQueue((int) emergencyQueue.size())
                .criticalCount((int) patientRepository.countByPriorityLevelAndStatus(1, Patient.PatientStatus.WAITING))
                .seriousCount((int) patientRepository.countByPriorityLevelAndStatus(2, Patient.PatientStatus.WAITING))
                .moderateCount((int) patientRepository.countByPriorityLevelAndStatus(3, Patient.PatientStatus.WAITING))
                .minorCount((int) patientRepository.countByPriorityLevelAndStatus(4, Patient.PatientStatus.WAITING))
                .normalCount((int) patientRepository.countByPriorityLevelAndStatus(5, Patient.PatientStatus.WAITING))
                .totalTreated((int) patientRepository.countByStatus(Patient.PatientStatus.TREATED))
                .totalPatients((int) patientRepository.count())
                .nextPatient(nextDTO)
                .build();
    }

    // =========================================================================
    // HELPER: Get Queue Position for a Patient
    // =========================================================================

    private int getQueuePosition(Long patientId) {
        List<Patient> sortedQueue = new ArrayList<>(emergencyQueue);
        sortedQueue.sort(Comparator.comparingInt(Patient::getPriorityLevel)
                                   .thenComparing(Patient::getArrivalTime));
        for (int i = 0; i < sortedQueue.size(); i++) {
            if (sortedQueue.get(i).getPatientId().equals(patientId)) {
                return i + 1;
            }
        }
        return -1;
    }

    private TriageResult analyzeSymptoms(String symptoms) {
        String text = symptoms == null ? "" : symptoms.toLowerCase(Locale.ROOT);

        Map<Integer, List<String>> rules = new LinkedHashMap<>();
        rules.put(1, Arrays.asList("heart attack", "chest pain", "stroke", "unconscious", "severe bleeding", "cardiac arrest"));
        rules.put(2, Arrays.asList("high fever", "breathing difficulty", "major fracture", "severe burns"));
        rules.put(3, Arrays.asList("moderate fever", "vomiting", "dehydration", "migraine"));
        rules.put(4, Arrays.asList("minor fracture", "sprain", "mild injury", "cough"));
        rules.put(5, Arrays.asList("routine checkup", "general consultation", "follow-up visit", "follow up visit"));

        for (Map.Entry<Integer, List<String>> entry : rules.entrySet()) {
            List<String> matched = entry.getValue().stream()
                    .filter(text::contains)
                    .toList();
            if (!matched.isEmpty()) {
                String reason = "Matched keywords: " + String.join(", ", matched);
                return new TriageResult(entry.getKey(), reason);
            }
        }

        return new TriageResult(5, "No specific emergency keywords matched; assigned P5 by default. Nurse may override if needed.");
    }

    private record TriageResult(int priorityLevel, String reason) {}

    // =========================================================================
    // GET QUEUE SIZE
    // =========================================================================

    public int getQueueSize() {
        return emergencyQueue.size();
    }
}
