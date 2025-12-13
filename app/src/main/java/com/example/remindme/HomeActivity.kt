package com.example.remindme

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        Color(0xFF6A5AE0),
        Color(0xFF9575CD)
    )
)

// -------------------------
// MODELS
// -------------------------

data class ReminderEvent(
    val id: String = "",
    val title: String = "",
    val dateLabel: String = "",
    val timeLabel: String = "",
    val notes: String = "",
    val isPinned: Boolean = false,
    val isDone: Boolean = false
)

data class CategorySection(
    val id: String,
    val title: String,
    val items: List<String>
)

data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val sectionId: String = "",
    val sectionTitle: String = "",
    val isChecked: Boolean = false
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
                        activity.startActivity(Intent(activity, AddEditReminderActivity::class.java))
                    },
                    onOpenReminder = { event ->
                        activity.startActivity(
                            Intent(activity, AddEditReminderActivity::class.java).apply {
                                putExtra("reminder_id", event.id)
                            }
                        )
                    },
                    onOpenProfile = {
                        activity.startActivity(Intent(activity, ProfileActivity::class.java))
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
    var shoppingList by remember { mutableStateOf<List<ShoppingItem>>(emptyList()) }

    var reminderListener: ListenerRegistration? by remember { mutableStateOf(null) }
    var shoppingListener: ListenerRegistration? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        val uid = auth.currentUser?.uid

        if (uid != null) {
            // Reminders listener
            reminderListener = db.collection("users")
                .document(uid)
                .collection("reminders")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    reminders = snapshot.documents.map { doc ->
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
                }

            // Shopping list listener
            shoppingListener = db.collection("users")
                .document(uid)
                .collection("shoppingList")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    shoppingList = snapshot.documents.map { doc ->
                        ShoppingItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            sectionId = doc.getString("sectionId") ?: "",
                            sectionTitle = doc.getString("sectionTitle") ?: "",
                            isChecked = doc.getBoolean("isChecked") ?: false
                        )
                    }.sortedWith(compareBy<ShoppingItem> { it.isChecked }.thenBy { it.sectionTitle }.thenBy { it.name })
                }
        }

        onDispose {
            reminderListener?.remove()
            shoppingListener?.remove()
        }
    }

    // ---------------- Reminders actions ----------------
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

    fun deleteReminder(event: ReminderEvent) {
        val uid = auth.currentUser?.uid ?: return
        if (event.id.isBlank()) return
        db.collection("users").document(uid)
            .collection("reminders").document(event.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(ctx, "Reminder deleted: ${event.title}", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------- Shopping list actions ----------------
    fun addOrRemoveShoppingItem(section: CategorySection, itemName: String) {
        val uid = auth.currentUser?.uid ?: return

        // if already exists -> delete; else -> add
        val existing = shoppingList.firstOrNull { it.name == itemName && it.sectionId == section.id }
        val col = db.collection("users").document(uid).collection("shoppingList")

        if (existing != null) {
            col.document(existing.id).delete()
        } else {
            val data = hashMapOf(
                "name" to itemName,
                "sectionId" to section.id,
                "sectionTitle" to section.title,
                "isChecked" to false
            )
            col.add(data)
        }
    }

    fun toggleShoppingChecked(item: ShoppingItem) {
        val uid = auth.currentUser?.uid ?: return
        if (item.id.isBlank()) return
        db.collection("users").document(uid)
            .collection("shoppingList").document(item.id)
            .update("isChecked", !item.isChecked)
    }

    fun removeShoppingItem(item: ShoppingItem) {
        val uid = auth.currentUser?.uid ?: return
        if (item.id.isBlank()) return
        db.collection("users").document(uid)
            .collection("shoppingList").document(item.id)
            .delete()
    }

    fun clearShoppingList() {
        val uid = auth.currentUser?.uid ?: return
        val col = db.collection("users").document(uid).collection("shoppingList")
        shoppingList.forEach { itx ->
            if (itx.id.isNotBlank()) col.document(itx.id).delete()
        }
        Toast.makeText(ctx, "Shopping list cleared", Toast.LENGTH_SHORT).show()
    }

    HomeScreen(
        reminders = reminders,
        shoppingList = shoppingList,
        onToggleDone = ::toggleDone,
        onTogglePinned = ::togglePinned,
        onDeleteReminder = ::deleteReminder,
        onAddReminder = onAddReminder,
        onOpenReminder = onOpenReminder,
        onOpenProfile = onOpenProfile,
        onLogout = onLogout,
        onToggleCategoryItem = ::addOrRemoveShoppingItem,
        onToggleShoppingChecked = ::toggleShoppingChecked,
        onRemoveShoppingItem = ::removeShoppingItem,
        onClearShoppingList = ::clearShoppingList
    )
}

// -------------------------
// UI
// -------------------------

enum class BottomTab { HOME, CATEGORIES, SHOPPING }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    reminders: List<ReminderEvent>,
    shoppingList: List<ShoppingItem>,
    onToggleDone: (ReminderEvent) -> Unit,
    onTogglePinned: (ReminderEvent) -> Unit,
    onDeleteReminder: (ReminderEvent) -> Unit,
    onAddReminder: () -> Unit,
    onOpenReminder: (ReminderEvent) -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
    onToggleCategoryItem: (CategorySection, String) -> Unit,
    onToggleShoppingChecked: (ShoppingItem) -> Unit,
    onRemoveShoppingItem: (ShoppingItem) -> Unit,
    onClearShoppingList: () -> Unit
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
                            text = when (selectedTab) {
                                BottomTab.HOME -> "Your upcoming events"
                                BottomTab.CATEGORIES -> "Choose items by section"
                                BottomTab.SHOPPING -> "Your saved shopping list"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == BottomTab.HOME) {
                FloatingActionButton(onClick = onAddReminder) {
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
                NavigationBarItem(
                    selected = selectedTab == BottomTab.SHOPPING,
                    onClick = { selectedTab = BottomTab.SHOPPING },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Shopping") },
                    label = { Text("Shopping") }
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
                    onDelete = onDeleteReminder,
                    onOpenReminder = onOpenReminder
                )

                BottomTab.CATEGORIES -> CategoriesContent(
                    shoppingList = shoppingList,
                    onToggleItem = onToggleCategoryItem
                )

                BottomTab.SHOPPING -> ShoppingListContent(
                    shoppingList = shoppingList,
                    onToggleChecked = onToggleShoppingChecked,
                    onRemove = onRemoveShoppingItem,
                    onClearAll = onClearShoppingList
                )
            }
        }
    }
}

