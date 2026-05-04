package com.example.aasha.ui.screens.appointment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aasha.domain.model.Appointment
import com.example.aasha.ui.components.AashaCard
import com.example.aasha.viewmodel.AppointmentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: AppointmentViewModel = hiltViewModel()
) {
    val appointments by viewModel.appointments.collectAsState(initial = emptyList())
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointment Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Month Selector
            MonthSelector(
                currentMonth = currentMonth,
                onMonthChange = { currentMonth = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Grid
            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                appointments = appointments,
                onDateSelected = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Appointments for Selected Date
            Text(
                text = "Appointments for ${SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(selectedDate.time)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val selectedDateAppointments = appointments.filter {
                val apptCal = Calendar.getInstance().apply { timeInMillis = it.dateTime }
                apptCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                        apptCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
            }

            if (selectedDateAppointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No appointments for this day", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDateAppointments) { appointment ->
                        CalendarAppointmentItem(appointment)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    currentMonth: Calendar,
    onMonthChange: (Calendar) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            val newMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            onMonthChange(newMonth)
        }) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
        }

        Text(
            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = {
            val newMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            onMonthChange(newMonth)
        }) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Calendar,
    appointments: List<Appointment>,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    
    val days = mutableListOf<Calendar?>()
    // Add empty spaces for previous month's days
    for (i in 1 until firstDayOfWeek) {
        days.add(null)
    }
    // Add current month's days
    for (i in 1..daysInMonth) {
        days.add((currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, i) })
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(280.dp) // Fixed height for calendar grid
        ) {
            items(days) { date ->
                if (date != null) {
                    val isSelected = date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                            date.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                    
                    val hasAppointment = appointments.any {
                        val apptCal = Calendar.getInstance().apply { timeInMillis = it.dateTime }
                        apptCal.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                                apptCal.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date.get(Calendar.DAY_OF_MONTH).toString(),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (hasAppointment) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun CalendarAppointmentItem(appointment: Appointment) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    AashaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(50.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeFormat.format(Date(appointment.dateTime)).split(" ")[0],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = timeFormat.format(Date(appointment.dateTime)).split(" ")[1],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = appointment.patientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Follow-up Visit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
