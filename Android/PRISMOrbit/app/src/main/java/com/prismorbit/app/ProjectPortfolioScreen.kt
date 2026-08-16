package com.prismorbit.app

import android.util.Log
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class FirebaseProjectItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val majorTopic: String = "",
    val codingLanguage: String = "",
    val status: String = "ONGOING",
    val completionDate: String = "",
    val githubUrl: String = "",
    val githubEvidence: Map<String, Any?> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

private val ProjectPurple = Color(0xFF9147FF)
private val ProjectTextSecondary = Color(0xFF92929C)
private val ProjectGreen = Color(0xFF5BE27C)
private val ProjectOrange = Color(0xFFFFB84D)
private val ProjectRed = Color(0xFFFF6868)
private const val PRISM_GITHUB_TAG = "PRISM_GITHUB"

private val projectStatuses = listOf(
    "IDEA",
    "PLANNING",
    "ONGOING",
    "COMPLETED",
    "ARCHIVED"
)

private fun projectsCollection(
    firestore: FirebaseFirestore,
    uid: String
) = firestore
    .collection("users")
    .document(uid)
    .collection("projects")

private fun FirebaseProjectItem.toFirestoreMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "name" to name,
        "normalizedName" to name.trim().lowercase(Locale.ROOT),
        "description" to description,
        "majorTopic" to majorTopic,
        "codingLanguage" to codingLanguage,
        "status" to status,
        "completionDate" to completionDate,
        "githubUrl" to githubUrl.trim(),
        "githubEvidence" to githubEvidence,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

private fun GitHubRepositoryEvidence.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "isValidUrl" to isValidUrl,
        "isPublic" to isPublic,
        "isAccessible" to isAccessible,
        "owner" to owner,
        "repository" to repository,
        "description" to description,
        "defaultBranch" to defaultBranch,
        "stars" to stars,
        "forks" to forks,
        "hasReadme" to hasReadme,
        "totalFileCount" to totalFileCount,
        "sourceFileCount" to sourceFileCount,
        "recentCommitSampleSize" to recentCommitSampleSize,
        "languageCount" to languageCount,
        "languages" to languages,
        "pushedAt" to pushedAt,
        "readmeContentLength" to readmeContentLength,
        "readmeHasMeaningfulContent" to readmeHasMeaningfulContent,
        "sourceContentSampleCount" to sourceContentSampleCount,
        "sourceContentNonEmptyCount" to sourceContentNonEmptyCount,
        "sourceContentCharacters" to sourceContentCharacters,
        "sourceContentHasMeaningfulCode" to sourceContentHasMeaningfulCode,
        "sampledSourceFiles" to sampledSourceFiles,
        "errorMessage" to errorMessage
    )
}

private fun firestoreMapToProject(
    data: Map<String, Any?>
): FirebaseProjectItem {
    return FirebaseProjectItem(
        id = data["id"] as? String ?: UUID.randomUUID().toString(),
        name = data["name"] as? String ?: "",
        description = data["description"] as? String ?: "",
        majorTopic = data["majorTopic"] as? String ?: "",
        codingLanguage = data["codingLanguage"] as? String ?: "",
        status = data["status"] as? String ?: "ONGOING",
        completionDate = data["completionDate"] as? String ?: "",
        githubUrl = data["githubUrl"] as? String ?: "",
        githubEvidence = (data["githubEvidence"] as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                (key as? String)?.let { it to value }
            }?.toMap()
            ?: emptyMap(),
        createdAt = (data["createdAt"] as? Number)?.toLong()
            ?: System.currentTimeMillis(),
        updatedAt = (data["updatedAt"] as? Number)?.toLong()
            ?: System.currentTimeMillis()
    )
}

