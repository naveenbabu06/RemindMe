package com.example.remindme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.remindme.ui.theme.RemindMeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddEditReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val reminderId = intent.getStringExtra("reminder_id")

        setContent {
            RemindMeTheme {
                val activity = this

                val auth = remember { FirebaseAuth.getInstance() }
                val db = remember { FirebaseFirestore.getInstance() }

                var title by remember { mutableStateOf("") }
                var dateLabel by remember { mutableStateOf("") }
                var timeLabel by remember { mutableStateOf("") }
                var notes by remember { mutableStateOf("") }
                var isPinned by remember { mutableStateOf(false) }
                var isDone by remember { mutableStateOf(false) }

                // Load existing reminder if editing
                LaunchedEffect(reminderId) {
                    val uid = auth.currentUser?.uid ?: return@LaunchedEffect
                    if (!reminderId.isNullOrBlank()) {
                        db.collection("users").document(uid)
                            .collection("reminders").document(reminderId)
                            .get()
                            .addOnSuccessListener { doc ->
                                if (doc != null && doc.exists()) {
                                    title = doc.getString("title") ?: ""
                                    dateLabel = doc.getString("dateLabel") ?: ""
                                    timeLabel = doc.getString("timeLabel") ?: ""
                                    notes = doc.getString("notes") ?: ""
                                    isPinned = doc.getBoolean("isPinned") ?: false
                                    isDone = doc.getBoolean("isDone") ?: false
                                }
                            }
                    } else {
                        // new reminder – keep defaults
                        title = ""
                        dateLabel = ""
                        timeLabel = ""
                        notes = ""
                        isPinned = false
                        isDone = false
                    }
                }

                fun saveReminder(onFinished: () -> Unit) {
                    val uid = auth.currentUser?.uid ?: return
                    if (title.isBlank()) return

                    val colRef = db.collection("users").document(uid)
                        .collection("reminders")

                    val data = hashMapOf(
                        "title" to title,
                        "dateLabel" to dateLabel.ifBlank { "Today" },
                        "timeLabel" to timeLabel.ifBlank { "09:00" },
                        "notes" to notes,
                        "isPinned" to isPinned,
                        "isDone" to isDone
                    )

                    val docRef = if (reminderId.isNullOrBlank()) {
                        // new
                        colRef.document()
                    } else {
                        // update
                        colRef.document(reminderId)
                    }

                    docRef.set(data)
                        .addOnSuccessListener {
                            onFinished()
                        }
                }

                AddEditReminderScreen(
                    isEditing = !reminderId.isNullOrBlank(),
                    title = title,
                    onTitleChange = { title = it },
                    dateLabel = dateLabel,
                    onDateChange = { dateLabel = it },
                    timeLabel = timeLabel,
                    onTimeChange = { timeLabel = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    isPinned = isPinned,
                    onPinnedChange = { isPinned = it },
                    onSave = {
                        saveReminder {
                            activity.finish()
                        }
                    },
                    onCancel = {
                        activity.finish()
                    }
                )
            }
        }
    }
}

// -------------------------
// COMPOSABLE FORM
// -------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    isEditing: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    dateLabel: String,
    onDateChange: (String) -> Unit,
    timeLabel: String,
    onTimeChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isPinned: Boolean,
    onPinnedChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (isEditing) "Edit reminder" else "Add reminder")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Basic details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dateLabel,
                onValueChange = onDateChange,
                label = { Text("Date label (e.g. Today, Tomorrow, Fri 21 Nov)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = timeLabel,
                onValueChange = onTimeChange,
                label = { Text("Time (e.g. 14:00)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = isPinned,
                    onCheckedChange = onPinnedChange
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pin this reminder")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCancel
                ) {
                    Text("Cancel")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onSave
                ) {
                    Text(if (isEditing) "Save changes" else "Save")
                }
            }
        }
    }
}
