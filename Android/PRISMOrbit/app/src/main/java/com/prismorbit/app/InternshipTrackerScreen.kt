package com.prismorbit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
    val updatedAt: Long = System.currentTimeMillis(),

    // ---- new optional details
    val companyType: String = "",
    val workMode: String = "",
    val proofLink: String = "",
    /** Small proof result (status, matches, tiny thumbnail). Never the file or its text. */
    val proof: Map<String, Any?> = emptyMap(),

    // ---- level (missing = Level 3, worked out later in the background)
    val levelParams: Map<String, Double> = emptyMap(),
    val levelScore: Int? = null,
    val levelFinal: Int? = null,
    val levelReason: String = "",
    val levelSource: String = "",
    val levelHash: String = "",
    val levelUpdatedAt: Long = 0L,
    val levelAiScores: Map<String, Double> = emptyMap(),
    val levelAiSummary: String = ""
) {
    val hasLevel: Boolean get() = levelFinal != null

    /** Missing level counts as Level 3 (factor 1.00). */
    val level: Int get() = (levelFinal ?: PrismScoring.DEFAULT_LEVEL).coerceIn(1, 5)

    val levelName: String get() = PrismScoring.internshipLevelName(level)

    val proofSummary: ProofSummary get() = ProofStorage.summaryOf(proof)

    fun toLevelInput() = InternshipLevelInput(
        company = company,
        companyType = companyType,
        role = role,
        duration = duration,
        technicalSkillsUsed = technicalSkillsUsed,
        responsibilities = responsibilities,
        problemSolved = problemSolved,
        outcomeImpact = outcomeImpact,
        interviewDate = interviewDate,
        offerDate = offerDate,
        stipend = stipend,
        status = status,
        workMode = workMode,
        proof = proofSummary,
        hasProofLink = proofLink.isNotBlank()
    )

    fun toScoreInput() = PrismScoring.InternshipScoreInput(
        status = status,
        detailsComplete = PrismScoring.internshipDetailsComplete(
            company = company,
            role = role,
            applicationDate = applicationDate,
            jobUrl = jobUrl
        ),
        level = level
    )

    /** Takes only the fields the background job owns, so a newer edit is never undone. */
    fun withBackgroundFrom(other: InternshipRecord): InternshipRecord = copy(
        levelParams = other.levelParams,
        levelScore = other.levelScore,
        levelFinal = other.levelFinal,
        levelReason = other.levelReason,
        levelSource = other.levelSource,
        levelHash = other.levelHash,
        levelUpdatedAt = other.levelUpdatedAt,
        levelAiScores = other.levelAiScores,
        levelAiSummary = other.levelAiSummary
    )
}

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

private fun InternshipRecord.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("id", id)
    put("company", company)
    put("role", role)
    put("duration", duration)
    put("technicalSkillsUsed", technicalSkillsUsed)
    put("responsibilities", responsibilities)
    put("problemSolved", problemSolved)
    put("outcomeImpact", outcomeImpact)
    put("location", location)
    put("applicationDate", applicationDate)
    put("interviewDate", interviewDate)
    put("offerDate", offerDate)
    put("stipend", stipend)
    put("status", status)
    put("jobUrl", jobUrl)
    put("notes", notes)
    put("followUpDate", followUpDate)
    put("priority", priority)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("companyType", companyType)
    put("workMode", workMode)
    put("proofLink", proofLink)
    // An empty proof means the student removed it, so the old one is deleted.
    put("proof", if (proof.isNotEmpty()) proof else FieldValue.delete())
    if (levelFinal != null) putAll(levelFieldsMap())
}

/** The fields the background job owns. Written with update(). */
internal fun InternshipRecord.levelFieldsMap(): Map<String, Any?> = buildMap {
    levelFinal?.let { final ->
        put("levelParams", levelParams)
        put("levelScore", levelScore ?: 0)
        put("levelFinal", final)
        put("levelReason", levelReason)
        put("levelSource", levelSource)
        put("levelHash", levelHash)
        put("levelUpdatedAt", levelUpdatedAt)
        put("levelAiScores", levelAiScores)
        put("levelAiSummary", levelAiSummary)
    }
}

