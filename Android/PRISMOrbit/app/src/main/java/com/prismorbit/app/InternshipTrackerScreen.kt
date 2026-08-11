package com.prismorbit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.UUID
import kotlin.math.roundToInt

data class InternshipRecord(
    val id: String = UUID.randomUUID().toString(),
    val company: String = "",
    val role: String = "",
    val location: String = "",
    val applicationDate: String = "",
    val interviewDate: String = "",
    val offerDate: String = "",
    val stipend: String = "",
    val status: String = "INTERESTED",
    val jobUrl: String = "",
    val notes: String = "",
    val followUpDate: String = "",
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

private val internshipStatuses = listOf(
    "INTERESTED", "APPLIED", "ASSESSMENT", "INTERVIEW",
    "SELECTED", "REJECTED", "WITHDRAWN"
)

private val internshipPriorities = listOf("HIGH", "MEDIUM", "LOW")

private fun internshipCollection(firestore: FirebaseFirestore, uid: String) =
    firestore.collection("users").document(uid).collection("internships")

private fun InternshipRecord.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "company" to company,
    "role" to role,
    "location" to location,
    "applicationDate" to applicationDate,
    "interviewDate" to interviewDate,
    "offerDate" to offerDate,
    "stipend" to stipend,
    "status" to status,
    "jobUrl" to jobUrl,
    "notes" to notes,
    "followUpDate" to followUpDate,
    "priority" to priority,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

private fun documentToInternshipRecord(
    document: com.google.firebase.firestore.DocumentSnapshot
): InternshipRecord? {
    val data = document.data ?: return null
    val company = data["company"]?.toString().orEmpty()
    if (company.isBlank()) return null

    return InternshipRecord(
        id = data["id"]?.toString()?.ifBlank { document.id } ?: document.id,
        company = company,
        role = data["role"]?.toString().orEmpty(),
        location = data["location"]?.toString().orEmpty(),
        applicationDate = data["applicationDate"]?.toString().orEmpty(),
        interviewDate = data["interviewDate"]?.toString().orEmpty(),
        offerDate = data["offerDate"]?.toString().orEmpty(),
        stipend = data["stipend"]?.toString().orEmpty(),
        status = data["status"]?.toString()?.uppercase()
            ?.takeIf { it in internshipStatuses } ?: "INTERESTED",
        jobUrl = data["jobUrl"]?.toString().orEmpty(),
        notes = data["notes"]?.toString().orEmpty(),
        followUpDate = data["followUpDate"]?.toString().orEmpty(),
        priority = data["priority"]?.toString()?.uppercase()
            ?.takeIf { it in internshipPriorities } ?: "MEDIUM",
        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )
}

private fun loadInternships(
    firestore: FirebaseFirestore,
    uid: String,
    onSuccess: (List<InternshipRecord>) -> Unit,
    onError: (Exception) -> Unit
) {
    internshipCollection(firestore, uid).get()
        .addOnSuccessListener { snapshot ->
            onSuccess(
                snapshot.documents.mapNotNull(::documentToInternshipRecord)
                    .sortedWith(
                        compareBy<InternshipRecord> { it.followUpDate.isBlank() }
                            .thenBy { it.followUpDate }
                            .thenByDescending { it.updatedAt }
                    )
            )
        }
        .addOnFailureListener(onError)
}

private fun saveInternship(
    firestore: FirebaseFirestore,
    uid: String,
    internship: InternshipRecord,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    internshipCollection(firestore, uid).document(internship.id)
        .set(internship.toFirestoreMap(), SetOptions.merge())
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onError)
}

private fun deleteInternship(
    firestore: FirebaseFirestore,
    uid: String,
    internshipId: String,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    internshipCollection(firestore, uid).document(internshipId).delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onError)
}

