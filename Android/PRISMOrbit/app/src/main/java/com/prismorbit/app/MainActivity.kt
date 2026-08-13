package com.prismorbit.app

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.prismorbit.app.ui.theme.PRISMOrbitTheme
import kotlinx.coroutines.delay


// ============================================================================
// SHARED APP APPEARANCE STATE
// ============================================================================
// SYSTEM = follow device theme
// LIGHT  = force light theme
// DARK   = force dark theme
//
// SettingsScreen updates this state, and MainActivity feeds it into the
// application theme so the whole Compose tree recomposes immediately.
// ============================================================================
object PrismAppearanceState {
    var mode by mutableStateOf("SYSTEM")
}

// ============================================================================
// MAIN ACTIVITY
// ============================================================================

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContent {
            PRISMOrbitTheme(
                appearance = PrismAppearanceState.mode
            ) {

                var showOpeningScreen by remember {
                    mutableStateOf(true)
                }

                var userIsAuthenticated by remember {
                    mutableStateOf(
                        firebaseAuth.currentUser?.isEmailVerified == true
                    )
                }

                // Restore the saved appearance preference when a signed-in
                // user opens the app. SettingsScreen also updates the shared
                // state immediately when the user taps an appearance option.
                LaunchedEffect(firebaseAuth.currentUser?.uid) {
                    val uid = firebaseAuth.currentUser?.uid ?: return@LaunchedEffect

                    firestore
                        .collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            val settings = document.get("settings") as? Map<*, *>

                            PrismAppearanceState.mode =
                                settings?.get("appearance")
                                    ?.toString()
                                    ?.uppercase()
                                    ?.takeIf {
                                        it == "SYSTEM" ||
                                                it == "LIGHT" ||
                                                it == "DARK"
                                    }
                                    ?: "SYSTEM"
                        }
                }

                if (showOpeningScreen) {

                    OpeningScreen(
                        onFinished = {
                            showOpeningScreen = false
                        }
                    )

                } else if (userIsAuthenticated) {

                    ProfileRouter(
                        firebaseAuth = firebaseAuth,
                        firestore = firestore,
                        onLogout = {
                            firebaseAuth.signOut()
                            PrismAppearanceState.mode = "SYSTEM"
                            showOpeningScreen = false
                            userIsAuthenticated = false
                        }
                    )

                } else {

                    AuthenticationScreen(
                        firebaseAuth = firebaseAuth,
                        onAuthenticationSuccess = {
                            userIsAuthenticated = true
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// EMAIL AUTHENTICATION SCREEN
// ============================================================================

@Composable
fun AuthenticationScreen(
    firebaseAuth: FirebaseAuth,
    onAuthenticationSuccess: () -> Unit
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }
    var verificationSent by remember { mutableStateOf(false) }

    fun setMessage(text: String, isError: Boolean = false) {
        message = text
        messageIsError = isError
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "PRISM",
            color = Color.White,
            fontSize = 42.sp,
            letterSpacing = 5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SEE BEYOND THE GRINDS",
            color = Color(0xFFD8C8FF),
            fontSize = 10.sp,
            letterSpacing = 2.5.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "WELCOME TO PRISM",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Create your account once and continue your journey.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF151515)
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "EMAIL ACCOUNT",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email address") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ------------------------------------------------------------
                // CREATE ACCOUNT
                // ------------------------------------------------------------

                Button(
                    onClick = {

                        val cleanEmail = email.trim()

                        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                            setMessage("Please enter a valid email address.", true)
                            return@Button
                        }

                        if (password.length < 6) {
                            setMessage("Password must be at least 6 characters.", true)
                            return@Button
                        }

                        isLoading = true
                        setMessage("")

                        firebaseAuth
                            .createUserWithEmailAndPassword(
                                cleanEmail,
                                password
                            )
                            .addOnCompleteListener { task ->

                                if (task.isSuccessful) {

                                    val user = firebaseAuth.currentUser

                                    if (user != null) {
                                        user.sendEmailVerification()
                                            .addOnCompleteListener { verificationTask ->

                                                isLoading = false

                                                if (verificationTask.isSuccessful) {
                                                    verificationSent = true
                                                    setMessage(
                                                        "Verification email sent. Verify your email, then tap CHECK VERIFICATION."
                                                    )
                                                } else {
                                                    setMessage(
                                                        verificationTask.exception?.message
                                                            ?: "Account created, but verification email could not be sent.",
                                                        true
                                                    )
                                                }
                                            }
                                    } else {
                                        isLoading = false
                                        setMessage(
                                            "Account created. Please sign in again.",
                                            true
                                        )
                                    }
                                } else {
                                    isLoading = false
                                    setMessage(
                                        task.exception?.message
                                            ?: "Unable to create account.",
                                        true
                                    )
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(
                        if (isLoading) "CREATING..." else "CREATE ACCOUNT"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ------------------------------------------------------------
                // SIGN IN
                // ------------------------------------------------------------

                OutlinedButton(
                    onClick = {

                        val cleanEmail = email.trim()

                        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                            setMessage("Please enter a valid email address.", true)
                            return@OutlinedButton
                        }

                        if (password.length < 6) {
                            setMessage("Please enter your password.", true)
                            return@OutlinedButton
                        }

                        isLoading = true
                        setMessage("")

                        firebaseAuth
                            .signInWithEmailAndPassword(
                                cleanEmail,
                                password
                            )
                            .addOnCompleteListener { task ->

                                if (task.isSuccessful) {

                                    val user = firebaseAuth.currentUser

                                    if (user != null) {

                                        user.reload()
                                            .addOnCompleteListener {

                                                if (user.isEmailVerified) {
                                                    isLoading = false
                                                    onAuthenticationSuccess()
                                                } else {
                                                    isLoading = false
                                                    verificationSent = true
                                                    setMessage(
                                                        "Your email is not verified yet. Check your inbox and tap CHECK VERIFICATION after verifying.",
                                                        true
                                                    )
                                                }
                                            }
                                    } else {
                                        isLoading = false
                                        setMessage("Unable to find your account.", true)
                                    }
                                } else {
                                    isLoading = false
                                    setMessage(
                                        task.exception?.message
                                            ?: "Sign in failed.",
                                        true
                                    )
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(
                        if (isLoading) "SIGNING IN..." else "SIGN IN"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ------------------------------------------------------------
                // CHECK VERIFICATION
                // ------------------------------------------------------------

                if (verificationSent || firebaseAuth.currentUser != null) {

                    TextButton(
                        onClick = {

                            val user = firebaseAuth.currentUser

                            if (user == null) {
                                setMessage(
                                    "Please create an account or sign in first.",
                                    true
                                )
                                return@TextButton
                            }

                            isLoading = true

                            user.reload()
                                .addOnCompleteListener { reloadTask ->

                                    if (reloadTask.isSuccessful && user.isEmailVerified) {
                                        isLoading = false
                                        onAuthenticationSuccess()
                                    } else {
                                        isLoading = false
                                        setMessage(
                                            "Email is still not verified. Verify the email first, then try again.",
                                            true
                                        )
                                    }
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("CHECK VERIFICATION")
                    }

                    TextButton(
                        onClick = {

                            val user = firebaseAuth.currentUser

                            if (user == null) {
                                setMessage(
                                    "Create an account first.",
                                    true
                                )
                                return@TextButton
                            }

                            isLoading = true

                            user.sendEmailVerification()
                                .addOnCompleteListener { task ->

                                    isLoading = false

                                    if (task.isSuccessful) {
                                        setMessage(
                                            "A new verification email has been sent."
                                        )
                                    } else {
                                        setMessage(
                                            task.exception?.message
                                                ?: "Unable to resend verification email.",
                                            true
                                        )
                                    }
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("RESEND VERIFICATION EMAIL")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ------------------------------------------------------------
                // FORGOT PASSWORD
                // ------------------------------------------------------------

                TextButton(
                    onClick = {

                        val cleanEmail = email.trim()

                        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                            setMessage(
                                "Enter your email address first.",
                                true
                            )
                            return@TextButton
                        }

                        isLoading = true

                        firebaseAuth
                            .sendPasswordResetEmail(cleanEmail)
                            .addOnCompleteListener { task ->

                                isLoading = false

                                if (task.isSuccessful) {
                                    setMessage(
                                        "Password reset email sent."
                                    )
                                } else {
                                    setMessage(
                                        task.exception?.message
                                            ?: "Unable to send password reset email.",
                                        true
                                    )
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("FORGOT PASSWORD")
                }
            }
        }

        if (message.isNotBlank()) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                color = if (messageIsError) {
                    Color(0xFFFF8A80)
                } else {
                    Color(0xFFB9F6CA)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your account stays signed in on this device.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ============================================================================
// PROFILE ROUTER
// ============================================================================

@Composable
fun ProfileRouter(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    onLogout: () -> Unit
) {

    val user = firebaseAuth.currentUser

    var profileExists by remember { mutableStateOf<Boolean?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    if (user == null) {
        onLogout()
        return
    }

    LaunchedEffect(user.uid) {

        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                profileExists = document.exists()
            }
            .addOnFailureListener { exception ->
                errorMessage = exception.message
                    ?: "Unable to load your profile."
            }
    }

    when {
        errorMessage.isNotBlank() -> {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "PROFILE ERROR",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            errorMessage = ""
                            profileExists = null
                        }
                    ) {
                        Text("TRY AGAIN")
                    }
                }
            }
        }

        profileExists == null -> {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading your PRISM profile...")
            }
        }

        profileExists == false -> {

            ProfileSetupScreen(
                firebaseAuth = firebaseAuth,
                firestore = firestore,
                onProfileSaved = {
                    profileExists = true
                }
            )
        }
        else -> {
            HomeScreen(onLogout = onLogout)
        }

    }
}

// ============================================================================
// PROFILE SETUP SCREEN
// ============================================================================

@Composable
fun ProfileSetupScreen(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    onProfileSaved: () -> Unit
) {

    val user = firebaseAuth.currentUser

    var fullName by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }

    if (user == null) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PRISM",
            color = Color.White,
            fontSize = 42.sp,
            letterSpacing = 5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "CREATE YOUR PROFILE",
            color = Color(0xFFD8C8FF),
            fontSize = 11.sp,
            letterSpacing = 2.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Set up your profile once. Your information will be saved to your PRISM account and available on your other devices.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF151515)
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "PERSONAL INFORMATION",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = user.email ?: "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    enabled = false,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "ACADEMIC INFORMATION",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = college,
                    onValueChange = {
                        college = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("College / University") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = course,
                    onValueChange = {
                        course = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Course / Branch") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = semester,
                    onValueChange = {
                        semester = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Semester") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cgpa,
                    onValueChange = {
                        cgpa = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("CGPA") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "SKILLS & ABOUT YOU",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = skills,
                    onValueChange = {
                        skills = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Skills") },
                    placeholder = { Text("C++, Java, Python, DSA...") },
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = about,
                    onValueChange = {
                        about = it
                        message = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("About you") },
                    minLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {

                        if (fullName.trim().isBlank()) {
                            message = "Please enter your full name."
                            messageIsError = true
                            return@Button
                        }

                        if (college.trim().isBlank()) {
                            message = "Please enter your college / university."
                            messageIsError = true
                            return@Button
                        }

                        if (course.trim().isBlank()) {
                            message = "Please enter your course / branch."
                            messageIsError = true
                            return@Button
                        }

                        isSaving = true
                        message = "Saving your PRISM profile..."
                        messageIsError = false

                        val profile = hashMapOf(
                            "uid" to user.uid,
                            "name" to fullName.trim(),
                            "email" to (user.email ?: ""),
                            "college" to college.trim(),
                            "course" to course.trim(),
                            "semester" to semester.trim(),
                            "cgpa" to cgpa.trim(),
                            "skills" to skills.trim(),
                            "about" to about.trim(),
                            "profileCompleted" to true
                        )

                        firestore
                            .collection("users")
                            .document(user.uid)
                            .set(profile)
                            .addOnSuccessListener {
                                isSaving = false
                                message = "Profile saved successfully."
                                messageIsError = false
                                onProfileSaved()
                            }
                            .addOnFailureListener { exception ->
                                isSaving = false
                                message = exception.message
                                    ?: "Unable to save your profile."
                                messageIsError = true
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text(
                        if (isSaving) {
                            "SAVING..."
                        } else {
                            "SAVE & CONTINUE"
                        }
                    )
                }

                if (message.isNotBlank()) {

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = message,
                        color = if (messageIsError) {
                            Color(0xFFFF8A80)
                        } else {
                            Color(0xFFB9F6CA)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your profile is linked to your PRISM account, not to a single device.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ============================================================================
// PRISM OPENING SCREEN
// ============================================================================

@Composable
fun OpeningScreen(
    onFinished: () -> Unit
) {

    val animation =
        remember {
            Animatable(0f)
        }


    LaunchedEffect(Unit) {

        animation.animateTo(
            targetValue = 1f,

            animationSpec =
                tween(
                    durationMillis = 7000,
                    easing =
                        FastOutSlowInEasing
                )
        )

        delay(1000)

        onFinished()
    }


    val progress =
        animation.value


    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        val screenWidth = maxWidth
        val screenHeight = maxHeight


        val features =
            listOf(
                "CGPA",
                "DSA",
                "PROJECTS",
                "INTERNSHIPS",
                "PLACEMENT",
                "GROWTH",
                "SMART AI"
            )


        val colors =
            listOf(
                Color(0xFFFF1744),
                Color(0xFFFF9100),
                Color(0xFFFFEA00),
                Color(0xFF64DD17),
                Color(0xFF00E5FF),
                Color(0xFF2979FF),
                Color(0xFFD500F9)
            )


        val featureY =
            listOf(
                0.215f,
                0.270f,
                0.325f,
                0.380f,
                0.435f,
                0.490f,
                0.545f
            )


        // =====================================================================
        // CANVAS
        // =====================================================================

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val width = size.width
            val height = size.height


            drawRect(
                color = Color.Black
            )


            // -----------------------------------------------------------------
            // PRISM
            // -----------------------------------------------------------------

            val centerX =
                width * 0.38f

            val centerY =
                height * 0.39f

            val prismWidth =
                width * 0.34f

            val prismHeight =
                height * 0.25f

            val topX =
                centerX

            val topY =
                centerY - prismHeight / 2f

            val leftX =
                centerX - prismWidth / 2f

            val leftY =
                centerY + prismHeight / 2f

            val rightX =
                centerX + prismWidth / 2f

            val rightY =
                centerY + prismHeight / 2f


            // -----------------------------------------------------------------
            // GLOW
            // -----------------------------------------------------------------

            drawLine(
                color =
                    Color(0xFF9EEAFF)
                        .copy(alpha = 0.13f),

                start =
                    androidx.compose.ui.geometry.Offset(
                        topX,
                        topY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        leftX,
                        leftY
                    ),

                strokeWidth =
                    width * 0.040f,

                cap =
                    StrokeCap.Round
            )


            drawLine(
                color =
                    Color(0xFF9EEAFF)
                        .copy(alpha = 0.13f),

                start =
                    androidx.compose.ui.geometry.Offset(
                        topX,
                        topY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        rightX,
                        rightY
                    ),

                strokeWidth =
                    width * 0.040f,

                cap =
                    StrokeCap.Round
            )


            drawLine(
                color =
                    Color(0xFF9EEAFF)
                        .copy(alpha = 0.13f),

                start =
                    androidx.compose.ui.geometry.Offset(
                        leftX,
                        leftY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        rightX,
                        rightY
                    ),

                strokeWidth =
                    width * 0.040f,

                cap =
                    StrokeCap.Round
            )


            // -----------------------------------------------------------------
            // GLASS
            // -----------------------------------------------------------------

            drawLine(
                color =
                    Color(0xFF7282B0)
                        .copy(alpha = 0.28f),

                start =
                    androidx.compose.ui.geometry.Offset(
                        topX,
                        topY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        leftX,
                        leftY
                    ),

                strokeWidth =
                    width * 0.020f,

                cap =
                    StrokeCap.Round
            )


            drawLine(
                color =
                    Color(0xFF7282B0)
                        .copy(alpha = 0.28f),

                start =
                    androidx.compose.ui.geometry.Offset(
                        topX,
                        topY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        rightX,
                        rightY
                    ),

                strokeWidth =
                    width * 0.020f,

                cap =
                    StrokeCap.Round
            )


            drawLine(
                color =
                    Color(0xFF7282B0)
                        .copy(alpha = 0.28f),

                start =
                    androidx.compose.ui.geometry.Offset(
                        leftX,
                        leftY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        rightX,
                        rightY
                    ),

                strokeWidth =
                    width * 0.020f,

                cap =
                    StrokeCap.Round
            )


            // -----------------------------------------------------------------
            // WHITE OUTLINE
            // -----------------------------------------------------------------

            drawLine(
                color =
                    Color.White.copy(
                        alpha = 0.96f
                    ),

                start =
                    androidx.compose.ui.geometry.Offset(
                        topX,
                        topY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        leftX,
                        leftY
                    ),

                strokeWidth =
                    width * 0.0045f,

                cap =
                    StrokeCap.Round
            )


            drawLine(
                color =
                    Color.White.copy(
                        alpha = 0.96f
                    ),

                start =
                    androidx.compose.ui.geometry.Offset(
                        topX,
                        topY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        rightX,
                        rightY
                    ),

                strokeWidth =
                    width * 0.0045f,

                cap =
                    StrokeCap.Round
            )


            drawLine(
                color =
                    Color.White.copy(
                        alpha = 0.96f
                    ),

                start =
                    androidx.compose.ui.geometry.Offset(
                        leftX,
                        leftY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        rightX,
                        rightY
                    ),

                strokeWidth =
                    width * 0.0045f,

                cap =
                    StrokeCap.Round
            )


            // -----------------------------------------------------------------
            // INNER SHINE
            // -----------------------------------------------------------------

            drawLine(
                color =
                    Color(0xFFBDEFFF)
                        .copy(alpha = 0.75f),

                start =
                    androidx.compose.ui.geometry.Offset(
                        topX + width * 0.008f,
                        topY + height * 0.008f
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        leftX + width * 0.014f,
                        leftY - height * 0.008f
                    ),

                strokeWidth =
                    width * 0.002f,

                cap =
                    StrokeCap.Round
            )


            // -----------------------------------------------------------------
            // WHITE LIGHT
            // -----------------------------------------------------------------

            val whiteProgress =
                ((progress - 0.00f) / 0.25f)
                    .coerceIn(
                        0f,
                        1f
                    )

            val whiteStartX =
                -width * 0.08f

            val whiteStartY =
                centerY

            val whiteEndX =
                leftX + prismWidth * 0.35f

            val whiteCurrentX =
                whiteStartX +
                        (whiteEndX - whiteStartX) *
                        whiteProgress


            drawLine(
                color =
                    Color.White.copy(
                        alpha =
                            whiteProgress * 0.18f
                    ),

                start =
                    androidx.compose.ui.geometry.Offset(
                        whiteStartX,
                        whiteStartY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        whiteCurrentX,
                        whiteStartY
                    ),

                strokeWidth =
                    width * 0.025f,

                cap =
                    StrokeCap.Round
            )


            drawLine(
                color =
                    Color.White.copy(
                        alpha =
                            whiteProgress
                    ),

                start =
                    androidx.compose.ui.geometry.Offset(
                        whiteStartX,
                        whiteStartY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        whiteCurrentX,
                        whiteStartY
                    ),

                strokeWidth =
                    width * 0.0045f,

                cap =
                    StrokeCap.Round
            )


            // -----------------------------------------------------------------
            // RAINBOW
            // -----------------------------------------------------------------

            val rainbowProgress =
                ((progress - 0.20f) / 0.42f)
                    .coerceIn(
                        0f,
                        1f
                    )

            val originX =
                rightX -
                        prismWidth * 0.16f

            val originY =
                centerY


            for (i in 0..6) {

                val targetX =
                    width * 0.755f

                val targetY =
                    height * featureY[i]

                val currentX =
                    originX +
                            (targetX - originX) *
                            rainbowProgress

                val currentY =
                    originY +
                            (targetY - originY) *
                            rainbowProgress


                drawLine(
                    color =
                        colors[i].copy(
                            alpha =
                                rainbowProgress * 0.20f
                        ),

                    start =
                        androidx.compose.ui.geometry.Offset(
                            originX,
                            originY
                        ),

                    end =
                        androidx.compose.ui.geometry.Offset(
                            currentX,
                            currentY
                        ),

                    strokeWidth =
                        width * 0.023f,

                    cap =
                        StrokeCap.Round
                )


                drawLine(
                    color =
                        colors[i].copy(
                            alpha =
                                rainbowProgress
                        ),

                    start =
                        androidx.compose.ui.geometry.Offset(
                            originX,
                            originY
                        ),

                    end =
                        androidx.compose.ui.geometry.Offset(
                            currentX,
                            currentY
                        ),

                    strokeWidth =
                        width * 0.0045f,

                    cap =
                        StrokeCap.Round
                )


                drawLine(
                    color =
                        Color.White.copy(
                            alpha =
                                rainbowProgress * 0.28f
                        ),

                    start =
                        androidx.compose.ui.geometry.Offset(
                            originX,
                            originY
                        ),

                    end =
                        androidx.compose.ui.geometry.Offset(
                            currentX,
                            currentY
                        ),

                    strokeWidth =
                        width * 0.0012f,

                    cap =
                        StrokeCap.Round
                )


                if (rainbowProgress > 0.80f) {

                    drawCircle(
                        color =
                            colors[i].copy(
                                alpha = 0.14f
                            ),

                        radius =
                            width * 0.027f,

                        center =
                            androidx.compose.ui.geometry.Offset(
                                targetX,
                                targetY
                            )
                    )


                    drawCircle(
                        color =
                            colors[i],

                        radius =
                            width * 0.014f,

                        center =
                            androidx.compose.ui.geometry.Offset(
                                targetX,
                                targetY
                            )
                    )


                    drawCircle(
                        color =
                            Color.Black,

                        radius =
                            width * 0.009f,

                        center =
                            androidx.compose.ui.geometry.Offset(
                                targetX,
                                targetY
                            )
                    )
                }
            }


            // -----------------------------------------------------------------
            // DISPERSION POINT
            // -----------------------------------------------------------------

            if (rainbowProgress > 0f) {

                drawCircle(
                    color =
                        Color.White.copy(
                            alpha =
                                rainbowProgress * 0.18f
                        ),

                    radius =
                        width * 0.035f,

                    center =
                        androidx.compose.ui.geometry.Offset(
                            originX,
                            originY
                        )
                )


                drawCircle(
                    color =
                        Color.White,

                    radius =
                        width * 0.005f,

                    center =
                        androidx.compose.ui.geometry.Offset(
                            originX,
                            originY
                        )
                )
            }


            // -----------------------------------------------------------------
            // LOADING BAR
            // -----------------------------------------------------------------

            val loadingProgress =
                ((progress - 0.55f) / 0.45f)
                    .coerceIn(
                        0f,
                        1f
                    )

            val barStartX =
                width * 0.16f

            val barEndX =
                width * 0.84f

            val barY =
                height * 0.775f


            drawLine(
                color =
                    Color.White.copy(
                        alpha = 0.12f
                    ),

                start =
                    androidx.compose.ui.geometry.Offset(
                        barStartX,
                        barY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        barEndX,
                        barY
                    ),

                strokeWidth =
                    width * 0.008f,

                cap =
                    StrokeCap.Round
            )


            drawLine(
                color =
                    Color.White.copy(
                        alpha =
                            loadingProgress * 0.85f
                    ),

                start =
                    androidx.compose.ui.geometry.Offset(
                        barStartX,
                        barY
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(
                        barStartX +
                                (barEndX - barStartX) *
                                loadingProgress,

                        barY
                    ),

                strokeWidth =
                    width * 0.0025f,

                cap =
                    StrokeCap.Round
            )
        }


        // =====================================================================
        // FEATURE LABELS
        // =====================================================================

        val labelAlpha =
            ((progress - 0.48f) / 0.20f)
                .coerceIn(
                    0f,
                    1f
                )


        for (i in 0..6) {

            Text(
                text = features[i],

                color = Color.White,

                fontSize =
                    if (features[i].length > 9) {
                        8.sp
                    } else {
                        9.sp
                    },

                letterSpacing = 1.4.sp,

                modifier =
                    Modifier
                        .padding(
                            start =
                                screenWidth * 0.775f,

                            top =
                                screenHeight *
                                        featureY[i]
                        )
                        .alpha(
                            labelAlpha
                        )
            )
        }


        // =====================================================================
        // LOGO
        // =====================================================================

        val logoAlpha =
            ((progress - 0.48f) / 0.25f)
                .coerceIn(
                    0f,
                    1f
                )


        Text(
            text = "PRISM",

            color = Color.White,

            fontSize = 40.sp,

            letterSpacing = 4.sp,

            modifier =
                Modifier
                    .padding(
                        start =
                            screenWidth * 0.20f,

                        top =
                            screenHeight * 0.635f
                    )
                    .alpha(
                        logoAlpha
                    )
        )


        // =====================================================================
        // TAGLINE
        // =====================================================================

        Text(
            text =
                "SEE BEYOND THE GRINDS",

            color =
                Color(0xFFD8C8FF),

            fontSize = 10.sp,

            letterSpacing = 2.5.sp,

            modifier =
                Modifier
                    .padding(
                        start =
                            screenWidth * 0.22f,

                        top =
                            screenHeight * 0.690f
                    )
                    .alpha(
                        logoAlpha
                    )
        )


        // =====================================================================
        // LOADING TEXT
        // =====================================================================

        val loadingAlpha =
            ((progress - 0.55f) / 0.30f)
                .coerceIn(
                    0f,
                    1f
                )


        Text(
            text =
                "LOADING NEW POSSIBILITIES...",

            color =
                Color.White.copy(
                    alpha = 0.55f
                ),

            fontSize = 8.sp,

            letterSpacing = 2.sp,

            modifier =
                Modifier
                    .padding(
                        start =
                            screenWidth * 0.28f,

                        top =
                            screenHeight * 0.805f
                    )
                    .alpha(
                        loadingAlpha
                    )
        )
    }
}