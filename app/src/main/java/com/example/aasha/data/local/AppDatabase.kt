package com.example.aasha.data.local

import androidx.room.*
import com.example.aasha.domain.model.Appointment
import com.example.aasha.domain.model.Patient
import com.example.aasha.domain.model.Vaccination
import com.example.aasha.domain.model.Visit

@Database(
    entities = [Patient::class, Visit::class, Vaccination::class, Appointment::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun visitDao(): VisitDao
    abstract fun vaccinationDao(): VaccinationDao
    abstract fun appointmentDao(): AppointmentDao
}
