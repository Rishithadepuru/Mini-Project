# 🏥 Hospital Emergency Queue Management System

A full-stack emergency room patient queue management application built with **Java Spring Boot** (backend) and **HTML/CSS/JavaScript/Bootstrap** (frontend), powered by a **Java PriorityQueue** as the core data structure.

---

## 📁 Project Structure

```
hospital-emergency-queue/
├── backend/                          ← Spring Boot Application
│   ├── pom.xml
│   └── src/main/java/com/hospital/emergency/
│       ├── EmergencyQueueApplication.java    ← Main entry point
│       ├── model/
│       │   ├── Patient.java                  ← Entity + Comparable<Patient>
│       │   ├── PatientDTO.java               ← Data Transfer Object
│       │   ├── ApiResponse.java              ← Unified API wrapper
│       │   └── QueueStats.java               ← Dashboard statistics
│       ├── repository/
│       │   └── PatientRepository.java        ← JPA queries
│       ├── service/
│       │   └── EmergencyQueueService.java    ← PriorityQueue logic ★
│       ├── controller/
│       │   └── PatientController.java        ← REST API endpoints
│       └── config/
│           ├── WebConfig.java                ← CORS configuration
│           └── GlobalExceptionHandler.java   ← Error handling
│
├── frontend/                         ← Static Web App
│   ├── index.html                    ← Single-page application
│   ├── css/style.css                 ← Hospital-themed styles
│   └── js/app.js                     ← API calls + UI logic
│
├── setup.sql                         ← MySQL database setup
├── README.md
└── run.sh / run.bat                  ← Quick start scripts
```

---

## ⚙️ Tech Stack

| Layer      | Technology                        |
|------------|-----------------------------------|
| Backend    | Java 17, Spring Boot 3.2          |
| Data Struct| `java.util.PriorityQueue<Patient>`|
| ORM        | Spring Data JPA / Hibernate       |
| Database   | MySQL 8.x                         |
| Frontend   | HTML5, CSS3, Bootstrap 5.3        |
| API Style  | RESTful JSON APIs                 |

---

## 🧠 Core Java Logic — PriorityQueue

```java
// In EmergencyQueueService.java
PriorityQueue<Patient> emergencyQueue = new PriorityQueue<>(
    Comparator.comparingInt(Patient::getPriorityLevel)
              .thenComparing(Patient::getArrivalTime)
);
```

The **Patient** class implements `Comparable<Patient>`:
```java
@Override
public int compareTo(Patient other) {
    int priorityComparison = Integer.compare(this.priorityLevel, other.priorityLevel);
    if (priorityComparison != 0) return priorityComparison;
    return this.arrivalTime.compareTo(other.arrivalTime); // FIFO tiebreaker
}
```

### Operations & Time Complexity

| Operation        | Method            | Complexity |
|-----------------|-------------------|------------|
| Add Patient     | `offer()`         | O(log n)   |
| Get Next        | `peek()`          | O(1)       |
| Call Next       | `poll()`          | O(log n)   |
| Remove by ID    | `removeIf()`      | O(n)       |
| Update Priority | remove + offer    | O(n)       |

---

## 🚦 Priority Levels

| Level | Label    | Color  | Examples                  |
|-------|----------|--------|---------------------------|
| P1    | Critical | 🔴 Red    | Heart Attack, Stroke, Trauma |
| P2    | Serious  | 🟠 Orange | Severe Pain, High Fever   |
| P3    | Moderate | 🟡 Yellow | Fractures, Moderate Pain  |
| P4    | Minor    | 🟢 Green  | Cuts, Sprains             |
| P5    | Normal   | ⚫ Grey   | Routine, Non-urgent       |

---

## 🚀 Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.x
- A modern web browser

### Step 1: Database Setup
```sql
-- Run in MySQL:
CREATE DATABASE IF NOT EXISTS hospital_emergency_db;
-- (Tables auto-created by Hibernate on first run)
```

Or run the provided script:
```bash
mysql -u root -p < setup.sql
```

