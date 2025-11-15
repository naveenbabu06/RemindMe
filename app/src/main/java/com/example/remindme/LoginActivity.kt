package com.example.remindme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemindMeTheme {
                LoginScreen(
                    onLoginSuccess = {
                        // TODO: Navigate to your Home/Main screen.
                        // e.g., startActivity(Intent(this, MainActivity::class.java))
                        // finish()
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

private enum class LoginMode { EMAIL, MOBILE }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(LoginMode.EMAIL) }

    // Email state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailLoading by remember { mutableStateOf(false) }

    // Mobile state
    var dialCode by remember { mutableStateOf("+44") } // default UK for demo
    var phone by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var mobileLoading by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableStateOf(0) } // seconds

    // Simple validators
    fun isValidEmail(text: String) =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(text)

    fun isValidPhone(text: String) =
        Regex("^[0-9]{7,15}$").matches(text.filter { it.isDigit() })

    fun isValidOtp(text: String) =
        Regex("^[0-9]{4,8}$").matches(text)

    // Fake email login
    fun loginWithEmail() {
        if (!isValidEmail(email)) {
            Toast.makeText(ctx, "Please enter a valid email.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(ctx, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }
        emailLoading = true
        scope.launch {
            delay(1200)
            emailLoading = false
            onLoginSuccess()
        }
    }

    // Fake OTP sending
    fun sendOtp() {
        if (!isValidPhone(phone)) {
            Toast.makeText(ctx, "Enter a valid phone number (digits only).", Toast.LENGTH_SHORT).show()
            return
        }
        mobileLoading = true
        scope.launch {
            delay(900)
            mobileLoading = false
            otpSent = true
            Toast.makeText(ctx, "OTP sent to $dialCode ${phone}", Toast.LENGTH_SHORT).show()
            // Start a 30s resend cooldown
            resendCooldown = 30
            while (resendCooldown > 0) {
                delay(1000)
                resendCooldown -= 1
            }
        }
    }

    // Fake OTP verify
    fun verifyOtp() {
        if (!isValidOtp(otpCode)) {
            Toast.makeText(ctx, "Enter the 6-digit OTP.", Toast.LENGTH_SHORT).show()
            return
        }
        mobileLoading = true
        scope.launch {
            delay(900)
            mobileLoading = false
            onLoginSuccess()
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
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 40.dp,          //
                    start = 8.dp,
                    end = 8.dp
                )
        ) {

            Spacer(Modifier.width(8.dp))
            Text(
                text = "Welcome to RemindMe",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }


        // Card
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
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose a method to continue",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(Modifier.height(16.dp))

            // Toggle chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = mode == LoginMode.EMAIL,
                    onClick = { mode = LoginMode.EMAIL },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                )
                FilterChip(
                    selected = mode == LoginMode.MOBILE,
                    onClick = { mode = LoginMode.MOBILE },
                    label = { Text("Mobile") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "mode"
            ) { current ->
                when (current) {
                    LoginMode.EMAIL -> {
                        EmailLoginSection(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            loading = emailLoading,
                            onSubmit = ::loginWithEmail
                        )
                    }
                    LoginMode.MOBILE -> {
                        MobileLoginSection(
                            dialCode = dialCode,
                            onDialCodeChange = { dialCode = it },
                            phone = phone,
                            onPhoneChange = { phone = it.filter { ch -> ch.isDigit() } },
                            otpSent = otpSent,
                            otpCode = otpCode,
                            onOtpChange = { otpCode = it.filter { ch -> ch.isDigit() }.take(6) },
                            loading = mobileLoading,
                            resendCooldown = resendCooldown,
                            onSendOtp = ::sendOtp,
                            onVerify = ::verifyOtp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "By continuing you agree to our Terms & Privacy Policy.",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
    onSubmit: () -> Unit
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
        Text(
            text = "Forgot password?",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    // TODO: Implement reset flow or deep-link
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
                modifier = Modifier.clickable {
                    // TODO: Navigate to Sign Up screen
                }
            )
        }
    }
}

@Composable
private fun MobileLoginSection(
    dialCode: String,
    onDialCodeChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    otpSent: Boolean,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    loading: Boolean,
    resendCooldown: Int,
    onSendOtp: () -> Unit,
    onVerify: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Country code + phone in a row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = dialCode,
                onValueChange = { value ->
                    // Keep + and digits only, max 5-6 chars
                    val sanitized = value.filter { it == '+' || it.isDigit() }.take(5)
                    onDialCodeChange(
                        if (sanitized.startsWith("+")) sanitized else "+$sanitized"
                    )
                },
                label = { Text("Code") },
                singleLine = true,
                modifier = Modifier.width(90.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Mobile number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = if (otpSent) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (!otpSent) onSendOtp() })
            )
        }

        if (!otpSent) {
            Button(
                onClick = onSendOtp,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Send OTP")
                }
            }
        } else {
            OutlinedTextField(
                value = otpCode,
                onValueChange = onOtpChange,
                label = { Text("Enter OTP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onVerify() })
            )

            Button(
                onClick = onVerify,
                enabled = !loading && otpCode.length >= 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Verify & Continue")
                }
            }

            TextButton(
                onClick = onSendOtp,
                enabled = resendCooldown == 0 && !loading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (resendCooldown == 0) "Resend OTP"
                    else "Resend in ${resendCooldown}s"
                )
            }
        }
    }
}
