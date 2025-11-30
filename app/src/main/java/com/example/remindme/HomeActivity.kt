package com.example.remindme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindme.ui.theme.RemindMeTheme
import androidx.compose.material.icons.outlined.PushPin

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemindMeTheme {
                HomeScreen(
                    onAddReminder = {
                        // TODO: Navigate to Add/Edit Reminder screen
                        // startActivity(Intent(this, AddEditReminderActivity::class.java))
                        Toast.makeText(this, "Add Reminder (TODO)", Toast.LENGTH_SHORT).show()
                    },
                    onOpenSettings = {
                        // TODO: Navigate to Settings screen when you build it
                        Toast.makeText(this, "Settings (TODO)", Toast.LENGTH_SHORT).show()
                    },
                    onLogout = {
                        // TODO: If you want, clear FirebaseAuth.currentUser and go back to Login
                        finish()
                    }
                )
            }
        }
    }
}

/**
 * Simple model for a reminder – later you can match this to your Room entity:
 * title, date, time, notes, pinned, done, etc.
 */
data class ReminderEvent(
    val id: Int,
    val title: String,
    val dateLabel: String,  // e.g. "Today", "Tomorrow", "Mon 18 Nov"
    val timeLabel: String,  // e.g. "09:30"
    val notes: String,
    val isPinned: Boolean = false,
    val isDone: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAddReminder: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val ctx = LocalContext.current

    // Fake reminders for now – later replace with data from Room (Flow/collectAsState)
    var reminders by remember {
        mutableStateOf(
            listOf(
                ReminderEvent(
                    id = 1,
                    title = "Submit assignment",
                    dateLabel = "Today",
                    timeLabel = "14:00",
                    notes = "CIS4034 – upload to Blackboard before 4pm",
                    isPinned = true
                ),
                ReminderEvent(
                    id = 2,
                    title = "Gym session",
                    dateLabel = "Today",
                    timeLabel = "18:30",
                    notes = "Leg day – don’t skip warm-up"
                ),
                ReminderEvent(
                    id = 3,
                    title = "Call mum",
                    dateLabel = "Tomorrow",
                    timeLabel = "20:00",
                    notes = "Check travel plans for weekend"
                ),
                ReminderEvent(
                    id = 4,
                    title = "Doctor appointment",
                    dateLabel = "Fri 21 Nov",
                    timeLabel = "09:15",
                    notes = "Bring prescription list"
                )
            )
        )
    }

    // Derive pinned first, then by date
    val sortedReminders = remember(reminders) {
        reminders.sortedWith(
            compareByDescending<ReminderEvent> { it.isPinned }
                .thenBy { it.dateLabel }
                .thenBy { it.timeLabel }
        )
    }

    val nextReminder = sortedReminders.firstOrNull { !it.isDone }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RemindMe", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "Your upcoming events",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Check, // you can pick a better icon later
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminder,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Next reminder summary
                if (nextReminder != null) {
                    NextReminderCard(event = nextReminder)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Upcoming reminders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (sortedReminders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No reminders yet.\nTap + to create your first one.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Group by date label (Today / Tomorrow / etc.)
                        val grouped = sortedReminders.groupBy { it.dateLabel }
                        grouped.forEach { (date, eventsForDate) ->
                            stickyHeader {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = date,
                                        modifier = Modifier
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            items(eventsForDate, key = { it.id }) { event ->
                                ReminderCard(
                                    event = event,
                                    onToggleDone = { toggled ->
                                        reminders = reminders.map {
                                            if (it.id == toggled.id) it.copy(isDone = !it.isDone) else it
                                        }
                                    },
                                    onTogglePinned = { toggled ->
                                        reminders = reminders.map {
                                            if (it.id == toggled.id) it.copy(isPinned = !it.isPinned) else it
                                        }
                                    },
                                    onDelete = { deleted ->
                                        reminders = reminders.filterNot { it.id == deleted.id }
                                        Toast.makeText(
                                            ctx,
                                            "Reminder deleted: ${deleted.title}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onClick = {
                                        // TODO: Navigate to Event Details/Edit screen with this reminder
                                        Toast
                                            .makeText(
                                                ctx,
                                                "Open details for: ${event.title}",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextReminderCard(event: ReminderEvent) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = event.timeLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Next reminder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${event.dateLabel} • ${event.timeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    event: ReminderEvent,
    onToggleDone: (ReminderEvent) -> Unit,
    onTogglePinned: (ReminderEvent) -> Unit,
    onDelete: (ReminderEvent) -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        onClick = { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (event.isDone) FontWeight.Normal else FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { onTogglePinned(event) }) {
                    Icon(
                        imageVector = if (event.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin"
                    )
                }
            }

            Text(
                text = "${event.dateLabel} • ${event.timeLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (event.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.notes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onToggleDone(event) },
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (event.isDone) "Mark as not done" else "Mark as done")
                }

                IconButton(onClick = { onDelete(event) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}