// -------------------------
// HOME TAB
// -------------------------

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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
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
// CATEGORIES TAB (saves to Firestore)
// -------------------------

@Composable
private fun CategoriesContent(
    shoppingList: List<ShoppingItem>,
    onToggleItem: (CategorySection, String) -> Unit
) {
    val sections = remember {
        listOf(
            CategorySection("produce", "Produce", listOf("Apples", "Bananas", "Tomatoes", "Onions", "Potatoes")),
            CategorySection("dairy", "Dairy", listOf("Milk", "Cheese", "Butter", "Yogurt", "Eggs")),
            CategorySection("bakery", "Bakery", listOf("Bread", "Buns", "Croissant", "Bagels")),
            CategorySection("meat", "Meat & Seafood", listOf("Chicken", "Beef", "Fish", "Shrimp")),
            CategorySection("snacks", "Snacks", listOf("Chips", "Biscuits", "Chocolate", "Nuts")),
            CategorySection("household", "Household", listOf("Detergent", "Bin bags", "Dish soap", "Toilet paper"))
        )
    }

    // expanded sections UI state
    var expanded by remember { mutableStateOf(sections.map { it.id }.toSet()) }

    fun isSaved(sectionId: String, name: String): Boolean {
        return shoppingList.any { it.sectionId == sectionId && it.name == name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Categories", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(
            "Tap items to save them. They will appear in Shopping List.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sections, key = { it.id }) { section ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expanded = if (expanded.contains(section.id)) expanded - section.id else expanded + section.id
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(section.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                            Icon(
                                imageVector = if (expanded.contains(section.id)) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Expand"
                            )
                        }

                        if (expanded.contains(section.id)) {
                            Spacer(Modifier.height(10.dp))

                            section.items.forEach { itemName ->
                                val saved = isSaved(section.id, itemName)

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onToggleItem(section, itemName) },
                                    shape = RoundedCornerShape(14.dp),
                                    tonalElevation = if (saved) 2.dp else 0.dp,
                                    color = if (saved) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (saved) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = itemName,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (saved) FontWeight.SemiBold else FontWeight.Normal)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------
// SHOPPING LIST TAB (reads saved items from Firestore)
// -------------------------

@Composable
private fun ShoppingListContent(
    shoppingList: List<ShoppingItem>,
    onToggleChecked: (ShoppingItem) -> Unit,
    onRemove: (ShoppingItem) -> Unit,
    onClearAll: () -> Unit
) {
    val grouped = remember(shoppingList) {
        shoppingList.groupBy { it.sectionTitle }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Shopping List",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )

            if (shoppingList.isNotEmpty()) {
                TextButton(onClick = onClearAll) { Text("Clear all") }
            }
        }

        Text(
            "These are the items you saved from Categories.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        if (shoppingList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No items saved yet.\nGo to Categories and tap items to save them.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            grouped.forEach { (sectionTitle, itemsInSection) ->
                item {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                items(itemsInSection, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { onToggleChecked(item) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = item.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.SemiBold
                                )
                            )
                            IconButton(onClick = { onRemove(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------
// CARDS
// -------------------------

@Composable
private fun NextReminderCard(event: ReminderEvent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
        modifier = Modifier.fillMaxWidth(),
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
                    Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (event.isDone) "Not done" else "Done")
                }

                IconButton(onClick = { onDelete(event) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