### Step 2: Configure Database
Edit `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hospital_emergency_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root       ← change this
spring.datasource.password=root       ← change this
```

### Step 3: Build & Run Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
Backend starts at: **http://localhost:8080**

### Step 4: Open Frontend
Simply open `frontend/index.html` in your browser.

> **Tip:** Use VS Code Live Server or `python3 -m http.server 3000` from the `frontend/` folder to avoid any CORS issues with file:// protocol.

---

## 📡 REST API Reference

| Method | Endpoint                       | Description                    |
|--------|-------------------------------|--------------------------------|
| POST   | `/api/patients`               | Register a new patient         |
| GET    | `/api/patients`               | Get all waiting patients       |
| GET    | `/api/patients/next`          | Peek at the next patient       |
| POST   | `/api/patients/next/call`     | Call & dequeue next patient    |
| DELETE | `/api/patients/{id}`          | Remove patient from queue      |
| PUT    | `/api/patients/{id}/priority` | Update patient priority        |
| GET    | `/api/history`                | View all treated patients      |
| GET    | `/api/patients/search?name=`  | Search patients by name        |
| GET    | `/api/patients/filter?priority=` | Filter by priority level    |
| GET    | `/api/patients/stats`         | Dashboard queue statistics     |

### Example — Register Patient
```json
POST /api/patients
{
  "name": "Rahul Sharma",
  "age": 45,
  "symptoms": "Chest pain and shortness of breath",
  "priorityLevel": 1,
  "contactNumber": "9876543210"
}
```

### Example — Response
```json
{
  "success": true,
  "message": "Patient 'Rahul Sharma' registered successfully at position #1",
  "data": {
    "patientId": 1,
    "name": "Rahul Sharma",
    "age": 45,
    "symptoms": "Chest pain and shortness of breath",
    "priorityLevel": 1,
    "priorityDescription": "Critical",
    "priorityColor": "danger",
    "status": "WAITING",
    "queuePosition": 1,
    "arrivalTime": "2024-01-15T10:30:00"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 🖥️ Dashboard Features

- **Live Stats Bar** — Total in queue, critical count, treated count
- **Next Patient Banner** — Always shows who gets called next with pulsing animation
- **Patient Registration Form** — Visual priority selector with color coding
- **Live Queue** — Color-coded patient cards ordered by priority
- **Call Next Patient** — One-click to dequeue and mark as treated
- **Update Priority** — Change urgency level mid-queue (auto-reorders)
- **Search** — Real-time patient search by name
- **Priority Filter** — Filter queue view by priority level
- **Treatment History** — Full audit trail of treated patients
- **Auto-refresh** — Queue refreshes every 15 seconds

---

## 🎨 Color Coding

| Priority   | Card Style                     |
|------------|-------------------------------|
| P1 Critical | Red left border, red badge   |
| P2 Serious  | Orange left border, orange badge |
| P3 Moderate | Yellow left border, yellow badge |
| P4 Minor    | Green left border, green badge |
| P5 Normal   | Grey left border, grey badge  |

---

## 🔑 Key OOP Concepts Demonstrated

1. **Comparable<Patient>** — Natural ordering for PriorityQueue
2. **Comparator chaining** — thenComparing for FIFO tiebreaking
3. **Encapsulation** — Private fields with Lombok getters/setters
4. **Enum** — PatientStatus (WAITING, IN_TREATMENT, TREATED, REMOVED)
5. **Builder pattern** — PatientDTO, ApiResponse, QueueStats via Lombok @Builder
6. **Generics** — ApiResponse<T> for type-safe API responses
7. **Spring DI** — @Autowired service injection
8. **JPA Entity** — @Entity, @Table, lifecycle hooks (@PrePersist)

---

## 🛠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| "Cannot connect to server" | Ensure Spring Boot is running: `mvn spring-boot:run` |
| DB connection failed | Check MySQL is running; verify credentials in application.properties |
| Queue not loading | Check browser console for CORS errors; use a local HTTP server for frontend |
| Tables not created | Ensure `spring.jpa.hibernate.ddl-auto=update` is set |
