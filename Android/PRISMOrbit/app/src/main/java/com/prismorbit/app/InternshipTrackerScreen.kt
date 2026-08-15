package com.prismorbit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
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
    val duration: String = "",
    val technicalSkillsUsed: String = "",
    val responsibilities: String = "",
    val problemSolved: String = "",
    val outcomeImpact: String = "",
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
    "INTERESTED",
    "APPLIED",
    "ASSESSMENT",
    "INTERVIEW",
    "SELECTED",
    "REJECTED",
    "WITHDRAWN"
)

private val internshipPriorities = listOf(
    "HIGH",
    "MEDIUM",
    "LOW"
)

private fun internshipCollection(
    firestore: FirebaseFirestore,
    uid: String
) = firestore
    .collection("users")
    .document(uid)
    .collection("internships")

private fun InternshipRecord.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "company" to company,
    "role" to role,
    "duration" to duration,
    "technicalSkillsUsed" to technicalSkillsUsed,
    "responsibilities" to responsibilities,
    "problemSolved" to problemSolved,
    "outcomeImpact" to outcomeImpact,
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

    return InternshipRecord(
        id = data["id"]?.toString()?.ifBlank { document.id } ?: document.id,
        company = data["company"]?.toString().orEmpty(),
        role = data["role"]?.toString().orEmpty(),
        duration = data["duration"]?.toString().orEmpty(),
        technicalSkillsUsed = data["technicalSkillsUsed"]?.toString().orEmpty(),
        responsibilities = data["responsibilities"]?.toString().orEmpty(),
        problemSolved = data["problemSolved"]?.toString().orEmpty(),
        outcomeImpact = data["outcomeImpact"]?.toString().orEmpty(),
        location = data["location"]?.toString().orEmpty(),
        applicationDate = data["applicationDate"]?.toString().orEmpty(),
        interviewDate = data["interviewDate"]?.toString().orEmpty(),
        offerDate = data["offerDate"]?.toString().orEmpty(),
        stipend = data["stipend"]?.toString().orEmpty(),
        status = data["status"]?.toString()
            ?.uppercase()
            ?.takeIf { it in internshipStatuses }
            ?: "INTERESTED",
        jobUrl = data["jobUrl"]?.toString().orEmpty(),
        notes = data["notes"]?.toString().orEmpty(),
        followUpDate = data["followUpDate"]?.toString().orEmpty(),
        priority = data["priority"]?.toString()
            ?.uppercase()
            ?.takeIf { it in internshipPriorities }
            ?: "MEDIUM",
        createdAt = (data["createdAt"] as? Number)?.toLong()
            ?: System.currentTimeMillis(),
        updatedAt = (data["updatedAt"] as? Number)?.toLong()
            ?: System.currentTimeMillis()
    ).takeIf { it.company.isNotBlank() }
}