@Composable
fun ProjectsScreen(
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val projects = remember {
        mutableStateListOf<FirebaseProjectItem>()
    }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<FirebaseProjectItem?>(null) }
    var deletingProject by remember { mutableStateOf<FirebaseProjectItem?>(null) }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid

        if (uid == null) {
            isLoading = false
            errorMessage = "No signed-in user found."
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = ""

        projectsCollection(firestore, uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val loaded = snapshot.documents
                    .mapNotNull { document ->
                        document.data?.let(::firestoreMapToProject)
                    }
                    .sortedByDescending { it.createdAt }

                projects.clear()
                projects.addAll(loaded)
                isLoading = false
            }
            .addOnFailureListener { exception ->
                isLoading = false
                errorMessage = exception.message ?: "Unable to load projects."
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        "←",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 28.sp
                    )
                }

                Spacer(Modifier.width(4.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        "PROJECTS",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        "${projects.size} saved projects",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = {
                        editingProject = null
                        errorMessage = ""
                        showEditor = true
                    }
                ) {
                    Text(
                        "+ ADD",
                        color = ProjectPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (errorMessage.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF241416)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        errorMessage,
                        color = ProjectRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(15.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            when {
                isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ProjectPurple)
                    }
                }

                projects.isEmpty() -> {
                    EmptyProjectsState {
                        editingProject = null
                        errorMessage = ""
                        showEditor = true
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 35.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = projects,
                            key = { it.id }
                        ) { project ->
                            ProjectCard(
                                project = project,
                                onEdit = {
                                    editingProject = project
                                    errorMessage = ""
                                    showEditor = true
                                },
                                onDelete = {
                                    deletingProject = project
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showEditor) {
            ProjectEditor(
                initialProject = editingProject,
                saving = isSaving,
                onDismiss = {
                    if (!isSaving) {
                        showEditor = false
                        editingProject = null
                    }
                },
                onSave = { project ->
                    val uid = currentUser?.uid

                    if (uid == null) {
                        errorMessage = "No signed-in user found."
                        return@ProjectEditor
                    }

                    val cleanName = project.name.trim()
                    val cleanDescription = project.description.trim()
                    val cleanMajorTopic = project.majorTopic.trim()
                    val cleanLanguage = project.codingLanguage.trim()
                    val cleanGithubUrl = project.githubUrl.trim()

                    when {
                        cleanName.isBlank() -> {
                            errorMessage = "Project name is required."
                        }

                        cleanDescription.isBlank() -> {
                            errorMessage = "Project description is required."
                        }

                        cleanMajorTopic.isBlank() -> {
                            errorMessage = "Major topic is required."
                        }

                        cleanLanguage.isBlank() -> {
                            errorMessage = "Coding language is required."
                        }

                        project.status == "COMPLETED" &&
                                project.completionDate.trim().isBlank() -> {
                            errorMessage =
                                "Completion date is required for a completed project."
                        }

                        else -> {
                            val normalizedName =
                                cleanName.lowercase(Locale.ROOT)

                            val duplicateExists = projects.any { existing ->
                                existing.id != project.id &&
                                        existing.name.trim()
                                            .lowercase(Locale.ROOT) == normalizedName
                            }

                            if (duplicateExists) {
                                errorMessage =
                                    "A project with this name already exists."
                            } else {
                                isSaving = true
                                errorMessage = ""

                                scope.launch {
                                    Log.d(
                                        PRISM_GITHUB_TAG,
                                        "SAVE BUTTON FLOW ENTERED"
                                    )

                                    try {
                                        Log.d(
                                            PRISM_GITHUB_TAG,
                                            "Project data prepared. name=$cleanName, githubUrl=$cleanGithubUrl"
                                        )

                                        val githubEvidence =
                                            if (cleanGithubUrl.isBlank()) {
                                                Log.d(
                                                    PRISM_GITHUB_TAG,
                                                    "GitHub URL is blank. Skipping GitHub analysis."
                                                )
                                                emptyMap()
                                            } else {
                                                Log.d(
                                                    PRISM_GITHUB_TAG,
                                                    "CALLING GitHubAnalyzer.analyzeRepository()"
                                                )

                                                val evidence =
                                                    GitHubAnalyzer
                                                        .analyzeRepository(cleanGithubUrl)

                                                Log.d(
                                                    PRISM_GITHUB_TAG,
                                                    "GitHubAnalyzer RETURNED: valid=${evidence.isValidUrl}, " +
                                                            "public=${evidence.isPublic}, accessible=${evidence.isAccessible}, " +
                                                            "repo=${evidence.repository}, totalFiles=${evidence.totalFileCount}, " +
                                                            "sourceFiles=${evidence.sourceFileCount}, languages=${evidence.languages}, " +
                                                            "readmeChars=${evidence.readmeContentLength}, " +
                                                            "sourceSamples=${evidence.sourceContentSampleCount}, " +
                                                            "sourceChars=${evidence.sourceContentCharacters}"
                                                )

                                                if (!evidence.errorMessage.isNullOrBlank()) {
                                                    Log.e(
                                                        PRISM_GITHUB_TAG,
                                                        "GitHubAnalyzer ERROR: ${evidence.errorMessage}"
                                                    )
                                                }

                                                evidence.toFirestoreMap()
                                            }

                                        Log.d(
                                            PRISM_GITHUB_TAG,
                                            "BUILDING projectToSave. githubEvidencePresent=${githubEvidence.isNotEmpty()}"
                                        )

                                        val projectToSave = project.copy(
                                            name = cleanName,
                                            description = cleanDescription,
                                            majorTopic = cleanMajorTopic,
                                            codingLanguage = cleanLanguage,
                                            githubUrl = cleanGithubUrl,
                                            githubEvidence = githubEvidence,
                                            completionDate =
                                                if (project.status == "COMPLETED") {
                                                    project.completionDate.trim()
                                                } else {
                                                    ""
                                                },
                                            updatedAt = System.currentTimeMillis()
                                        )

                                        projectsCollection(firestore, uid)
                                            .document(projectToSave.id)
                                            .set(
                                                projectToSave.toFirestoreMap(),
                                                SetOptions.merge()
                                            )
                                            .addOnSuccessListener {
                                                Log.d(
                                                    PRISM_GITHUB_TAG,
                                                    "FIRESTORE SAVE SUCCESS: projectId=${projectToSave.id}"
                                                )

                                                val index = projects.indexOfFirst {
                                                    it.id == projectToSave.id
                                                }

                                                if (index >= 0) {
                                                    projects[index] = projectToSave
                                                } else {
                                                    projects.add(0, projectToSave)
                                                }

                                                val sorted =
                                                    projects.sortedByDescending {
                                                        it.createdAt
                                                    }

                                                projects.clear()
                                                projects.addAll(sorted)

                                                isSaving = false
                                                showEditor = false
                                                editingProject = null

                                                val githubError =
                                                    projectToSave.githubEvidence["errorMessage"]
                                                            as? String

                                                errorMessage =
                                                    if (cleanGithubUrl.isNotBlank() &&
                                                        !githubError.isNullOrBlank()
                                                    ) {
                                                        "Project saved, but GitHub could not be analyzed: $githubError"
                                                    } else {
                                                        ""
                                                    }
                                            }
                                            .addOnFailureListener { exception ->
                                                Log.e(
                                                    PRISM_GITHUB_TAG,
                                                    "FIRESTORE SAVE FAILED",
                                                    exception
                                                )

                                                isSaving = false
                                                errorMessage =
                                                    exception.message
                                                        ?: "Unable to save project."
                                            }

                                    } catch (exception: Exception) {
                                        Log.e(
                                            PRISM_GITHUB_TAG,
                                            "SAVE FLOW EXCEPTION",
                                            exception
                                        )

                                        isSaving = false
                                        errorMessage =
                                            exception.message
                                                ?: "Unable to analyze GitHub repository."
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        deletingProject?.let { project ->
            AlertDialog(
                onDismissRequest = {
                    deletingProject = null
                },
                title = {
                    Text("Delete project?")
                },
                text = {
                    Text(
                        "Are you sure you want to permanently delete \"${project.name}\"?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val uid = currentUser?.uid

                            if (uid == null) {
                                errorMessage = "No signed-in user found."
                                deletingProject = null
                            } else {
                                projectsCollection(firestore, uid)
                                    .document(project.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        projects.removeAll {
                                            it.id == project.id
                                        }

                                        deletingProject = null
                                        errorMessage = ""
                                    }
                                    .addOnFailureListener { exception ->
                                        errorMessage =
                                            exception.message
                                                ?: "Unable to delete project."
                                        deletingProject = null
                                    }
                            }
                        }
                    ) {
                        Text("DELETE", color = ProjectRed)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            deletingProject = null
                        }
                    ) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyProjectsState(
    onAddProject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "NO PROJECTS YET",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Your saved projects will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onAddProject,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProjectPurple
                )
            ) {
                Text(
                    "+  ADD PROJECT",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: FirebaseProjectItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(18.dp)) {

            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        project.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        project.majorTopic,
                        color = ProjectPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onEdit) {
                    Text(
                        "EDIT",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                }

                TextButton(onClick = onDelete) {
                    Text(
                        "DELETE",
                        color = ProjectRed,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                project.description,
                color = Color(0xFFCCCCD2),
                fontSize = 13.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProjectInfoChip(project.codingLanguage)
                ProjectStatusChip(project.status)
            }

            if (project.githubEvidence.isNotEmpty()) {
                val githubAccessible =
                    project.githubEvidence["isAccessible"] as? Boolean ?: false
                val sourceFiles =
                    (project.githubEvidence["sourceFileCount"] as? Number)?.toInt() ?: 0

                Spacer(Modifier.height(10.dp))

                Text(
                    if (githubAccessible) {
                        "GitHub analyzed • $sourceFiles source files detected"
                    } else {
                        "GitHub analysis unavailable"
                    },
                    color = if (githubAccessible) ProjectGreen else ProjectOrange,
                    fontSize = 10.sp
                )
            }

            if (project.completionDate.isNotBlank()) {
                Spacer(Modifier.height(10.dp))

                Text(
                    "Completed: ${project.completionDate}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ProjectInfoChip(
    text: String
) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFF202027),
                shape = RoundedCornerShape(50.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ProjectStatusChip(
    status: String
) {
    val statusColor =
        when (status) {
            "COMPLETED" -> ProjectGreen
            "ARCHIVED" -> MaterialTheme.colorScheme.onSurfaceVariant
            "IDEA" -> ProjectPurple
            "PLANNING" -> ProjectOrange
            else -> ProjectOrange
        }

    Box(
        modifier = Modifier
            .background(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {
        Text(
            status,
            color = statusColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProjectEditor(
    initialProject: FirebaseProjectItem?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (FirebaseProjectItem) -> Unit
) {
    var name by remember(initialProject?.id) {
        mutableStateOf(initialProject?.name ?: "")
    }

    var description by remember(initialProject?.id) {
        mutableStateOf(initialProject?.description ?: "")
    }

    var majorTopic by remember(initialProject?.id) {
        mutableStateOf(initialProject?.majorTopic ?: "")
    }

    var codingLanguage by remember(initialProject?.id) {
        mutableStateOf(initialProject?.codingLanguage ?: "")
    }

    var githubUrl by remember(initialProject?.id) {
        mutableStateOf(initialProject?.githubUrl ?: "")
    }

    var status by remember(initialProject?.id) {
        mutableStateOf(
            initialProject?.status?.uppercase(Locale.ROOT) ?: "ONGOING"
        )
    }

    var completionDate by remember(initialProject?.id) {
        mutableStateOf(initialProject?.completionDate ?: "")
    }

    var statusMenuExpanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !saving
                    ) {
                        Text(
                            "←",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 28.sp
                        )
                    }

                    Spacer(Modifier.width(5.dp))

                    Column {
                        Text(
                            if (initialProject == null) {
                                "ADD PROJECT"
                            } else {
                                "EDIT PROJECT"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "PROJECT PORTFOLIO",
                            color = ProjectPurple,
                            fontSize = 9.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Project Name") },
                    placeholder = { Text("Enter project name") },
                    singleLine = true,
                    enabled = !saving
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    placeholder = {
                        Text("What does this project do?")
                    },
                    minLines = 4,
                    enabled = !saving
                )
            }

            item {
                OutlinedTextField(
                    value = majorTopic,
                    onValueChange = { majorTopic = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Major Topic") },
                    placeholder = {
                        Text("e.g. Android Development")
                    },
                    singleLine = true,
                    enabled = !saving
                )
            }

            item {
                OutlinedTextField(
                    value = codingLanguage,
                    onValueChange = { codingLanguage = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Coding Language / Technology")
                    },
                    placeholder = {
                        Text("e.g. Kotlin")
                    },
                    singleLine = true,
                    enabled = !saving
                )
            }

            item {
                OutlinedTextField(
                    value = githubUrl,
                    onValueChange = { githubUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("GitHub Repository URL (Optional)")
                    },
                    placeholder = {
                        Text("https://github.com/username/repository")
                    },
                    singleLine = true,
                    enabled = !saving
                )
            }

            item {
                Box(Modifier.fillMaxWidth()) {

                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !saving) {
                                statusMenuExpanded = true
                            },
                        label = { Text("Project Status") },
                        readOnly = true,
                        enabled = !saving
                    )

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = {
                            statusMenuExpanded = false
                        }
                    ) {
                        projectStatuses.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(option)
                                },
                                onClick = {
                                    status = option

                                    if (option != "COMPLETED") {
                                        completionDate = ""
                                    }

                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (status == "COMPLETED") {
                item {
                    OutlinedTextField(
                        value = completionDate,
                        onValueChange = {
                            completionDate = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Completion Date")
                        },
                        placeholder = {
                            Text("YYYY-MM-DD")
                        },
                        singleLine = true,
                        enabled = !saving
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val existing = initialProject

                        onSave(
                            FirebaseProjectItem(
                                id = existing?.id
                                    ?: UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                majorTopic = majorTopic,
                                codingLanguage = codingLanguage,
                                status = status,
                                completionDate =
                                    if (status == "COMPLETED") {
                                        completionDate
                                    } else {
                                        ""
                                    },
                                githubUrl = githubUrl,
                                createdAt =
                                    existing?.createdAt
                                        ?: System.currentTimeMillis(),
                                updatedAt =
                                    System.currentTimeMillis()
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProjectPurple
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(20.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(10.dp))
                    }

                    Text(
                        if (saving) "SAVING..." else "SAVE PROJECT",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}