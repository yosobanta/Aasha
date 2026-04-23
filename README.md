# Aasha: Digital Assistant for ASHA Workers

Aasha is a specialized Android application designed to empower Accredited Social Health Activists (ASHA) in rural India. It provides a robust, offline-first platform for patient management, vaccination tracking, and community health monitoring.

---

## 🏗️ Architecture & Technical Stack

The project follows **Clean Architecture** principles combined with the **MVVM (Model-View-ViewModel)** pattern to ensure scalability, testability, and maintainability.

### Key Technologies:
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Declarative UI)
- **Dependency Injection**: Hilt (Dagger)
- **Local Database**: Room (with SQLite)
- **Asynchronous Flow**: Kotlin Coroutines & Flow
- **Remote Backend**: Firebase (Authentication & Firestore)
- **Background Processing**: WorkManager (for Offline Sync)
- **Secure Storage**: EncryptedSharedPreferences (for MPIN)
- **Navigation**: Jetpack Compose Navigation

---

## 📁 Folder Structure & File Manifest

### 1. `app/src/main/java/com/example/aasha/`

#### 📂 `data/` (Data Layer)
- **`local/`**: Room database configuration and DAOs.
    - `AppDatabase.kt`: The main database holder. Defines entities and versioning.
    - `PatientDao.kt`: Interface for Patient CRUD operations. Includes `searchPatients` and `findDuplicate`.
    - `VisitDao.kt`: Handles health visit records.
    - `VaccinationDao.kt`: Manages vaccination schedules and records.
    - `AppointmentDao.kt`: Manages follow-up appointments.
    - `SessionManager.kt`: Manages user sessions, worker IDs, and secure MPIN using DataStore and EncryptedSharedPreferences.
    - `Converters.kt`: Room type converters for complex data types (e.g., Date to Long).
- **`repository/`**:
    - `MainRepository.kt`: The single source of truth. It mediates between the local Room database and remote Firestore. It handles the logic for "Last-Write-Wins" conflict resolution during sync.
- **`sync/`**:
    - `SyncWorker.kt`: A `CoroutineWorker` that handles background synchronization. It triggers whenever the device gains internet connectivity.

#### 📂 `domain/` (Domain Layer)
- **`model/`**: Pure Kotlin data classes representing app entities.
    - `Patient.kt`: The core entity (Name, Age, Village, SyncStatus, etc.).
    - `Visit.kt`: Records of house visits.
    - `Vaccination.kt`: Records of administered vaccines.
    - `Appointment.kt`: Future health checkup schedules.
    - `SyncStatus.kt`: Enum representing `PENDING`, `SYNCED`, or `ERROR`.

#### 📂 `di/` (Dependency Injection)
- `DatabaseModule.kt`: Provides Singleton instances of Room Database and DAOs.
- `FirebaseModule.kt`: Provides instances of FirebaseAuth and FirebaseFirestore.

#### 📂 `viewmodel/` (Logic Layer)
- `LoginViewModel.kt`: Manages Firebase Auth, OTP verification, and Password-based login.
- `PatientViewModel.kt`: Handles patient listing, searching, and adding new records.
- `DashboardViewModel.kt`: Aggregates data for the main dashboard (appointments due, vaccination counts).
- `SplashViewModel.kt`: Determines the initial routing logic (Login vs. Dashboard).

#### 📂 `ui/` (Presentation Layer)
- **`screens/`**: Individual screen Composables (Dashboard, Login, Add Patient, etc.).
- **`navigation/`**:
    - `Screen.kt`: Defines routes and navigation arguments.
    - `MainScreen.kt`: The root Composable containing the `NavHost`.
- **`theme/`**: Design system (Color, Type, Shape, Theme).
- **`components/`**: Reusable UI widgets like custom buttons and cards.

---

## ⚙️ Core Workflows

### 1. Authentication System
Aasha uses a dual-layer authentication:
1. **Cloud Auth**: Firebase Email/Password (linked to Aasha ID).
2. **Local Security**: A 4-digit MPIN stored in `EncryptedSharedPreferences` for quick, offline-capable access after the first login.

### 2. Offline-First Synchronization
1. Data is first saved to the local **Room** database with a `SyncStatus.PENDING`.
2. A `OneTimeWorkRequest` is queued via **WorkManager**.
3. `SyncWorker` executes when internet is available, pushing updates to **Firestore** and pulling remote changes.
4. Uses a "Last-Write-Wins" strategy based on `lastUpdated` timestamps.

### 3. Duplicate Detection
The `PatientDao` includes a fuzzy matching query (`findDuplicate`) that checks for similar names, villages, and age ranges to prevent accidental double-registration of patients in the field.

---

## 🛠️ Key Functions Glossary

- `MainRepository.syncLocalWithRemote()`: Orchestrates the push/pull logic between Room and Firestore.
- `LoginViewModel.loginWithAashaId()`: Handles secure Firebase authentication and session creation.
- `PatientDao.searchPatients(query)`: Performs a SQL `LIKE` search for real-time patient filtering.
- `SessionManager.saveMpin(pin)`: Encrypts and persists the user's quick-access PIN.
- `SyncWorker.doWork()`: The entry point for background sync tasks.

---

## 🚀 Future Roadmap
- [ ] Integration with Government Health APIs (Ayushman Bharat).
- [ ] Voice-to-text for easy clinical note-taking.
- [ ] Multilingual support (Hindi, Bengali, Marathi, etc.).
- [ ] Automated SMS reminders for patients.