private fun loadInternships(
    firestore: FirebaseFirestore,
    uid: String,
    onSuccess: (List<InternshipRecord>) -> Unit,
    onError: (Exception) -> Unit
) {
    internshipCollection(firestore, uid)
        .get()
        .addOnSuccessListener { snapshot ->
            val loaded = snapshot.documents
                .mapNotNull(::documentToInternshipRecord)
                .sortedWith(
                    compareBy<InternshipRecord> {
                        it.followUpDate.isBlank()
                    }.thenBy {
                        it.followUpDate
                    }.thenByDescending {
                        it.updatedAt
                    }
                )

            onSuccess(loaded)
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
    internshipCollection(firestore, uid)
        .document(internship.id)
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
    internshipCollection(firestore, uid)
        .document(internshipId)
        .delete()
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
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var editorProject by remember { mutableStateOf<InternshipRecord?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<InternshipRecord?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun refresh() {
        val uid = currentUser?.uid
        if (uid == null) {
            isLoading = false
            errorMessage = "No signed-in user found."
            return
        }

        isLoading = true
        errorMessage = ""

        loadInternships(
            firestore = firestore,
            uid = uid,
            onSuccess = {
                internships = it
                isLoading = false
                onCountChanged(it.size)
            },
            onError = {
                isLoading = false
                errorMessage = it.message ?: "Unable to load internships."
            }
        )
    }

    LaunchedEffect(currentUser?.uid) {
        refresh()
    }

    val selected = internships.count { it.status == "SELECTED" }
    val rejected = internships.count { it.status == "REJECTED" }
    val interviews = internships.count {
        it.status == "INTERVIEW" || it.status == "SELECTED"
    }
    val active = internships.count {
        it.status == "INTERESTED" ||
                it.status == "APPLIED" ||
                it.status == "ASSESSMENT" ||
                it.status == "INTERVIEW"
    }
    val decided = selected + rejected
    val successRate = if (decided == 0) 0
    else (selected.toFloat() / decided.toFloat() * 100f).roundToInt()

    val profileStrength = calculateInternshipProfileStrength(
        internships = internships,
        projectCount = projectCount
    )

    val upcomingFollowUps = internships
        .filter { it.followUpDate.isNotBlank() }
        .sortedBy { it.followUpDate }
        .take(5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showEditor) {
            InternshipEditor(
                initial = editorProject,
                saving = saving,
                onBack = {
                    if (!saving) {
                        showEditor = false
                        editorProject = null
                    }
                },
                onSave = { candidate ->
                    val uid = currentUser?.uid
                    if (uid == null) {
                        errorMessage = "No signed-in user found."
                        return@InternshipEditor
                    }

                    saving = true
                    errorMessage = ""

                    saveInternship(
                        firestore = firestore,
                        uid = uid,
                        internship = candidate.copy(
                            company = candidate.company.trim(),
                            role = candidate.role.trim(),
                            duration = candidate.duration.trim(),
                            technicalSkillsUsed = candidate.technicalSkillsUsed.trim(),
                            responsibilities = candidate.responsibilities.trim(),
                            problemSolved = candidate.problemSolved.trim(),
                            outcomeImpact = candidate.outcomeImpact.trim(),
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
                            editorProject = null
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 18.dp,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onBack) {
                            Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 34.sp)
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                "INTERNSHIPS",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "APPLY • INTERVIEW • GROW",
                                color = Color(0xFFFFD23F),
                                fontSize = 8.sp,
                                letterSpacing = 1.7.sp
                            )
                        }

                        TextButton(
                            onClick = {
                                editorProject = null
                                errorMessage = ""
                                showEditor = true
                            }
                        ) {
                            Text(
                                "+ ADD",
                                color = Color(0xFFFFD23F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF241416)
                            )
                        ) {
                            Text(
                                errorMessage,
                                color = Color(0xFFFF6B6B),
                                modifier = Modifier.padding(15.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    item {
                        InternshipProfileCard(profileStrength)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                Modifier.weight(1f),
                                "APPLICATIONS",
                                internships.size.toString()
                            )
                            StatCard(
                                Modifier.weight(1f),
                                "ACTIVE",
                                active.toString()
                            )
                            StatCard(
                                Modifier.weight(1f),
                                "INTERVIEWS",
                                interviews.toString()
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                Modifier.weight(1f),
                                "SELECTED",
                                selected.toString()
                            )
                            StatCard(
                                Modifier.weight(1f),
                                "REJECTED",
                                rejected.toString()
                            )
                            StatCard(
                                Modifier.weight(1f),
                                "SUCCESS",
                                "$successRate%"
                            )
                        }
                    }

                    item {
                        PipelineCard(internships)
                    }

                    if (upcomingFollowUps.isNotEmpty()) {
                        item {
                            FollowUpCard(upcomingFollowUps)
                        }
                    }

                    item {
                        Text(
                            "YOUR APPLICATIONS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    if (internships.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Text(
                                        "NO APPLICATIONS YET",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Add your first internship application.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = internships,
                            key = { it.id }
                        ) { internship ->
                            InternshipCard(
                                internship = internship,
                                onEdit = {
                                    editorProject = internship
                                    errorMessage = ""
                                    showEditor = true
                                },
                                onDelete = {
                                    deleting = internship
                                }
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
                text = {
                    Text(
                        "Delete ${internship.company} — ${internship.role.ifBlank { "internship" }} permanently?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val uid = currentUser?.uid
                            if (uid == null) {
                                errorMessage = "No signed-in user found."
                                deleting = null
                                return@TextButton
                            }

                            deleteInternship(
                                firestore = firestore,
                                uid = uid,
                                internshipId = internship.id,
                                onSuccess = {
                                    internships = internships.filterNot {
                                        it.id == internship.id
                                    }
                                    onCountChanged(internships.size)
                                    deleting = null
                                },
                                onError = {
                                    errorMessage =
                                        it.message ?: "Unable to delete internship."
                                    deleting = null
                                }
                            )
                        }
                    ) {
                        Text("DELETE", color = Color(0xFFFF6B6B))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleting = null }) {
                        Text("CANCEL")
                    }
                }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "INTERNSHIP PROFILE STRENGTH",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(7.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "$score / 100",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    label,
                    color = Color(0xFFFFD23F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgress(score / 100f)
        }
    }
}

@Composable
private fun LinearProgress(value: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                Color(0xFF25252C),
                RoundedCornerShape(50.dp)
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .height(8.dp)
                .background(
                    Color(0xFFFFD23F),
                    RoundedCornerShape(50.dp)
                )
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PipelineCard(
    internships: List<InternshipRecord>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(
                "APPLICATION PIPELINE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )

            Spacer(Modifier.height(12.dp))

            internshipStatuses.forEach { status ->
                val count = internships.count { it.status == status }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        status,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp
                    )
                    Text(
                        count.toString(),
                        color = Color(0xFFFFD23F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowUpCard(
    internships: List<InternshipRecord>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(
                "UPCOMING FOLLOW-UPS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )

            Spacer(Modifier.height(8.dp))

            internships.forEach {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${it.company} • ${it.status}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        it.followUpDate,
                        color = Color(0xFFFFD23F),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InternshipCard(
    internship: InternshipRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (internship.status) {
        "SELECTED" -> Color(0xFF65E572)
        "REJECTED" -> Color(0xFFFF7B72)
        "INTERVIEW" -> Color(0xFF00D9FF)
        else -> Color(0xFFFFD23F)
    }

    val priorityColor = when (internship.priority) {
        "HIGH" -> Color(0xFFFF6B6B)
        "LOW" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color(0xFFFFD23F)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        internship.company,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        internship.role.ifBlank { "Role not added" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        internship.status,
                        color = statusColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        internship.priority,
                        color = priorityColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (internship.applicationDate.isNotBlank()) {
                Text(
                    "Applied: ${internship.applicationDate}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }

            if (internship.duration.isNotBlank()) {
                Text(
                    "Duration: ${internship.duration}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }

            if (internship.technicalSkillsUsed.isNotBlank()) {
                Text(
                    "Technical Skills: ${internship.technicalSkillsUsed}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (internship.responsibilities.isNotBlank()) {
                Text(
                    "Work Done: ${internship.responsibilities}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (internship.problemSolved.isNotBlank()) {
                Text(
                    "Problem Solved: ${internship.problemSolved}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (internship.outcomeImpact.isNotBlank()) {
                Text(
                    "Outcome / Impact: ${internship.outcomeImpact}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "Stipend: ${internship.stipend}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }

        if (internship.interviewDate.isNotBlank()) {
            Text(
                "Interview: ${internship.interviewDate}",
                color = Color(0xFF00D9FF),
                fontSize = 9.sp
            )
        }

        if (internship.offerDate.isNotBlank()) {
            Text(
                "Offer: ${internship.offerDate}",
                color = Color(0xFF65E572),
                fontSize = 9.sp
            )
        }

        if (internship.followUpDate.isNotBlank()) {
            Text(
                "Follow-up: ${internship.followUpDate}",
                color = Color(0xFFFFD23F),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (internship.notes.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(
                internship.notes,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("EDIT", fontSize = 9.sp)
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("DELETE", fontSize = 9.sp)
            }
        }
    }
}


@Composable
private fun InternshipEditor(
    initial: InternshipRecord?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (InternshipRecord) -> Unit
) {
    var company by remember(initial?.id) {
        mutableStateOf(initial?.company.orEmpty())
    }
    var role by remember(initial?.id) {
        mutableStateOf(initial?.role.orEmpty())
    }
    var duration by remember(initial?.id) {
        mutableStateOf(initial?.duration.orEmpty())
    }
    var technicalSkillsUsed by remember(initial?.id) {
        mutableStateOf(initial?.technicalSkillsUsed.orEmpty())
    }
    var responsibilities by remember(initial?.id) {
        mutableStateOf(initial?.responsibilities.orEmpty())
    }
    var problemSolved by remember(initial?.id) {
        mutableStateOf(initial?.problemSolved.orEmpty())
    }
    var outcomeImpact by remember(initial?.id) {
        mutableStateOf(initial?.outcomeImpact.orEmpty())
    }
    var location by remember(initial?.id) {
        mutableStateOf(initial?.location.orEmpty())
    }
    var applicationDate by remember(initial?.id) {
        mutableStateOf(initial?.applicationDate.orEmpty())
    }
    var interviewDate by remember(initial?.id) {
        mutableStateOf(initial?.interviewDate.orEmpty())
    }
    var offerDate by remember(initial?.id) {
        mutableStateOf(initial?.offerDate.orEmpty())
    }
    var stipend by remember(initial?.id) {
        mutableStateOf(initial?.stipend.orEmpty())
    }
    var status by remember(initial?.id) {
        mutableStateOf(initial?.status ?: "INTERESTED")
    }
    var priority by remember(initial?.id) {
        mutableStateOf(initial?.priority ?: "MEDIUM")
    }
    var jobUrl by remember(initial?.id) {
        mutableStateOf(initial?.jobUrl.orEmpty())
    }
    var notes by remember(initial?.id) {
        mutableStateOf(initial?.notes.orEmpty())
    }
    var followUpDate by remember(initial?.id) {
        mutableStateOf(initial?.followUpDate.orEmpty())
    }
    var error by remember(initial?.id) {
        mutableStateOf("")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    enabled = !saving
                ) {
                    Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 34.sp)
                }

                Column {
                    Text(
                        if (initial == null) "ADD INTERNSHIP"
                        else "EDIT INTERNSHIP",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "INTERNSHIP TRACKER",
                        color = Color(0xFFFFD23F),
                        fontSize = 8.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = company,
                onValueChange = {
                    company = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Company") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Role") },
                placeholder = { Text("Android Intern, SDE Intern...") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Duration") },
                placeholder = { Text("3 months, 6 months...") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = technicalSkillsUsed,
                onValueChange = { technicalSkillsUsed = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Technical Skills Used") },
                placeholder = { Text("Kotlin, Firebase, REST APIs...") },
                minLines = 2
            )
        }

        item {
            OutlinedTextField(
                value = responsibilities,
                onValueChange = { responsibilities = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Responsibilities / Work Done") },
                placeholder = { Text("What did you actually work on?") },
                minLines = 3
            )
        }

        item {
            OutlinedTextField(
                value = problemSolved,
                onValueChange = { problemSolved = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Problem Solved") },
                placeholder = { Text("What problem or challenge did you solve?") },
                minLines = 3
            )
        }

        item {
            OutlinedTextField(
                value = outcomeImpact,
                onValueChange = { outcomeImpact = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Outcome / Impact") },
                placeholder = { Text("What was the result or impact?") },
                minLines = 3
            )
        }

        item {
            OutlinedTextField(
                value = applicationDate,
                onValueChange = { applicationDate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Application Date") },
                placeholder = { Text("DD/MM/YYYY") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = interviewDate,
                onValueChange = { interviewDate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Interview Date") },
                placeholder = { Text("DD/MM/YYYY") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = offerDate,
                onValueChange = { offerDate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Offer Date") },
                placeholder = { Text("DD/MM/YYYY") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = followUpDate,
                onValueChange = { followUpDate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Follow-up Date") },
                placeholder = { Text("DD/MM/YYYY") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = stipend,
                onValueChange = { stipend = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Stipend") },
                singleLine = true
            )
        }

        item {
            Text(
                "APPLICATION STATUS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(internshipStatuses) { option ->
            ChoiceRow(
                text = option,
                selected = status == option,
                onClick = { status = option }
            )
        }

        item {
            Spacer(Modifier.height(5.dp))
            Text(
                "PRIORITY",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(internshipPriorities) { option ->
            ChoiceRow(
                text = option,
                selected = priority == option,
                onClick = { priority = option }
            )
        }

        item {
            OutlinedTextField(
                value = jobUrl,
                onValueChange = { jobUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Job / Application URL") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                placeholder = {
                    Text("Rounds, requirements, preparation notes...")
                },
                minLines = 4
            )
        }

        if (error.isNotBlank()) {
            item {
                Text(
                    error,
                    color = Color(0xFFFF6B6B),
                    fontSize = 10.sp
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (company.trim().isBlank()) {
                        error = "Please enter a company name."
                        return@Button
                    }

                    onSave(
                        InternshipRecord(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            company = company.trim(),
                            role = role.trim(),
                            duration = duration.trim(),
                            technicalSkillsUsed = technicalSkillsUsed.trim(),
                            responsibilities = responsibilities.trim(),
                            problemSolved = problemSolved.trim(),
                            outcomeImpact = outcomeImpact.trim(),
                            location = location.trim(),
                            applicationDate = applicationDate.trim(),
                            interviewDate = interviewDate.trim(),
                            offerDate = offerDate.trim(),
                            stipend = stipend.trim(),
                            status = status,
                            jobUrl = jobUrl.trim(),
                            notes = notes.trim(),
                            followUpDate = followUpDate.trim(),
                            priority = priority,
                            createdAt = initial?.createdAt
                                ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB28A2E)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (saving) "SAVING..." else "SAVE INTERNSHIP",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFF211A2D)
            else Color.Transparent
        )
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text,
                color = if (selected) Color(0xFFFFD23F)
                else Color.White,
                fontWeight = if (selected) FontWeight.Bold
                else FontWeight.Normal
            )
            if (selected) {
                Text("✓", color = Color(0xFF65E572))
            }
        }
    }
}

private fun calculateInternshipProfileStrength(
    internships: List<InternshipRecord>,
    projectCount: Int
): Int {
    // No internship records means there is no internship evidence.
    // Projects must not create a non-zero internship score.
    if (internships.isEmpty()) return 0

    val applicationActivity =
        (internships.size / 10f).coerceIn(0f, 1f) * 25f

    val interviewExposure =
        (
                internships.count {
                    it.status == "INTERVIEW" || it.status == "SELECTED"
                } / 5f
                ).coerceIn(0f, 1f) * 20f

    val outcomeExperience =
        (internships.count { it.status == "SELECTED" } / 2f)
            .coerceIn(0f, 1f) * 20f

    val projectFoundation =
        (projectCount / 4f).coerceIn(0f, 1f) * 20f

    val completeProfiles = internships.count {
        it.company.isNotBlank() &&
                it.role.isNotBlank() &&
                it.applicationDate.isNotBlank() &&
                it.jobUrl.isNotBlank()
    }

    val profileCompleteness =
        if (internships.isEmpty()) 0f
        else (completeProfiles.toFloat() / internships.size) * 15f

    return (
            applicationActivity +
                    interviewExposure +
                    outcomeExperience +
                    projectFoundation +
                    profileCompleteness
            )
        .roundToInt()
        .coerceIn(0, 100)
}