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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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

// Purple gradient used as the background for Home screen
val PurpleAppGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF6A5AE0),   // deep purple
        Color(0xFF9575CD)    // lavender
    )
)

// -------------------------
// MODEL
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
                                "Task / shopping categories",
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
                    onClick = { selectedTab = BottomTab.CATEGORIES },
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

@Composable
private fun CategoriesContent() {
    // Category model: each category has a list of items you might want to buy
    data class Category(
        val id: String,
        val title: String,
        val description: String,
        val items: List<String>
    )

    val categories = listOf(
        Category(
            id = "groceries",
            title = "Groceries",
            description = "Everyday supermarket items",
            items = listOf(
                "Bread",
                "Milk",
                "Eggs",
                "Salt",
                "Rice",
                "Pasta",
                "Vegetables",
                "Fruits"
            )
        ),
        Category(
            id = "clothes",
            title = "Clothes",
            description = "Things to wear",
            items = listOf(
                "T-shirt",
                "Jeans",
                "Jacket",
                "Shoes",
                "Socks",
                "Hoodie"
            )
        ),
        Category(
            id = "food_out",
            title = "Food / Takeaway",
            description = "Eating out or ordering in",
            items = listOf(
                "Pizza",
                "Burger",
                "Fries",
                "Noodles",
                "Biryani"
            )
        ),
        Category(
            id = "home",
            title = "Home items",
            description = "Things for home and room",
            items = listOf(
                "Detergent",
                "Toilet paper",
                "Shampoo",
                "Soap",
                "Cleaning spray",
                "Trash bags"
            )
        ),
        Category(
            id = "electronics",
            title = "Electronics",
            description = "Gadgets and accessories",
            items = listOf(
                "Phone charger",
                "Earphones",
                "USB cable",
                "Power bank",
                "Batteries"
            )
        ),
        Category(
            id = "other",
            title = "Other",
            description = "Anything else you need",
            items = listOf(
                "Stationery",
                "Notebook",
                "Pen",
                "Gift item"
            )
        )
    )

    // Which category is currently selected
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    // Which items have been ticked per category (like a mini shopping list in memory)
    var selectedItemsByCategory by remember {
        mutableStateOf<Map<String, Set<String>>>(emptyMap())
    }

    fun toggleItem(categoryId: String, item: String) {
        val currentSet = selectedItemsByCategory[categoryId] ?: emptySet()
        val newSet = if (currentSet.contains(item)) {
            currentSet - item
        } else {
            currentSet + item
        }
        selectedItemsByCategory = selectedItemsByCategory.toMutableMap().apply {
            put(categoryId, newSet)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Task / Shopping categories",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Pick a category and tick what you want to remember to buy or do.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category cards list
        categories.forEach { category ->
            CategoryCard(
                title = category.title,
                description = category.description,
                isSelected = category.id == selectedCategoryId,
                onClick = {
                    selectedCategoryId =
                        if (selectedCategoryId == category.id) null else category.id
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Items inside the selected category
        if (selectedCategory == null) {
            Text(
                text = "Select a category above to see items like bread, salt, clothes, etc.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Items in ${selectedCategory.title}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            selectedCategory.items.forEach { itemName ->
                val isChecked =
                    selectedItemsByCategory[selectedCategory.id]?.contains(itemName) == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { toggleItem(selectedCategory.id, itemName) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = itemName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            val selectedItems = selectedItemsByCategory[selectedCategory.id].orEmpty()
            if (selectedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Things you marked in this category:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                selectedItems.forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (isSelected) 6.dp else 3.dp,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
