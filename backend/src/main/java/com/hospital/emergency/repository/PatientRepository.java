package com.hospital.emergency.repository;

import com.hospital.emergency.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Find all waiting patients ordered by priority then arrival time
    List<Patient> findByStatusOrderByPriorityLevelAscArrivalTimeAsc(Patient.PatientStatus status);

    // Find patients by status
    List<Patient> findByStatus(Patient.PatientStatus status);

    // Find patients by priority level
    List<Patient> findByPriorityLevelAndStatus(Integer priorityLevel, Patient.PatientStatus status);

    // Find the next patient to be treated (highest priority, earliest arrival)
    @Query("SELECT p FROM Patient p WHERE p.status = 'WAITING' ORDER BY p.priorityLevel ASC, p.arrivalTime ASC")
    List<Patient> findNextPatient();

    // Search patients by name (case-insensitive)
    @Query("SELECT p FROM Patient p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Patient> searchByName(@Param("name") String name);

    // Count patients by status
    long countByStatus(Patient.PatientStatus status);

    // Count patients by priority and status
    long countByPriorityLevelAndStatus(Integer priorityLevel, Patient.PatientStatus status);

    // Find all treated patients ordered by treated time
    @Query("SELECT p FROM Patient p WHERE p.status = 'TREATED' ORDER BY p.treatedTime DESC")
    List<Patient> findPatientHistory();

    // Find patients by priority level (all statuses)
    List<Patient> findByPriorityLevelOrderByArrivalTimeAsc(Integer priorityLevel);

    // Get all waiting patients filtered by priority
    @Query("SELECT p FROM Patient p WHERE p.status = 'WAITING' AND p.priorityLevel = :priority ORDER BY p.arrivalTime ASC")
    List<Patient> findWaitingPatientsByPriority(@Param("priority") Integer priority);
}
