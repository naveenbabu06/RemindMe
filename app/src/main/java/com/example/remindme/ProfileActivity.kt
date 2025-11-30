package com.example.remindme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindme.ui.theme.RemindMeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            RemindMeTheme {
                ProfileScreen(
                    auth = auth,
                    db = db,
                    onBack = { finish() },
                    onLogout = {
                        auth.signOut()
                        // Go back to login/signup screen
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser = auth.currentUser
    var createdAtText by remember { mutableStateOf<String?>(null) }

    // Load extra user info from Firestore (optional)
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val createdAt = doc.getTimestamp("createdAt")?.toDate()
                createdAtText = createdAt?.toString()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Account details",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Email:",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = currentUser?.email ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (createdAtText != null) {
                Text(
                    text = "Member since:",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = createdAtText ?: "",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log out")
            }
        }
    }
}