@Composable
fun InternshipTrackerScreen(
    projectCount: Int,
    onBack: () -> Unit,
    onCountChanged: (Int) -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    var internships by remember { mutableStateOf<List<InternshipRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<InternshipRecord?>(null) }
    var deleting by remember { mutableStateOf<InternshipRecord?>(null) }

    fun refresh() {
        val uid = currentUser?.uid
        if (uid == null) {
            loading = false
            errorMessage = "No signed-in user found."
            return
        }
        loading = true
        errorMessage = ""
        loadInternships(
            firestore,
            uid,
            onSuccess = {
                internships = it
                loading = false
                onCountChanged(it.size)
            },
            onError = {
                loading = false
                errorMessage = it.message ?: "Unable to load internships."
            }
        )
    }

    LaunchedEffect(currentUser?.uid) { refresh() }

    val selected = internships.count { it.status == "SELECTED" }
    val rejected = internships.count { it.status == "REJECTED" }
    val interviews = internships.count { it.status == "INTERVIEW" || it.status == "SELECTED" }
    val active = internships.count {
        it.status == "INTERESTED" || it.status == "APPLIED" ||
                it.status == "ASSESSMENT" || it.status == "INTERVIEW"
    }
    val decided = selected + rejected
    val successRate = if (decided == 0) 0
    else (selected.toFloat() / decided * 100f).roundToInt()
    val profileStrength = calculateInternshipProfileStrength(internships, projectCount)

    Box(Modifier.fillMaxSize().background(Color(0xFF050507))) {
        if (showEditor) {
            InternshipEditor(
                initial = editing,
                saving = saving,
                onBack = { if (!saving) { showEditor = false; editing = null } },
                onSave = { candidate ->
                    val uid = currentUser?.uid
                    if (uid == null) {
                        errorMessage = "No signed-in user found."
                        return@InternshipEditor
                    }
                    saving = true
                    errorMessage = ""
                    saveInternship(
                        firestore,
                        uid,
                        candidate.copy(
                            company = candidate.company.trim(),
                            role = candidate.role.trim(),
                            location = candidate.location.trim(),
                            applicationDate = candidate.applicationDate.trim(),
                            interviewDate = candidate.interviewDate.trim(),
                            offerDate = candidate.offerDate.trim(),
                            stipend = candidate.stipend.trim(),
                            jobUrl = candidate.jobUrl.trim(),
                            notes = candidate.notes.trim(),
                            followUpDate = candidate.followUpDate.trim(),
                            updatedAt = System.currentTimeMillis()
                        ),
                        onSuccess = {
                            saving = false
                            showEditor = false
                            editing = null
                            refresh()
                        },
                        onError = {
                            saving = false
                            errorMessage = it.message ?: "Unable to save internship."
                        }
                    )
                }
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onBack) {
                            Text("‹", color = Color.White, fontSize = 34.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("INTERNSHIPS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                            Text("APPLY • INTERVIEW • GROW", color = Color(0xFFFFD23F), fontSize = 8.sp, letterSpacing = 1.7.sp)
                        }
                        Button(
                            onClick = {
                                editing = null
                                errorMessage = ""
                                showEditor = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB28A2E)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("+ ADD", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF241416))) {
                            Text(errorMessage, color = Color(0xFFFF6B6B), modifier = Modifier.padding(15.dp))
                        }
                    }
                }

                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    item { InternshipProfileCard(profileStrength) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(Modifier.weight(1f), "APPLICATIONS", internships.size.toString())
                            StatCard(Modifier.weight(1f), "ACTIVE", active.toString())
                            StatCard(Modifier.weight(1f), "INTERVIEWS", interviews.toString())
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(Modifier.weight(1f), "SELECTED", selected.toString())
                            StatCard(Modifier.weight(1f), "REJECTED", rejected.toString())
                            StatCard(Modifier.weight(1f), "SUCCESS", "$successRate%")
                        }
                    }
                    item { PipelineCard(internships) }
                    item {
                        Text("YOUR APPLICATIONS", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                    if (internships.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)), shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.padding(20.dp)) {
                                    Text("NO APPLICATIONS YET", color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Tap + ADD above to create your first internship.", color = Color(0xFF777780), fontSize = 11.sp)
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = { editing = null; errorMessage = ""; showEditor = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB28A2E)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) { Text("+ ADD INTERNSHIP", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    } else {
                        items(internships, key = { it.id }) { internship ->
                            InternshipCard(
                                internship = internship,
                                onEdit = { editing = internship; errorMessage = ""; showEditor = true },
                                onDelete = { deleting = internship }
                            )
                        }
                    }
                }
            }
        }

        deleting?.let { internship ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("Delete internship?") },
                text = { Text("Delete ${internship.company} — ${internship.role.ifBlank { "internship" }} permanently?") },
                confirmButton = {
                    TextButton(onClick = {
                        val uid = currentUser?.uid
                        if (uid == null) {
                            errorMessage = "No signed-in user found."
                            deleting = null
                            return@TextButton
                        }
                        deleteInternship(
                            firestore, uid, internship.id,
                            onSuccess = {
                                internships = internships.filterNot { it.id == internship.id }
                                onCountChanged(internships.size)
                                deleting = null
                            },
                            onError = {
                                errorMessage = it.message ?: "Unable to delete internship."
                                deleting = null
                            }
                        )
                    }) { Text("DELETE", color = Color(0xFFFF6B6B)) }
                },
                dismissButton = { TextButton(onClick = { deleting = null }) { Text("CANCEL") } }
            )
        }
    }
}