private fun doubleMap(value: Any?): Map<String, Double> =
    PrismScoreLoader.stringMap(value)
        ?.mapNotNull { (key, v) -> PrismScoreLoader.number(v)?.let { key to it } }
        ?.toMap()
        ?: emptyMap()

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
            ?: System.currentTimeMillis(),
        companyType = data["companyType"]?.toString()
            ?.uppercase()
            ?.takeIf { CompanyMatcher.CompanyType.fromName(it) != null }
            .orEmpty(),
        workMode = data["workMode"]?.toString()
            ?.uppercase()
            ?.takeIf { value -> WorkMode.ALL.any { it.first == value } }
            .orEmpty(),
        proofLink = data["proofLink"]?.toString().orEmpty(),
        proof = PrismScoreLoader.stringMap(data["proof"]) ?: emptyMap(),
        levelParams = doubleMap(data["levelParams"]),
        levelScore = PrismScoreLoader.number(data["levelScore"])?.toInt(),
        levelFinal = PrismScoreLoader.number(data["levelFinal"])?.toInt()?.coerceIn(1, 5),
        levelReason = data["levelReason"]?.toString().orEmpty(),
        levelSource = data["levelSource"]?.toString().orEmpty(),
        levelHash = data["levelHash"]?.toString().orEmpty(),
        levelUpdatedAt = PrismScoreLoader.number(data["levelUpdatedAt"])?.toLong() ?: 0L,
        levelAiScores = doubleMap(data["levelAiScores"]),
        levelAiSummary = data["levelAiSummary"]?.toString().orEmpty()
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val busyIds = remember { mutableStateListOf<String>() }
    var showKeySetup by remember { mutableStateOf(false) }
    var infoFor by remember { mutableStateOf<InternshipRecord?>(null) }

    // Refreshes right after the key is saved or removed on the setup screen.
    val refreshVersion = AiKeyRefreshSignal.version
    val hasAiKey = remember(currentUser?.uid, refreshVersion, showKeySetup) {
        currentUser?.uid != null && LevelAiRunner.hasKey(context, currentUser.uid)
    }

    // Level work runs in the background after saving. It never blocks the student.
    fun runPipeline(record: InternshipRecord, runAi: Boolean, forceAi: Boolean = false) {
        val uid = currentUser?.uid ?: return
        busyIds.add(record.id)
        scope.launch {
            try {
                val enriched = InternshipPipeline.process(context, firestore, uid, record, runAi, forceAi)
                internships = internships.map {
                    if (it.id == record.id) it.withBackgroundFrom(enriched) else it
                }
            } finally {
                busyIds.remove(record.id)
            }
        }
    }

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
            onSuccess = { loaded ->
                internships = loaded
                isLoading = false
                onCountChanged(loaded.size)

                // Old internships get a rules-only level once, in the background.
                val missingLevel = loaded.filter { !it.hasLevel }
                if (missingLevel.isNotEmpty()) {
                    scope.launch {
                        for (old in missingLevel) {
                            val enriched = InternshipPipeline.process(
                                context, firestore, uid, old, runAi = false
                            )
                            internships = internships.map {
                                if (it.id == old.id) it.withBackgroundFrom(enriched) else it
                            }
                        }
                    }
                }
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
                hasAiKey = hasAiKey,
                onOpenKeySetup = { showKeySetup = true },
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

                    val trimmed = candidate.copy(
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
                            proofLink = candidate.proofLink.trim(),
                            updatedAt = System.currentTimeMillis()
                        )

                    saveInternship(
                        firestore = firestore,
                        uid = uid,
                        internship = trimmed,
                        onSuccess = {
                            saving = false
                            showEditor = false
                            editorProject = null
                            refresh()
                            // Level (and AI, only if the text changed) in the background.
                            runPipeline(trimmed, runAi = true)
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
                    top = 20.dp,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(13.dp)
                                )
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "‹",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "INTERNSHIPS",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "APPLY • INTERVIEW • GROW",
                                color = Color(0xFFFFD23F),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.7.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFF2A2417),
                                    RoundedCornerShape(11.dp)
                                )
                                .clickable {
                                    editorProject = null
                                    errorMessage = ""
                                    showEditor = true
                                }
                                .padding(horizontal = 11.dp, vertical = 9.dp)
                        ) {
                            Text(
                                "+ ADD",
                                color = Color(0xFFFFD23F),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF241416)
                            )
                        ) {
                            Text(
                                errorMessage,
                                color = Color(0xFFFF6B6B),
                                fontSize = 10.sp,
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
                                .height(190.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFD23F)
                            )
                        }
                    }
                } else {
                    item {
                        InternshipProfileCard(profileStrength)
                    }

                    item {
                        Text(
                            "APPLICATION OVERVIEW",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "YOUR APPLICATIONS",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "Track every opportunity from interest to outcome.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                            }

                            Text(
                                "${internships.size} TOTAL",
                                color = Color(0xFFFFD23F),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.9.sp
                            )
                        }
                    }

                    if (internships.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Text(
                                        "NO APPLICATIONS YET",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.7.sp
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    Text(
                                        "Add your first internship application to start building your career pipeline.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        lineHeight = 15.sp
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
                                busy = internship.id in busyIds,
                                hasAiKey = hasAiKey,
                                onOpenKeySetup = { showKeySetup = true },
                                onInfo = { infoFor = internship },
                                onRecheckLevel = { runPipeline(internship, runAi = false, forceAi = true) },
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

        // The existing key setup screen opens on top, so a half-filled form is not lost.
        if (showKeySetup) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                SmartAiApiKeySetupScreen(onBack = { showKeySetup = false })
            }
        }

        infoFor?.let { record ->
            HowLevelsWorkDialog(
                title = "How internship levels work",
                definitions = InternshipLevelEngine.PARAMETERS,
                result = InternshipLevelEngine.compute(
                    record.toLevelInput(),
                    aiScores = record.levelAiScores.takeIf { it.isNotEmpty() && record.levelSource == "ai+rules" },
                    aiSummary = record.levelAiSummary.takeIf { it.isNotBlank() }
                ),
                subject = "internship",
                levelNames = PrismScoring.INTERNSHIP_LEVEL_NAMES,
                extraNote = "The level counts fully only when you are SELECTED. For INTERESTED, APPLIED " +
                        "and INTERVIEW only ${(PrismScoring.INTERNSHIP_UNSELECTED_GATE * 100).toInt()}% of the level counts. " +
                        "We check that your proof matches what you typed. Proof raises your level a little. " +
                        "Money amounts are never judged or shown.",
                onDismiss = { infoFor = null }
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "INTERNSHIP PROFILE",
                        color = Color(0xFFFFD23F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.7.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Strength of your internship evidence",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Text(
                    label,
                    color = Color(0xFFFFD23F),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "$score",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "/ 100",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(Modifier.height(9.dp))
            InternshipProgressBar(score / 100f)
        }
    }
}

@Composable
private fun InternshipProgressBar(value: Float) {
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
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
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
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "APPLICATION PIPELINE",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Where your current applications stand.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }

                Text(
                    internships.size.toString(),
                    color = Color(0xFFFFD23F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(13.dp))

            internshipStatuses.forEach { status ->
                val count = internships.count { it.status == status }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                when (status) {
                                    "SELECTED" -> Color(0xFF65E572)
                                    "REJECTED" -> Color(0xFFFF7B72)
                                    "INTERVIEW" -> Color(0xFF00D9FF)
                                    else -> Color(0xFFFFD23F)
                                },
                                RoundedCornerShape(50.dp)
                            )
                    )

                    Spacer(Modifier.width(9.dp))

                    Text(
                        status,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        count.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
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
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "UPCOMING FOLLOW-UPS",
                color = Color(0xFFFFD23F),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Keep the next action visible.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )

            Spacer(Modifier.height(9.dp))

            internships.forEach {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            it.company,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            it.status,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                    }

                    Text(
                        it.followUpDate,
                        color = Color(0xFFFFD23F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InternshipCard(
    internship: InternshipRecord,
    busy: Boolean,
    hasAiKey: Boolean,
    onOpenKeySetup: () -> Unit,
    onInfo: () -> Unit,
    onRecheckLevel: () -> Unit,
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
        shape = RoundedCornerShape(21.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        internship.company,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
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
                    Box(
                        modifier = Modifier
                            .background(
                                statusColor.copy(alpha = 0.12f),
                                RoundedCornerShape(9.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            internship.status,
                            color = statusColor,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    Text(
                        internship.priority,
                        color = priorityColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
            }

            // ---- Level
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBadge(internship.level, internship.levelName)
                Spacer(Modifier.width(8.dp))
                InfoButton(onInfo)
            }
            Spacer(Modifier.height(6.dp))
            if (!internship.hasLevel) {
                Text(
                    if (busy) "Working out the level..." else "Level 3 for now. The level is worked out in the background.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            } else {
                if (internship.levelReason.isNotBlank()) {
                    Text(internship.levelReason, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    levelImpactText(internship.level, "internship", PrismScoring.internshipGate(internship.status)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))

            if (internship.applicationDate.isNotBlank()) {
                InternshipDetailLine("Applied", internship.applicationDate)
            }

            CompanyMatcher.CompanyType.fromName(internship.companyType)?.let {
                InternshipDetailLine("Company type", it.label)
            }

            WorkMode.ALL.firstOrNull { it.first == internship.workMode }?.let {
                InternshipDetailLine("Work place", it.second)
            }

            if (internship.duration.isNotBlank()) {
                InternshipDetailLine("Duration", internship.duration)
            }

            if (internship.technicalSkillsUsed.isNotBlank()) {
                InternshipDetailLine(
                    "Technical Skills",
                    internship.technicalSkillsUsed,
                    maxLines = 2
                )
            }

            if (internship.responsibilities.isNotBlank()) {
                InternshipDetailLine(
                    "Work Done",
                    internship.responsibilities,
                    maxLines = 3
                )
            }

            if (internship.problemSolved.isNotBlank()) {
                InternshipDetailLine(
                    "Problem Solved",
                    internship.problemSolved,
                    maxLines = 3
                )
            }

            if (internship.outcomeImpact.isNotBlank()) {
                InternshipDetailLine(
                    "Outcome / Impact",
                    internship.outcomeImpact,
                    maxLines = 3
                )
            }

            InternshipDetailLine("Stipend", internship.stipend)

            if (internship.interviewDate.isNotBlank()) {
                InternshipDetailLine("Interview", internship.interviewDate)
            }

            if (internship.offerDate.isNotBlank()) {
                InternshipDetailLine("Offer", internship.offerDate)
            }

            if (internship.followUpDate.isNotBlank()) {
                InternshipDetailLine(
                    "Follow-up",
                    internship.followUpDate,
                    accent = Color(0xFFFFD23F)
                )
            }

            if (internship.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    internship.notes,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val proofSummary = internship.proofSummary
            if (proofSummary.isPresent || internship.proofLink.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                ProofResultRow(
                    summary = proofSummary,
                    thumbnailBase64 = internship.proof["thumbnail"]?.toString(),
                    proofLink = internship.proofLink
                )
            }

            if (!hasAiKey) {
                Spacer(Modifier.height(10.dp))
                AiKeyHint(hasKey = false, onOpenSetup = onOpenKeySetup)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onRecheckLevel,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (busy) "CHECKING..." else "RE-CHECK LEVEL", fontSize = 9.sp)
            }
        }
    }
}

/** Shows the proof result in simple words, with the tiny saved thumbnail. */
@Composable
private fun ProofResultRow(
    summary: ProofSummary,
    thumbnailBase64: String?,
    proofLink: String
) {
    val context = LocalContext.current
    val thumbnail = remember(thumbnailBase64) {
        ProofReader.decodeThumbnail(thumbnailBase64)?.asImageBitmap()
    }
    val color = when (summary.status.uppercase()) {
        ProofTextChecker.OK -> PrismAccent.green()
        ProofTextChecker.PARTIAL -> PrismAccent.amber()
        ProofTextChecker.UNREADABLE -> PrismAccent.red()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = "Proof photo",
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            if (summary.isPresent) {
                LinkChip("PROOF: ${summary.status}", color, null)
                Spacer(Modifier.height(4.dp))
                Text(
                    ProofTextChecker.describe(summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
            if (proofLink.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                LinkChip("OPEN PROOF LINK", PrismAccent.blue()) { openLink(context, proofLink) }
            }
        }
    }
}

@Composable
private fun InternshipDetailLine(
    label: String,
    value: String,
    maxLines: Int = 1,
    accent: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (value.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            "$label: ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            color = accent,
            fontSize = 9.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InternshipEditor(
    initial: InternshipRecord?,
    saving: Boolean,
    hasAiKey: Boolean,
    onOpenKeySetup: () -> Unit,
    onBack: () -> Unit,
    onSave: (InternshipRecord) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val firestore = remember { FirebaseFirestore.getInstance() }

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
    var companyType by remember(initial?.id) {
        mutableStateOf(initial?.companyType.orEmpty())
    }
    var workMode by remember(initial?.id) {
        mutableStateOf(initial?.workMode.orEmpty())
    }
    var proofLink by remember(initial?.id) {
        mutableStateOf(initial?.proofLink.orEmpty())
    }
    var error by remember(initial?.id) {
        mutableStateOf("")
    }
    var showInfo by remember { mutableStateOf(false) }

    // ---- Proof. The text read from the file stays in memory only. It is never saved.
    var proofMap by remember(initial?.id) { mutableStateOf(initial?.proof ?: emptyMap()) }
    var proofText by remember(initial?.id) { mutableStateOf<String?>(null) }
    var proofThumb by remember(initial?.id) { mutableStateOf(initial?.proof?.get("thumbnail")?.toString()) }
    var proofBusy by remember { mutableStateOf(false) }
    var proofMessage by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        if (uid != null) {
            profileName = ProfileFactsLoader.load(firestore, uid).fullName
        }
    }

    fun readProof(uri: android.net.Uri?) {
        if (uri == null) return
        proofBusy = true
        proofMessage = ""
        scope.launch {
            try {
                val read = ProofReader.read(context, uri)
                proofText = read.text
                proofThumb = read.thumbnailBase64
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                proofMessage = "We could not read this file. Try a clearer photo or a PDF."
            } finally {
                proofBusy = false
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { readProof(it) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { readProof(it) }

    // The check runs again when the company or your name changes, using the text in memory.
    val proofCheck = proofText?.let { ProofTextChecker.check(it, company, profileName) }
    val proofSummary: ProofSummary =
        proofCheck?.toSummary() ?: ProofStorage.summaryOf(proofMap)

    fun currentProofMap(): Map<String, Any?> =
        if (proofCheck != null) {
            ProofStorage.toMap(proofCheck, proofThumb, company.trim())
        } else {
            proofMap
        }

    // ---- Estimated level, from the typed words only (rules, before any AI).
    val preview = InternshipLevelEngine.compute(
        InternshipLevelInput(
            company = company,
            companyType = companyType,
            role = role,
            duration = duration,
            technicalSkillsUsed = technicalSkillsUsed,
            responsibilities = responsibilities,
            problemSolved = problemSolved,
            outcomeImpact = outcomeImpact,
            interviewDate = interviewDate,
            offerDate = offerDate,
            stipend = stipend,
            status = status,
            workMode = workMode,
            proof = proofSummary,
            hasProofLink = proofLink.isNotBlank()
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(13.dp)
                        )
                        .clickable(enabled = !saving, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "‹",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        if (initial == null) "ADD INTERNSHIP"
                        else "EDIT INTERNSHIP",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "INTERNSHIP TRACKER",
                        color = Color(0xFFFFD23F),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "ESTIMATED LEVEL",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LevelBadge(preview.level, preview.name)
                        Spacer(Modifier.width(8.dp))
                        InfoButton { showInfo = true }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(preview.reason, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        levelImpactText(preview.level, "internship", PrismScoring.internshipGate(status)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        item {
            Text(
                "ROLE & COMPANY",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
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
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Location") },
                singleLine = true
            )
        }

        item {
            Text(
                "COMPANY TYPE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick one. Well-known companies are also found by name.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(8.dp))
            TapChipRow {
                CompanyMatcher.CompanyType.entries.forEach { type ->
                    TapChip(type.label, companyType == type.name) {
                        companyType = if (companyType == type.name) "" else type.name
                    }
                }
            }
        }

        item {
            Text(
                "WORK PLACE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(8.dp))
            TapChipRow {
                WorkMode.ALL.forEach { (key, label) ->
                    TapChip(label, workMode == key) {
                        workMode = if (workMode == key) "" else key
                    }
                }
            }
        }

        item {
            Text(
                "EXPERIENCE EVIDENCE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
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
            Text(
                "TIMELINE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
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
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
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
            Spacer(Modifier.height(4.dp))
            Text(
                "PRIORITY",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
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
            Text(
                "PROOF",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "We check that your proof matches what you typed. Proof raises your level a little.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your file is read on your phone. Only the result and a tiny picture are saved. The file itself is never saved.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
        }

        item {
            Text("Add proof (photo or PDF)", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !proofBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("PHOTO", fontSize = 9.sp) }
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("application/pdf", "image/*")) },
                    enabled = !proofBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("PDF OR FILE", fontSize = 9.sp) }
            }
            if (proofBusy) {
                Spacer(Modifier.height(8.dp))
                Text("Reading your proof on this phone...", color = PrismAccent.amber(), fontSize = 10.sp)
            }
            if (proofMessage.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(proofMessage, color = PrismAccent.red(), fontSize = 10.sp)
            }
            if (proofSummary.isPresent) {
                Spacer(Modifier.height(10.dp))
                ProofResultRow(
                    summary = proofSummary,
                    thumbnailBase64 = proofThumb,
                    proofLink = ""
                )
                if (proofCheck != null && proofCheck.companyMatch.not() && proofSummary.status != ProofTextChecker.UNREADABLE) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tip: the company name in your file should match the company you typed above.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        proofMap = emptyMap()
                        proofText = null
                        proofThumb = null
                        proofMessage = ""
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("REMOVE PROOF", fontSize = 9.sp) }
            }
        }

        item {
            OutlinedTextField(
                value = proofLink,
                onValueChange = { proofLink = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Proof link (optional)") },
                placeholder = { Text("Offer or certificate link") },
                singleLine = true
            )
        }

        item {
            Text(
                "LINK & NOTES",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
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

        item {
            AiKeyHint(hasKey = hasAiKey, onOpenSetup = onOpenKeySetup)
        }

        if (error.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF241416)
                    )
                ) {
                    Text(
                        error,
                        color = Color(0xFFFF6B6B),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(13.dp)
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (company.trim().isBlank()) {
                        error = "Please enter a company name."
                        return@Button
                    }

                    // copy() keeps the level of an existing internship.
                    onSave(
                        (initial ?: InternshipRecord()).copy(
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
                            companyType = companyType,
                            workMode = workMode,
                            proofLink = proofLink.trim(),
                            proof = currentProofMap(),
                            createdAt = initial?.createdAt
                                ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
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

        item {
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showInfo) {
        HowLevelsWorkDialog(
            title = "How internship levels work",
            definitions = InternshipLevelEngine.PARAMETERS,
            result = preview,
            subject = "internship",
            levelNames = PrismScoring.INTERNSHIP_LEVEL_NAMES,
            extraNote = "The level counts fully only when you are SELECTED. For INTERESTED, APPLIED " +
                    "and INTERVIEW only ${(PrismScoring.INTERNSHIP_UNSELECTED_GATE * 100).toInt()}% of the level counts. " +
                    "This is an estimate from your typed words.",
            onDismiss = { showInfo = false }
        )
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
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFF211A2D)
            else Color.Transparent
        )
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                color = if (selected) Color(0xFFFFD23F)
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold
                else FontWeight.Normal,
                fontSize = 10.sp
            )
            if (selected) {
                Text("✓", color = Color(0xFF65E572), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun calculateInternshipProfileStrength(
    internships: List<InternshipRecord>,
    projectCount: Int
): Int = PrismScoring.internshipsScore(
    items = internships.map { it.toScoreInput() },
    projectCount = projectCount
)
