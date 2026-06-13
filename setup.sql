-- ============================================================
-- Hospital Emergency Queue Management System
-- MySQL Database Setup Script
-- ============================================================

-- Create and select the database
CREATE DATABASE IF NOT EXISTS hospital_emergency_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hospital_emergency_db;

-- (Hibernate will auto-create the 'patients' table on first run)
-- This script only creates the database.

-- To verify the setup, run after starting the Spring Boot app:
-- SHOW TABLES;
-- DESCRIBE patients;

-- Optional: Seed some sample data (run AFTER starting the application)
-- INSERT INTO patients (name, age, symptoms, priority_level, arrival_time, status)
-- VALUES
--   ('Ravi Kumar',    58, 'Severe chest pain, sweating', 1, NOW() - INTERVAL 10 MINUTE, 'WAITING'),
--   ('Priya Singh',   32, 'High fever, difficulty breathing', 2, NOW() - INTERVAL 8 MINUTE, 'WAITING'),
--   ('Amit Sharma',   45, 'Fractured wrist', 3, NOW() - INTERVAL 5 MINUTE, 'WAITING'),
--   ('Sunita Verma',  25, 'Minor cut on hand', 4, NOW() - INTERVAL 3 MINUTE, 'WAITING'),
--   ('Deepak Gupta',  40, 'Routine check-up', 5, NOW() - INTERVAL 1 MINUTE, 'WAITING');

SELECT 'Hospital Emergency DB setup complete!' AS Status;
