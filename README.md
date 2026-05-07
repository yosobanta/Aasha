# Aasha: Empowering Community Health Workers (ASHA)

**Aasha** is a specialized, professional-grade Android application designed to digitalize and streamline the workflows of Accredited Social Health Activists (ASHA) in India. Built with a focus on reliability in low-connectivity environments, Aasha provides a robust platform for patient management, vaccination tracking, and community health monitoring.

---

## 🌟 Key Features

### 📶 Offline-First Excellence
Designed for the field, Aasha ensures that health workers never lose data. All records are saved locally first and intelligently synchronized with the cloud once a stable connection is established.

### 🩺 Comprehensive Patient Lifecycle
- **Smart Registration**: Quick patient onboarding with built-in **fuzzy duplicate detection** to prevent double entries.
- **Visit Records**: Track house visits, symptoms, and treatment advice.
- **Vaccination Management**: Log immunizations and schedule booster doses with ease.
- **Appointment Scheduling**: Integrated calendar for managing follow-up checkups.

### 🛡️ Dual-Layer Security
- **Cloud Authentication**: Secure login via Firebase Authentication (Email/Password).
- **Quick Access MPIN**: A 4-digit, encrypted MPIN allows workers to unlock the app quickly while in the field, even without internet access.

### 🔔 Proactive Health Monitoring
- **Automated Reminders**: Background workers (WorkManager) trigger notifications for upcoming vaccinations and appointments.
- **Dynamic Dashboard**: Real-time counters for pending tasks, upcoming visits, and community health stats.

### 🌍 Built for Diversity
- **Multilingual Support**: Fully localized in **Hindi**, **Bengali**, and **Marathi**, ensuring ease of use across different regions.
- **Localized UI**: Cultural nuances and terminology tailored for the ASHA workflow.

---

## 🏗️ Technical Architecture

Aasha follows **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** pattern, ensuring a modular, testable, and maintainable codebase.

### 🛠️ Tech Stack
- **UI**: Jetpack Compose (Modern Declarative UI)
- **Dependency Injection**: Hilt (Dagger)
- **Local Database**: Room (SQLite) with Flow for reactive updates.
- **Remote Backend**: Firebase (Firestore & Authentication)
- **Background Tasks**: WorkManager (Reliable Sync & Notifications)
- **Asynchronous Programming**: Kotlin Coroutines & Shared/StateFlow
- **Navigation**: Jetpack Compose Navigation

### 📁 Project Structure
- `data/`: Room entities, DAOs, Repositories (the "Single Source of Truth"), and Session Management.
- `domain/`: Pure Kotlin models and business logic entities.
- `ui/`: Compose screens, navigation logic, and custom components.
- `viewmodel/`: State management and UI logic.
- `sync/` & `worker/`: Background processing for offline-sync and notifications.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Kotlin 1.9.0+
- A Firebase Project (Firestore and Email/Password Auth enabled)

### Setup
1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/aasha.git
   ```
2. **Firebase Configuration**:
   - Place your `google-services.json` in the `app/` directory.
3. **Build & Run**:
   - Open the project in Android Studio.
   - Sync Gradle and run the app on an emulator or physical device.

---

## 📈 Roadmap
- [x] **Firebase Integration**: Migration to production-ready authentication.
- [x] **Secure MPIN**: Implementation of EncryptedSharedPreferences.
- [x] **Multilingual Support**: Hindi, Bengali, and Marathi localization.
- [ ] **Reporting Engine**: Automated generation of weekly/monthly health reports for PHCs.
- [ ] **Government API Integration**: Syncing data with Ayushman Bharat/ABHA IDs.
- [ ] **Data Export**: Export patient data to PDF/CSV for offline reporting.

---

## 🤝 Contributing
Contributions are welcome! Please read our contributing guidelines before submitting a Pull Request.

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Aasha — Bringing hope and health to every doorstep.*
