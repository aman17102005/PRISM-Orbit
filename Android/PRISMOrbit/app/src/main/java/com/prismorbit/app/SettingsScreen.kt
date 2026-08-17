package com.prismorbit.app

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// =========================================================
// SETTINGS SCREEN
// =========================================================

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var college by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }

    var appearance by remember { mutableStateOf("SYSTEM") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var followUpReminders by remember { mutableStateOf(true) }

    var editingProfile by remember { mutableStateOf(false) }
    var savingProfile by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null || user == null) return@rememberLauncherForActivityResult

        val uid = user.uid
        val collections = listOf(
            "academicEvents",
            "dsaProblems",
            "projects",
            "internships",
            "placementSkills",
            "placementAchievements",
            "placementCertifications",
            "placementLearning"
        )

        message = "Preparing your data export..."
        messageIsError = false

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { profileDocument ->
                val output = StringBuilder()
                output.appendLine("PRISMOrbit Data Export")
                output.appendLine("User ID: $uid")
                output.appendLine()
                output.appendLine("PROFILE")
                profileDocument.data?.forEach { (key, value) ->
                    output.appendLine("$key: $value")
                }
                output.appendLine()

                fun loadCollection(index: Int) {
                    if (index >= collections.size) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                stream.write(output.toString().toByteArray(Charsets.UTF_8))
                            }
                            message = "Data exported successfully."
                            messageIsError = false
                        } catch (e: Exception) {
                            message = e.message ?: "Unable to create export file."
                            messageIsError = true
                        }
                        return
                    }

                    val collectionName = collections[index]
                    firestore.collection("users").document(uid)
                        .collection(collectionName).get()
                        .addOnSuccessListener { snapshot ->
                            output.appendLine(collectionName.uppercase())
                            snapshot.documents.forEach { document ->
                                output.appendLine("ID: ${document.id}")
                                document.data?.forEach { (key, value) ->
                                    output.appendLine("$key: $value")
                                }
                                output.appendLine()
                            }
                            loadCollection(index + 1)
                        }
                        .addOnFailureListener { error ->
                            message = error.message ?: "Unable to export $collectionName."
                            messageIsError = true
                        }
                }

                loadCollection(0)
            }
            .addOnFailureListener { error ->
                message = error.message ?: "Unable to load your account data."
                messageIsError = true
            }
    }

    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                fullName = document.getString("name") ?: ""
                email = document.getString("email") ?: (user.email ?: "")
                college = document.getString("college") ?: ""
                course = document.getString("course") ?: ""
                semester = document.getString("semester") ?: ""
                cgpa = document.getString("cgpa") ?: ""
                skills = document.getString("skills") ?: ""
                about = document.getString("about") ?: ""

                val settings = document.get("settings") as? Map<*, *>
                appearance = settings?.get("appearance")?.toString()?.uppercase()
                    ?.takeIf { it == "SYSTEM" || it == "LIGHT" || it == "DARK" }
                    ?: "SYSTEM"
                PrismAppearanceState.mode = appearance
                notificationsEnabled = settings?.get("notificationsEnabled") as? Boolean ?: true
                followUpReminders = settings?.get("followUpReminders") as? Boolean ?: true
            }
            .addOnFailureListener { error ->
                message = error.message ?: "Unable to load settings."
                messageIsError = true
            }
    }

    fun savePreferences() {
        val uid = user?.uid ?: return
        firestore.collection("users").document(uid)
            .set(
                mapOf(
                    "settings" to mapOf(
                        "appearance" to appearance,
                        "notificationsEnabled" to notificationsEnabled,
                        "followUpReminders" to followUpReminders
                    )
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { error ->
                message = error.message ?: "Unable to save settings."
                messageIsError = true
            }
    }

    fun saveProfile() {
        val currentUser = auth.currentUser ?: return
        savingProfile = true
        message = ""

        val profile = mapOf(
            "name" to fullName.trim(),
            "email" to email.trim(),
            "college" to college.trim(),
            "course" to course.trim(),
            "semester" to semester.trim(),
            "cgpa" to cgpa.trim(),
            "skills" to skills.trim(),
            "about" to about.trim()
        )

        val saveFirestore: () -> Unit = {
            firestore.collection("users").document(currentUser.uid)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener {
                    savingProfile = false
                    editingProfile = false
                    message = "Profile updated successfully."
                    messageIsError = false
                }
                .addOnFailureListener { error ->
                    savingProfile = false
                    message = error.message ?: "Unable to save your profile."
                    messageIsError = true
                }
        }

        val newEmail = email.trim()
        val oldEmail = currentUser.email?.trim().orEmpty()

        if (newEmail.isNotBlank() && newEmail != oldEmail) {
            currentUser.updateEmail(newEmail)
                .addOnSuccessListener { saveFirestore() }
                .addOnFailureListener { error ->
                    savingProfile = false
                    message = error.message ?: "Unable to update email. You may need to sign in again."
                    messageIsError = true
                }
        } else {
            saveFirestore()
        }
    }

    fun deleteAllUserDataAndAccount() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val collections = listOf(
            "academicEvents",
            "dsaProblems",
            "projects",
            "internships",
            "placementSkills",
            "placementAchievements",
            "placementCertifications",
            "placementLearning"
        )

        deletingAccount = true

        fun deleteCollection(index: Int) {
            if (index >= collections.size) {
                firestore.collection("users").document(uid).delete()
                    .addOnSuccessListener {
                        currentUser.delete()
                            .addOnSuccessListener {
                                deletingAccount = false
                                onLogout()
                            }
                            .addOnFailureListener { error ->
                                deletingAccount = false
                                message = error.message ?: "Unable to delete account. Sign in again and retry."
                                messageIsError = true
                            }
                    }
                    .addOnFailureListener { error ->
                        deletingAccount = false
                        message = error.message ?: "Unable to remove account data."
                        messageIsError = true
                    }
                return
            }

            firestore.collection("users").document(uid)
                .collection(collections[index]).get()
                .addOnSuccessListener { snapshot ->
                    val batch = firestore.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit()
                        .addOnSuccessListener { deleteCollection(index + 1) }
                        .addOnFailureListener { error ->
                            deletingAccount = false
                            message = error.message ?: "Unable to delete ${collections[index]}."
                            messageIsError = true
                        }
                }
                .addOnFailureListener { error ->
                    deletingAccount = false
                    message = error.message ?: "Unable to access ${collections[index]}."
                    messageIsError = true
                }
        }

        deleteCollection(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onBack() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "‹",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SETTINGS",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "PRISM CONTROL CENTER",
                    color = Color(0xFFB76CFF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )
            }

            Text(
                "SECURE",
                color = Color(0xFF65E572),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionTitle("ACCOUNT & PROFILE")
        SettingsCard {
            if (editingProfile) {
                SettingsField("Name", fullName) { fullName = it }
                SettingsField("Email", email) { email = it }
                SettingsField("College", college) { college = it }
                SettingsField("Course", course) { course = it }
                SettingsField("Semester", semester) { semester = it }
                SettingsField("CGPA", cgpa) { cgpa = it }
                SettingsField("Skills", skills) { skills = it }
                SettingsField("About", about) { about = it }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { saveProfile() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !savingProfile
                ) {
                    Text(if (savingProfile) "SAVING..." else "SAVE PROFILE")
                }

                OutlinedButton(
                    onClick = { editingProfile = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CANCEL")
                }
            } else {
                SettingsRow("NAME", fullName.ifBlank { "Not set" })
                SettingsRow("EMAIL", email.ifBlank { "Not set" })
                SettingsRow("COLLEGE", college.ifBlank { "Not set" })
                SettingsRow("COURSE", course.ifBlank { "Not set" })
                SettingsRow("SEMESTER", semester.ifBlank { "Not set" })
                SettingsRow("CGPA", cgpa.ifBlank { "Not set" })
                SettingsRow("SKILLS", skills.ifBlank { "Not set" })
                SettingsRow("ABOUT", about.ifBlank { "Not set" })

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { editingProfile = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("EDIT PROFILE")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        SettingsSectionTitle("APPEARANCE")
        SettingsCard {
            Text(
                "Choose your preferred appearance",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("DARK", "LIGHT", "SYSTEM").forEach { option ->
                    OutlinedButton(
                        onClick = {
                            appearance = option
                            PrismAppearanceState.mode = option
                            savePreferences()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            option,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Selected: $appearance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))
        SettingsSectionTitle("NOTIFICATIONS")
        SettingsCard {
            SettingsToggleRow("Notifications", notificationsEnabled) {
                notificationsEnabled = it
                savePreferences()
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsToggleRow("Internship follow-up reminders", followUpReminders) {
                followUpReminders = it
                savePreferences()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Follow-up preference is saved to your account and can be used by the internship reminder system.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        SettingsSectionTitle("PRIVACY & SECURITY")
        SettingsCard {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24242B))
            ) {
                Text(
                    "LOG OUT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !deletingAccount
            ) {
                Text(if (deletingAccount) "DELETING..." else "DELETE ACCOUNT")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        SettingsSectionTitle("EXPORT MY DATA")
        SettingsCard {
            Text(
                "Export your saved PRISMOrbit profile and career data as a text file.",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { exportLauncher.launch("PRISMOrbit_Data_Export.txt") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "EXPORT MY DATA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        SettingsSectionTitle("ABOUT PRISMOrbit")
        SettingsCard {
            SettingsRow("APP", "PRISMOrbit")
            SettingsRow("VERSION", "1.0")
            Text(
                "Your personal academic, development and career workspace.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                message,
                color = if (messageIsError) Color(0xFFFF8A80) else Color(0xFFB9F6CA),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showDeleteConfirmation) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("DELETE ACCOUNT?", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "This permanently removes your PRISMOrbit account data. This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        deleteAllUserDataAndAccount()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                ) { Text("DELETE PERMANENTLY") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("CANCEL") }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        singleLine = label != "About"
    )
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        Text(
            if (checked) "ON" else "OFF",
            color = if (checked) Color(0xFF65E572) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}