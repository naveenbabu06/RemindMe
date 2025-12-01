package com.example.remindme

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindme.ui.theme.RemindMeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// -------------------------
// APP GRADIENT
// -------------------------

val PurpleAppGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF6A5AE0),   // deep purple
        Color(0xFF9575CD)    // lavender
    )
)

// -------------------------
// MODELS
// -------------------------

data class ReminderEvent(
    val id: String = "",
    val title: String = "",
    val dateLabel: String = "",  // e.g. "Today", "Tomorrow", "Fri 21 Nov"
    val timeLabel: String = "",  // e.g. "14:00"
    val notes: String = "",
    val isPinned: Boolean = false,
    val isDone: Boolean = false
)

// One groceries / tasks section in the Categories tab
data class CategorySection(
    val id: String,
    val title: String,
    val items: List<String>
)

// -------------------------
// ACTIVITY
// -------------------------

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RemindMeTheme {
                val activity = this
                val ctx = LocalContext.current

                HomeScreenHost(
                    onAddReminder = {
                        activity.startActivity(
                            Intent(activity, AddEditReminderActivity::class.java)
                        )
                    },
                    onOpenReminder = { event ->
                        activity.startActivity(
                            Intent(activity, AddEditReminderActivity::class.java).apply {
                                putExtra("reminder_id", event.id)
                            }
                        )
                    },
                    onOpenProfile = {
                        activity.startActivity(
                            Intent(activity, ProfileActivity::class.java)
                        )
                    },
                    onLogout = {
                        // FirebaseAuth.getInstance().signOut()
                        activity.finish()
                        Toast.makeText(ctx, "Logged out", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// -------------------------
// FIRESTORE + STATE HOST
// -------------------------

@Composable
private fun HomeScreenHost(
    onAddReminder: () -> Unit,
    onOpenReminder: (ReminderEvent) -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val ctx = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var reminders by remember { mutableStateOf<List<ReminderEvent>>(emptyList()) }
    var listener: ListenerRegistration? by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Listen to Firestore changes
    DisposableEffect(Unit) {
        val uid = auth.currentUser?.uid

        if (uid != null) {
            val colRef = db.collection("users")
                .document(uid)
                .collection("reminders")

            listener = colRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.map { doc ->
                    ReminderEvent(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        dateLabel = doc.getString("dateLabel") ?: "",
                        timeLabel = doc.getString("timeLabel") ?: "",
                        notes = doc.getString("notes") ?: "",
                        isPinned = doc.getBoolean("isPinned") ?: false,
                        isDone = doc.getBoolean("isDone") ?: false
                    )
                }
                reminders = list
            }
        }

        onDispose {
            listener?.remove()
        }
    }

    // Firestore actions
    fun toggleDone(event: ReminderEvent) {
        val uid = auth.currentUser?.uid ?: return
        if (event.id.isBlank()) return

        db.collection("users").document(uid)
            .collection("reminders").document(event.id)
            .update("isDone", !event.isDone)
    }

    fun togglePinned(event: ReminderEvent) {
        val uid = auth.currentUser?.uid ?: return
        if (event.id.isBlank()) return

        db.collection("users").document(uid)
            .collection("reminders").document(event.id)
            .update("isPinned", !event.isPinned)
    }

    fun delete(event: ReminderEvent) {
        val uid = auth.currentUser?.uid ?: return
        if (event.id.isBlank()) return

        db.collection("users").document(uid)
            .collection("reminders").document(event.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(ctx, "Reminder deleted: ${event.title}", Toast.LENGTH_SHORT).show()
            }
    }

    HomeScreen(
        reminders = reminders,
        onToggleDone = ::toggleDone,
        onTogglePinned = ::togglePinned,
        onDelete = ::delete,
        onAddReminder = onAddReminder,
        onOpenReminder = onOpenReminder,
        onOpenProfile = onOpenProfile,
        onLogout = onLogout
    )
}

// -------------------------
// COMPOSABLES
// -------------------------

enum class BottomTab {
    HOME, CATEGORIES
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    reminders: List<ReminderEvent>,
    onToggleDone: (ReminderEvent) -> Unit,
    onTogglePinned: (ReminderEvent) -> Unit,
    onDelete: (ReminderEvent) -> Unit,
    onAddReminder: () -> Unit,
    onOpenReminder: (ReminderEvent) -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

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
                            text = if (selectedTab == BottomTab.HOME)
                                "Your upcoming events"
                            else
                                "Groceries & task sections",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == BottomTab.HOME) {
                FloatingActionButton(
                    onClick = onAddReminder,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add reminder")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HOME,
                    onClick = { selectedTab = BottomTab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.CATEGORIES,
                    onClick = { selectedTab == BottomTab.CATEGORIES; selectedTab = BottomTab.CATEGORIES },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Categories") },
                    label = { Text("Categories") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PurpleAppGradient)
        ) {
            when (selectedTab) {
                BottomTab.HOME -> HomeContent(
                    sortedReminders = sortedReminders,
                    nextReminder = nextReminder,
                    onToggleDone = onToggleDone,
                    onTogglePinned = onTogglePinned,
                    onDelete = onDelete,
                    onOpenReminder = onOpenReminder
                )

                BottomTab.CATEGORIES -> CategoriesContent()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    sortedReminders: List<ReminderEvent>,
    nextReminder: ReminderEvent?,
    onToggleDone: (ReminderEvent) -> Unit,
    onTogglePinned: (ReminderEvent) -> Unit,
    onDelete: (ReminderEvent) -> Unit,
    onOpenReminder: (ReminderEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
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
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            onToggleDone = onToggleDone,
                            onTogglePinned = onTogglePinned,
                            onDelete = onDelete,
                            onClick = { onOpenReminder(event) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------
// CATEGORIES TAB – GROCERY STYLE
// -------------------------

@Composable
private fun CategoriesContent() {
    val sections = listOf(
        CategorySection(
            id = "frozen",
            title = "Frozen Foods",
            items = listOf("Ice cream", "Frozen peas", "Frozen pizza", "Mixed veg")
        ),
        CategorySection(
            id = "produce",
            title = "Produce",
            items = listOf("Apples", "Bananas", "Napa cabbage", "Melon", "Tomatoes")
        ),
        CategorySection(
            id = "sauces",
            title = "Sauces & Condiments",
            items = listOf("Almond butter", "XO sauce", "Ketchup", "Soy sauce")
        ),
        CategorySection(
            id = "seafood",
            title = "Seafood",
            items = listOf("Shrimp", "Salmon", "Tuna", "Fish fingers")
        ),
        CategorySection(
            id = "household",
            title = "Household",
            items = listOf("Laundry detergent", "Sponges", "Bin bags", "Dish soap")
        )
    )

    // Expanded sections (open/closed)
    var expandedSections by remember {
        mutableStateOf(sections.map { it.id }.toSet()) // all open at start
    }

    // Checked items per section
    var checkedItemsBySection by remember {
        mutableStateOf<Map<String, Set<String>>>(emptyMap())
    }

    fun toggleSection(sectionId: String) {
        expandedSections = if (expandedSections.contains(sectionId)) {
            expandedSections - sectionId
        } else {
            expandedSections + sectionId
        }
    }

    fun toggleItem(sectionId: String, item: String) {
        val currentSet = checkedItemsBySection[sectionId] ?: emptySet()
        val newSet = if (currentSet.contains(item)) {
            currentSet - item
        } else {
            currentSet + item
        }
        checkedItemsBySection = checkedItemsBySection.toMutableMap().apply {
            put(sectionId, newSet)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Groceries list",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Organise what you need to buy by section, then tick items as you go.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        sections.forEach { section ->
            SectionCard(
                section = section,
                isExpanded = expandedSections.contains(section.id),
                checkedItems = checkedItemsBySection[section.id].orEmpty(),
                onToggleSection = { toggleSection(section.id) },
                onToggleItem = { item -> toggleItem(section.id, item) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val totalChecked = checkedItemsBySection.values.sumOf { it.size }
        if (totalChecked > 0) {
            Text(
                text = "You’ve selected $totalChecked item(s) in this list.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(
    section: CategorySection,
    isExpanded: Boolean,
    checkedItems: Set<String>,
    onToggleSection: () -> Unit,
    onToggleItem: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSection() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                section.items.forEach { itemName ->
                    val isChecked = checkedItems.contains(itemName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleItem(itemName) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = itemName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// -------------------------
// NEXT REMINDER + REMINDER CARD
// -------------------------

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
