package com.hospital.emergency.controller;

import com.hospital.emergency.model.ApiResponse;
import com.hospital.emergency.model.Patient;
import com.hospital.emergency.model.PatientDTO;
import com.hospital.emergency.model.QueueStats;
import com.hospital.emergency.service.EmergencyQueueService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller — Emergency Queue API
 *
 * Endpoints:
 *   POST   /api/patients            → Register new patient
 *   GET    /api/patients            → View current queue
 *   GET    /api/patients/next       → Peek next patient
 *   POST   /api/patients/next/call  → Call (dequeue) next patient
 *   DELETE /api/patients/{id}       → Remove patient from queue
 *   PUT    /api/patients/{id}/priority → Update patient priority
 *   GET    /api/patients/history    → View patient history
 *   GET    /api/patients/search     → Search patients by name
 *   GET    /api/patients/filter     → Filter by priority level
 *   GET    /api/patients/stats      → Queue statistics
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class PatientController {

    @Autowired
    private EmergencyQueueService queueService;

    // =========================================================================
    // POST /api/patients — Register a new patient
    // =========================================================================
    @PostMapping("/patients")
    public ResponseEntity<ApiResponse<PatientDTO>> addPatient(@Valid @RequestBody Patient patient) {
        log.info("API: POST /patients — {}", patient.getName());
        try {
            PatientDTO dto = queueService.addPatient(patient);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Patient '" + dto.getName() + "' registered successfully at position #" + dto.getQueuePosition(),
                            dto));
        } catch (Exception e) {
            log.error("Error adding patient: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to register patient: " + e.getMessage()));
        }
    }

    // =========================================================================
    // GET /api/patients — View current queue (sorted by priority)
    // =========================================================================
    @GetMapping("/patients")
    public ResponseEntity<ApiResponse<List<PatientDTO>>> getQueue() {
        log.debug("API: GET /patients");
        List<PatientDTO> queue = queueService.getQueue();
        return ResponseEntity.ok(ApiResponse.success(
                "Queue fetched successfully. Total: " + queue.size() + " patient(s).",
                queue));
    }

    // =========================================================================
    // GET /api/patients/next — Peek next patient (no removal)
    // =========================================================================
    @GetMapping("/patients/next")
    public ResponseEntity<ApiResponse<PatientDTO>> getNextPatient() {
        log.debug("API: GET /patients/next");
        Optional<PatientDTO> next = queueService.peekNextPatient();
        return next.map(dto -> ResponseEntity.ok(ApiResponse.success("Next patient to be treated.", dto)))
                   .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Queue is empty.", null)));
    }

    // =========================================================================
    // POST /api/patients/next/call — Call and dequeue the next patient
    // =========================================================================
    @PostMapping("/patients/next/call")
    public ResponseEntity<ApiResponse<PatientDTO>> callNextPatient() {
        log.info("API: POST /patients/next/call");
        Optional<PatientDTO> called = queueService.callNextPatient();
        return called.map(dto -> ResponseEntity.ok(ApiResponse.success(
                        "Patient '" + dto.getName() + "' has been called for treatment.", dto)))
                     .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Queue is empty — no patient to call.", null)));
    }

    // =========================================================================
    // DELETE /api/patients/{id} — Remove patient from queue
    // =========================================================================
    @DeleteMapping("/patients/{id}")
    public ResponseEntity<ApiResponse<Void>> removePatient(@PathVariable Long id) {
        log.info("API: DELETE /patients/{}", id);
        boolean removed = queueService.removePatient(id);
        if (removed) {
            return ResponseEntity.ok(ApiResponse.success("Patient ID " + id + " removed from queue.", null));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Patient ID " + id + " not found or not in waiting queue."));
        }
    }

    // =========================================================================
    // PUT /api/patients/{id}/priority — Update patient priority
    // =========================================================================
    @PutMapping("/patients/{id}/priority")
    public ResponseEntity<ApiResponse<PatientDTO>> updatePriority(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {

        Integer newPriority = body.get("priorityLevel");
        if (newPriority == null || newPriority < 1 || newPriority > 5) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Priority level must be between 1 (Critical) and 5 (Normal)."));
        }

        log.info("API: PUT /patients/{}/priority → {}", id, newPriority);
        Optional<PatientDTO> updated = queueService.updatePriority(id, newPriority);

        return updated.map(dto -> ResponseEntity.ok(ApiResponse.success(
                        "Priority updated to " + Patient.getPriorityDescription(newPriority) + " for patient '" + dto.getName() + "'.", dto)))
                      .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                              .body(ApiResponse.error("Patient not found or not in waiting queue.")));
    }

    // =========================================================================
    // GET /api/history — View patient history
    // =========================================================================
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PatientDTO>>> getHistory() {
        log.debug("API: GET /history");
        List<PatientDTO> history = queueService.getPatientHistory();
        return ResponseEntity.ok(ApiResponse.success(
                "History fetched. Total treated: " + history.size() + " patient(s).", history));
    }

    // =========================================================================
    // GET /api/patients/search?name=... — Search patients
    // =========================================================================
    @GetMapping("/patients/search")
    public ResponseEntity<ApiResponse<List<PatientDTO>>> searchPatients(@RequestParam String name) {
        log.debug("API: GET /patients/search?name={}", name);
        List<PatientDTO> results = queueService.searchPatients(name);
        return ResponseEntity.ok(ApiResponse.success(
                results.size() + " patient(s) found matching '" + name + "'.", results));
    }

    // =========================================================================
    // GET /api/patients/filter?priority=... — Filter queue by priority
    // =========================================================================
    @GetMapping("/patients/filter")
    public ResponseEntity<ApiResponse<List<PatientDTO>>> filterByPriority(
            @RequestParam(required = false) Integer priority) {
        log.debug("API: GET /patients/filter?priority={}", priority);
        List<PatientDTO> results = queueService.getQueueByPriority(priority);
        return ResponseEntity.ok(ApiResponse.success(
                results.size() + " patient(s) in queue" + (priority != null ? " with priority " + priority : "") + ".",
                results));
    }

    // =========================================================================
    // GET /api/patients/stats — Queue statistics for dashboard
    // =========================================================================
    @GetMapping("/patients/stats")
    public ResponseEntity<ApiResponse<QueueStats>> getStats() {
        log.debug("API: GET /patients/stats");
        QueueStats stats = queueService.getQueueStats();
        return ResponseEntity.ok(ApiResponse.success("Statistics fetched.", stats));
    }
}
