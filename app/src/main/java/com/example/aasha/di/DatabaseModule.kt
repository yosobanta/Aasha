package com.example.aasha.di

import android.content.Context
import androidx.room.Room
import com.example.aasha.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aasha_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePatientDao(db: AppDatabase): PatientDao = db.patientDao()

    @Provides
    fun provideVisitDao(db: AppDatabase): VisitDao = db.visitDao()

    @Provides
    fun provideVaccinationDao(db: AppDatabase): VaccinationDao = db.vaccinationDao()

    @Provides
    fun provideAppointmentDao(db: AppDatabase): AppointmentDao = db.appointmentDao()
}