@Composable
private fun InternshipProfileCard(score: Int) {
    val label = when {
        score >= 85 -> "EXCELLENT"
        score >= 70 -> "STRONG"
        score >= 50 -> "GOOD"
        score >= 30 -> "DEVELOPING"
        else -> "STARTING"
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("INTERNSHIP PROFILE STRENGTH", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("$score / 100", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(label, color = Color(0xFFFFD23F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgress(score / 100f)
        }
    }
}

@Composable
private fun LinearProgress(value: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF25252C), RoundedCornerShape(50.dp))) {
        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(8.dp).background(Color(0xFFFFD23F), RoundedCornerShape(50.dp)))
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, color = Color(0xFF777780), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PipelineCard(internships: List<InternshipRecord>) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(17.dp)) {
            Text("APPLICATION PIPELINE", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(12.dp))
            internshipStatuses.forEach { status ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(status, color = Color.White, fontSize = 10.sp)
                    Text(internships.count { it.status == status }.toString(), color = Color(0xFFFFD23F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InternshipCard(internship: InternshipRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    val statusColor = when (internship.status) {
        "SELECTED" -> Color(0xFF65E572)
        "REJECTED" -> Color(0xFFFF7B72)
        "INTERVIEW" -> Color(0xFF00D9FF)
        else -> Color(0xFFFFD23F)
    }
    val priorityColor = when (internship.priority) {
        "HIGH" -> Color(0xFFFF6B6B)
        "LOW" -> Color(0xFF777780)
        else -> Color(0xFFFFD23F)
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(17.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(internship.company, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(internship.role.ifBlank { "Role not added" }, color = Color(0xFF888891), fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(internship.status, color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(internship.priority, color = priorityColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (internship.applicationDate.isNotBlank()) Text("Applied: ${internship.applicationDate}", color = Color(0xFF777780), fontSize = 9.sp)
            if (internship.location.isNotBlank()) Text("Location: ${internship.location}", color = Color(0xFF777780), fontSize = 9.sp)
            if (internship.stipend.isNotBlank()) Text("Stipend: ${internship.stipend}", color = Color(0xFF777780), fontSize = 9.sp)
            if (internship.interviewDate.isNotBlank()) Text("Interview: ${internship.interviewDate}", color = Color(0xFF00D9FF), fontSize = 9.sp)
            if (internship.offerDate.isNotBlank()) Text("Offer: ${internship.offerDate}", color = Color(0xFF65E572), fontSize = 9.sp)
            if (internship.followUpDate.isNotBlank()) Text("Follow-up: ${internship.followUpDate}", color = Color(0xFFFFD23F), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            if (internship.notes.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(internship.notes, color = Color(0xFF888891), fontSize = 9.sp, lineHeight = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("EDIT", fontSize = 9.sp) }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("DELETE", fontSize = 9.sp) }
            }
        }
    }
}

@Composable
private fun InternshipEditor(initial: InternshipRecord?, saving: Boolean, onBack: () -> Unit, onSave: (InternshipRecord) -> Unit) {
    var company by remember(initial?.id) { mutableStateOf(initial?.company.orEmpty()) }
    var role by remember(initial?.id) { mutableStateOf(initial?.role.orEmpty()) }
    var location by remember(initial?.id) { mutableStateOf(initial?.location.orEmpty()) }
    var applicationDate by remember(initial?.id) { mutableStateOf(initial?.applicationDate.orEmpty()) }
    var interviewDate by remember(initial?.id) { mutableStateOf(initial?.interviewDate.orEmpty()) }
    var offerDate by remember(initial?.id) { mutableStateOf(initial?.offerDate.orEmpty()) }
    var stipend by remember(initial?.id) { mutableStateOf(initial?.stipend.orEmpty()) }
    var status by remember(initial?.id) { mutableStateOf(initial?.status ?: "INTERESTED") }
    var priority by remember(initial?.id) { mutableStateOf(initial?.priority ?: "MEDIUM") }
    var jobUrl by remember(initial?.id) { mutableStateOf(initial?.jobUrl.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var followUpDate by remember(initial?.id) { mutableStateOf(initial?.followUpDate.orEmpty()) }
    var error by remember(initial?.id) { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFF050507)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, enabled = !saving) { Text("‹", color = Color.White, fontSize = 34.sp) }
                Column {
                    Text(if (initial == null) "ADD INTERNSHIP" else "EDIT INTERNSHIP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("INTERNSHIP TRACKER", color = Color(0xFFFFD23F), fontSize = 8.sp, letterSpacing = 1.5.sp)
                }
            }
        }
        item { Field("Company", company) { company = it; error = "" } }
        item { Field("Role", role, "Android Intern, SDE Intern...") { role = it } }
        item { Field("Location / Remote", location) { location = it } }
        item { Field("Application Date", applicationDate, "DD/MM/YYYY") { applicationDate = it } }
        item { Field("Interview Date", interviewDate, "DD/MM/YYYY") { interviewDate = it } }
        item { Field("Offer Date", offerDate, "DD/MM/YYYY") { offerDate = it } }
        item { Field("Follow-up Date", followUpDate, "DD/MM/YYYY") { followUpDate = it } }
        item { Field("Stipend", stipend) { stipend = it } }
        item { Text("APPLICATION STATUS", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        items(internshipStatuses) { option -> ChoiceRow(option, status == option) { status = option } }
        item { Text("PRIORITY", color = Color(0xFF888891), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        items(internshipPriorities) { option -> ChoiceRow(option, priority == option) { priority = option } }
        item { Field("Job / Application URL", jobUrl) { jobUrl = it } }
        item {
            OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Notes") }, placeholder = { Text("Rounds, requirements, preparation notes...") }, minLines = 4)
        }
        if (error.isNotBlank()) item { Text(error, color = Color(0xFFFF6B6B), fontSize = 10.sp) }
        item {
            Button(
                onClick = {
                    if (company.trim().isBlank()) {
                        error = "Please enter a company name."
                    } else {
                        onSave(
                            InternshipRecord(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                company = company.trim(), role = role.trim(), location = location.trim(),
                                applicationDate = applicationDate.trim(), interviewDate = interviewDate.trim(),
                                offerDate = offerDate.trim(), stipend = stipend.trim(), status = status,
                                jobUrl = jobUrl.trim(), notes = notes.trim(), followUpDate = followUpDate.trim(),
                                priority = priority, createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB28A2E)),
                shape = RoundedCornerShape(16.dp)
            ) { Text(if (saving) "SAVING..." else "SAVE INTERNSHIP", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Field(label: String, value: String, placeholder: String = "", onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { if (placeholder.isNotBlank()) Text(placeholder) }, singleLine = true)
}

@Composable
private fun ChoiceRow(text: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) Color(0xFF211A2D) else Color.Transparent)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text, color = if (selected) Color(0xFFFFD23F) else Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) Text("✓", color = Color(0xFF65E572))
        }
    }
}

private fun calculateInternshipProfileStrength(internships: List<InternshipRecord>, projectCount: Int): Int {
    if (internships.isEmpty() && projectCount == 0) return 0
    val applicationActivity = (internships.size / 10f).coerceIn(0f, 1f) * 25f
    val interviewExposure = (internships.count { it.status == "INTERVIEW" || it.status == "SELECTED" } / 5f).coerceIn(0f, 1f) * 20f
    val outcomeExperience = (internships.count { it.status == "SELECTED" } / 2f).coerceIn(0f, 1f) * 20f
    val projectFoundation = (projectCount / 4f).coerceIn(0f, 1f) * 20f
    val completeProfiles = internships.count { it.company.isNotBlank() && it.role.isNotBlank() && it.applicationDate.isNotBlank() && it.jobUrl.isNotBlank() }
    val profileCompleteness = if (internships.isEmpty()) 0f else completeProfiles.toFloat() / internships.size * 15f
    return (applicationActivity + interviewExposure + outcomeExperience + projectFoundation + profileCompleteness).roundToInt().coerceIn(0, 100)
}