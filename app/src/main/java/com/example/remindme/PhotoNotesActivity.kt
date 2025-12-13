package com.example.remindme

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.remindme.ui.theme.RemindMeTheme

class PhotoNotesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RemindMeTheme {
                PhotoNotesScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoNotesScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val purpleAppGradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF6A5AE0), Color(0xFF9575CD))
        )
    }

    // In-memory photos (simple)
    var photos by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Camera preview launcher (NO Uri, NO FileProvider, NO coil)
    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photos = listOf(bitmap) + photos
            Toast.makeText(context, "Photo added", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            takePicturePreviewLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun openCamera() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            takePicturePreviewLauncher.launch(null)
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Preview dialog
    if (previewBitmap != null) {
        AlertDialog(
            onDismissRequest = { previewBitmap = null },
            confirmButton = {
                TextButton(onClick = { previewBitmap = null }) { Text("Close") }
            },
            title = { Text("Preview") },
            text = {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Photo Notes", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Capture papers / tasks to remember",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { openCamera() }) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Camera")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { openCamera() }) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Take photo")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(purpleAppGradient)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Saved photos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(12.dp))

                    if (photos.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ImageIcon, contentDescription = null)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "No photos yet.\nTap the camera button to add one.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = photos,
                                key = { it.hashCode() }
                            ) { bmp ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 2.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Photo",
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { previewBitmap = bmp }
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        Text(
                                            text = "Tap image to preview",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        IconButton(onClick = {
                                            photos = photos.filterNot { it == bmp }
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
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
}
