package com.example.remindme

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindme.ui.theme.RemindMeTheme
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()

        setContent {
            RemindMeTheme {
                LoginScreen(
                    auth = auth,
                    onLoginSuccess = {
                        // Go to HomeActivity after login
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    },
                    onNavigateToSignup = {
                        startActivity(Intent(this, AccountActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    val ctx = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun isValidEmail(text: String) =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(text)

    fun loginWithEmail() {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (!isValidEmail(trimmedEmail)) {
            errorMessage = "Please enter a valid email."
            return
        }
        if (trimmedPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }

        errorMessage = null
        loading = true

        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
            .addOnCompleteListener { task ->
                loading = false
                if (task.isSuccessful) {
                    Toast.makeText(ctx, "Login successful", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    errorMessage = task.exception?.message ?: "Login failed. Please try again."
                }
            }
    }

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF6A5AE0), Color(0xFF9575CD))
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome to RemindMe",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Login Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Use your email and password to continue",
                style = MaterialTheme.typography.bodyMedium
                    .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(Modifier.height(16.dp))

            EmailLoginSection(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                loading = loading,
                errorMessage = errorMessage,
                onSubmit = { loginWithEmail() },
                onCreateAccount = onNavigateToSignup
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "By continuing you agree to our Terms & Privacy Policy.",
                style = MaterialTheme.typography.bodySmall
                    .copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Footer
        Text(
            text = "RemindMe • Simple Event Reminder",
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun EmailLoginSection(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    loading: Boolean,
    errorMessage: String?,
    onSubmit: () -> Unit,
    onCreateAccount: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        var passwordVisible by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            trailingIcon = {
                val text = if (passwordVisible) "Hide" else "Show"
                Text(
                    text,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { passwordVisible = !passwordVisible }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Text(
            text = "Forgot password?",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    // TODO: Reset password later
                }
        )

        Button(
            onClick = onSubmit,
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text("Sign In")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("New here? ")
            Text(
                text = "Create account",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onCreateAccount() }
            )
        }
    }
}
