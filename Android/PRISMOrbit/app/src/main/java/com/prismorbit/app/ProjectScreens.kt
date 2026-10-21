package com.prismorbit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt

// =========================================================
// PROJECTS SCREEN (the one project screen in the app)
// =========================================================
// GitHub and live links are checked in the background after
// saving. Each project has a level (1-5) that changes its marks.
// =========================================================

@Composable
internal fun ProjectsScreen(
    projects: List<ProjectItem>,
    busyIds: List<Long>,
    onBack: () -> Unit,
    onAddProject: (ProjectItem) -> Unit,
    /** Second value: true when saved from the editor, false for a quick tick of a task. */
    onUpdateProject: (ProjectItem, Boolean) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onRecheckLinks: (ProjectItem) -> Unit,
    onRecheckLevel: (ProjectItem) -> Unit
) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var showAddProject by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<ProjectItem?>(null) }
    var showKeySetup by remember { mutableStateOf(false) }
    var infoFor by remember { mutableStateOf<ProjectItem?>(null) }

    // Refreshes right after the key is saved or removed on the setup screen.
    val refreshVersion = AiKeyRefreshSignal.version
    val hasAiKey = remember(uid, refreshVersion, showKeySetup) {
        uid != null && LevelAiRunner.hasKey(context, uid)
    }

    val completedProjects = projects.count { it.status == "COMPLETED" }
    val averageProgress = if (projects.isEmpty()) 0 else projects.map { it.progress }.average().roundToInt()
    val projectsWithGithub = projects.count { it.githubUrl.isNotBlank() }
    // Same number the Placement, Growth and Home screens use.
    val portfolioScore = PrismScoring.projectsScore(projects.map { it.toScoreInput() })

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Light)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("PROJECTS", color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("BUILD • DOCUMENT • SHOWCASE", color = PrismAccent.green(), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
                LinkChip("${projects.size} TOTAL", PrismAccent.green(), null)
            }

            Spacer(modifier = Modifier.height(23.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PORTFOLIO OVERVIEW", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                            Spacer(modifier = Modifier.height(7.dp))
                            Text("${projects.size} projects", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("Real proof and higher levels raise your project score.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 14.sp)
                        }
                        Text("$portfolioScore", color = PrismAccent.green(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    ProgressBar(portfolioScore / 100f)
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        "PROJECT SCORE (SAME ON HOME, PLACEMENT AND GROWTH) • AVERAGE PROGRESS $averageProgress%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(13.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStatCard(Modifier.weight(1f), "PROJECTS", projects.size.toString())
                SmallStatCard(Modifier.weight(1f), "COMPLETED", completedProjects.toString())
                SmallStatCard(Modifier.weight(1f), "GITHUB", projectsWithGithub.toString())
            }

            Spacer(modifier = Modifier.height(23.dp))
            SectionTitle("YOUR PROJECTS")
            Spacer(modifier = Modifier.height(11.dp))

            if (projects.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("NO PROJECTS YET", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = .7.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Add your projects, tasks, links and project photos here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
                        Spacer(modifier = Modifier.height(13.dp))
                        Text("Start with one project and build your portfolio from there.", color = PrismAccent.green(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                projects.forEach { project ->
                    ProjectCard(
                        project = project,
                        busy = project.id in busyIds,
                        hasAiKey = hasAiKey,
                        onEdit = { editingProject = project },
                        onDelete = { onDeleteProject(project.id) },
                        onToggleTask = { taskId ->
                            onUpdateProject(
                                project.copy(
                                    tasks = project.tasks.map { task ->
                                        if (task.id == taskId) task.copy(completed = !task.completed) else task
                                    }
                                ),
                                false
                            )
                        },
                        onRecheckLinks = { onRecheckLinks(project) },
                        onRecheckLevel = { onRecheckLevel(project) },
                        onOpenKeySetup = { showKeySetup = true },
                        onInfo = { infoFor = project }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(13.dp))
            Button(
                onClick = { showAddProject = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF65A96B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+  ADD PROJECT", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(25.dp))
        }

        if (showAddProject) {
            ProjectEditorScreen(
                initialProject = null,
                hasAiKey = hasAiKey,
                onOpenKeySetup = { showKeySetup = true },
                onBack = { showAddProject = false },
                onSave = { onAddProject(it); showAddProject = false }
            )
        }

        editingProject?.let { project ->
            ProjectEditorScreen(
                initialProject = project,
                hasAiKey = hasAiKey,
                onOpenKeySetup = { showKeySetup = true },
                onBack = { editingProject = null },
                onSave = { onUpdateProject(it, true); editingProject = null }
            )
        }

        // The existing key setup screen opens on top, so a half-filled form is not lost.
        if (showKeySetup) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                SmartAiApiKeySetupScreen(onBack = { showKeySetup = false })
            }
        }
    }

    infoFor?.let { project ->
        val result = projectLevelResult(project)
        HowLevelsWorkDialog(
            title = "How project levels work",
            definitions = ProjectLevelEngine.PARAMETERS,
            result = result,
            subject = "project",
            levelNames = PrismScoring.PROJECT_LEVEL_NAMES,
            extraNote = "GitHub proof counts only when the repo is yours and can be checked. " +
                    "A live link counts fully only when it works.",
            onDismiss = { infoFor = null }
        )
    }
}

/** Rules-only result for the info sheet and the editor preview (uses saved facts). */
internal fun projectLevelResult(project: ProjectItem): LevelResult =
    ProjectLevelEngine.compute(
        input = project.toLevelInput(),
        aiScores = project.levelAiScores.takeIf { it.isNotEmpty() && project.levelSource == "ai+rules" },
        aiSummary = project.levelAiSummary.takeIf { it.isNotBlank() }
    )

@Composable
private fun ProjectCard(
    project: ProjectItem,
    busy: Boolean,
    hasAiKey: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleTask: (Long) -> Unit,
    onRecheckLinks: () -> Unit,
    onRecheckLevel: () -> Unit,
    onOpenKeySetup: () -> Unit,
    onInfo: () -> Unit
) {
    val context = LocalContext.current
    val completedTasks = project.tasks.count { it.completed }
    val taskTotal = project.tasks.size
    val statusColor = when (project.status) {
        "COMPLETED" -> PrismAccent.green()
        "ARCHIVED" -> MaterialTheme.colorScheme.onSurfaceVariant
        "PLANNING" -> PrismAccent.amber()
        "ONGOING" -> PrismAccent.blue()
        else -> PrismAccent.purple()
    }

    val githubChip = ProjectVerifier.githubChip(project.githubUrl, project.githubEvidence)
    val liveChip = ProjectVerifier.liveChip(project.liveUrl, project.liveCheck)
    val detected = GithubEvidenceInput.fromMap(project.githubEvidence)?.detectedFeatures.orEmpty()
    val features = (project.featureChips + detected).distinct().filter { it in ProjectFeatures.ALL }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(5.dp))
                    LinkChip(project.status, statusColor, null)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("${project.progress}%", color = statusColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            // ---- Level
            Spacer(modifier = Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBadge(project.level, project.levelName)
                Spacer(Modifier.width(8.dp))
                InfoButton(onInfo)
            }
            Spacer(Modifier.height(6.dp))
            if (!project.hasLevel) {
                Text(
                    if (busy) "Working out the level..." else "Level 3 for now. The level is worked out in the background.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            } else {
                if (project.levelReason.isNotBlank()) {
                    Text(project.levelReason, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    levelImpactText(project.level, "project"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }

            if (project.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Text(project.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 15.sp)
            }

            Spacer(modifier = Modifier.height(11.dp))
            ProgressBar(project.progress / 100f)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectMetaPill("TECH", project.techStack.ifBlank { "Not added" }, Modifier.weight(1f))
                ProjectMetaPill("TASKS", "$completedTasks / $taskTotal", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(7.dp))
            ProjectMetaPill("STARTED", project.startDate.ifBlank { "Not added" }, Modifier.fillMaxWidth())

            // ---- Link proof (tap a chip to open the link)
            if (project.githubUrl.isNotBlank() || project.liveUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                TapChipRow {
                    if (githubChip != GithubChip.NONE) {
                        LinkChip(githubChip.text, githubChipColor(githubChip)) { openLink(context, project.githubUrl) }
                    }
                    if (liveChip != LiveChip.NONE) {
                        LinkChip(liveChip.text, liveChipColor(liveChip)) { openLink(context, project.liveUrl) }
                    }
                }
                if (ProjectVerifier.needsProfileUrlNote(project.githubUrl, project.githubEvidence)) {
                    Spacer(Modifier.height(6.dp))
                    Text(ProjectVerifier.NEUTRAL_OWNER_NOTE, color = PrismAccent.amber(), fontSize = 9.sp, lineHeight = 13.sp)
                }
                if (githubChip == GithubChip.NOT_YOURS) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "This repo belongs to another GitHub user, so it gives no GitHub proof.",
                        color = PrismAccent.red(), fontSize = 9.sp, lineHeight = 13.sp
                    )
                }
                if (githubChip == GithubChip.COULD_NOT_CHECK) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "GitHub could not be reached right now. Your link is saved. Try Re-check later.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 13.sp
                    )
                }
                githubEvidenceLine(project.githubEvidence)?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 13.sp)
                }
            }

            if (features.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                TapChipRow {
                    features.forEach { key ->
                        val confirmed = key in detected
                        LinkChip(
                            ProjectFeatures.label(key).uppercase() + if (confirmed) " ✓" else "",
                            if (confirmed) PrismAccent.green() else MaterialTheme.colorScheme.onSurfaceVariant,
                            null
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("✓ = found in your code. Others are what you ticked.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            }

            if (project.tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("TASKS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(5.dp))
                project.tasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTask(task.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (task.completed) "☑" else "☐", color = if (task.completed) PrismAccent.green() else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(task.title, color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }

            if (!hasAiKey) {
                Spacer(Modifier.height(12.dp))
                AiKeyHint(hasKey = false, onOpenSetup = onOpenKeySetup)
            }

            Spacer(modifier = Modifier.height(11.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp)) {
                    Text("EDIT", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp)) {
                    Text("DELETE", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRecheckLinks,
                    enabled = !busy && (project.githubUrl.isNotBlank() || project.liveUrl.isNotBlank()),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text(if (busy) "CHECKING..." else "RE-CHECK LINKS", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRecheckLevel,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text("RE-CHECK LEVEL", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** A short "what we found" line from the saved GitHub proof. */
private fun githubEvidenceLine(evidence: Map<String, Any?>): String? {
    if (evidence["isAccessible"] != true) return null
    val files = (evidence["sourceFileCount"] as? Number)?.toInt() ?: 0
    val commits = (evidence["recentCommitSampleSize"] as? Number)?.toInt() ?: 0
    val parts = mutableListOf<String>()
    parts += if (files > 0) "$files source files" else "no source files found"
    if (evidence["hasReadme"] == true) parts += "README found"
    if (commits > 0) parts += "$commits recent commits"
    return "GitHub checked: " + parts.joinToString(", ") + "."
}

@Composable
private fun ProjectMetaPill(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ProjectEditorScreen(
    initialProject: ProjectItem?,
    hasAiKey: Boolean,
    onOpenKeySetup: () -> Unit,
    onBack: () -> Unit,
    onSave: (ProjectItem) -> Unit
) {
    var name by remember { mutableStateOf(initialProject?.name ?: "") }
    var description by remember { mutableStateOf(initialProject?.description ?: "") }
    var techStack by remember { mutableStateOf(initialProject?.techStack ?: "") }
    var startDate by remember { mutableStateOf(initialProject?.startDate ?: "") }
    var status by remember { mutableStateOf(initialProject?.status ?: "IDEA") }
    var githubUrl by remember { mutableStateOf(initialProject?.githubUrl ?: "") }
    var liveUrl by remember { mutableStateOf(initialProject?.liveUrl ?: "") }
    var progressText by remember { mutableStateOf(initialProject?.progress?.toString() ?: "0") }
    var taskText by remember { mutableStateOf(initialProject?.tasks?.joinToString("\n") { it.title } ?: "") }
    var realUsers by remember { mutableStateOf(initialProject?.realUsers ?: RealUsersTier.NONE) }
    var teamSizeText by remember { mutableStateOf(initialProject?.teamSize?.takeIf { it > 0 }?.toString() ?: "") }
    var chips by remember { mutableStateOf(initialProject?.featureChips?.toSet() ?: emptySet()) }
    var error by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    // Project status is intentionally a real selectable list, not a hardcoded single option.
    val statuses = listOf("IDEA", "PLANNING", "ONGOING", "COMPLETED", "ARCHIVED")
    val progress = progressText.toIntOrNull()?.coerceIn(0, 100) ?: 0
    val teamSize = teamSizeText.toIntOrNull()?.coerceIn(0, 999) ?: 0

    // Live "Estimated level": rules only, worked out from what is typed right now.
    // Saved proof is used only while the link is unchanged.
    val sameGithub = initialProject != null && initialProject.githubUrl.trim() == githubUrl.trim()
    val sameLive = initialProject != null && initialProject.liveUrl.trim() == liveUrl.trim()
    val preview = ProjectLevelEngine.compute(
        ProjectItem(
            name = name,
            description = description,
            techStack = techStack,
            startDate = startDate,
            status = status,
            githubUrl = githubUrl.trim(),
            liveUrl = liveUrl.trim(),
            progress = progress,
            tasks = taskText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                .mapIndexed { index, title ->
                    ProjectTask(
                        id = index.toLong(),
                        title = title,
                        completed = initialProject?.tasks?.getOrNull(index)?.completed ?: false
                    )
                },
            githubEvidence = if (sameGithub) initialProject!!.githubEvidence else emptyMap(),
            liveCheck = if (sameLive) initialProject!!.liveCheck else emptyMap(),
            realUsers = realUsers,
            teamSize = teamSize,
            featureChips = chips.toList()
        ).toLevelInput()
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 38.sp, modifier = Modifier.clickable { onBack() })
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(if (initialProject == null) "ADD PROJECT" else "EDIT PROJECT", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("PROJECT PORTFOLIO", color = PrismAccent.green(), fontSize = 8.sp, letterSpacing = 1.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ---- Estimated level preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("ESTIMATED LEVEL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
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
                        levelImpactText(preview.level, "project") + " Links are checked after you save.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(name, { name = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Project Name") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(description, { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Description") }, placeholder = { Text("What problem does it solve, and for whom?") }, minLines = 3)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(techStack, { techStack = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tech Stack") }, placeholder = { Text("Kotlin, Compose, Firebase...") }, singleLine = true)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(startDate, { startDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Start Date") }, placeholder = { Text("DD/MM/YYYY") }, singleLine = true)

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("STATUS")
            Spacer(modifier = Modifier.height(8.dp))
            statuses.forEach { option ->
                ChoiceButton(option, status == option) { status = option }
                Spacer(modifier = Modifier.height(7.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(githubUrl, { githubUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("GitHub URL") }, singleLine = true)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "We check that the repo is public and that it is yours. Add your GitHub profile link in Placement.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(liveUrl, { liveUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Live Demo URL") }, singleLine = true)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Website, Play Store or App Store link. We check that it opens.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("WHAT DOES IT HAVE?")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap what is true. Features we find in your GitHub code count fully. Features you only tick count half.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            TapChipRow {
                ProjectFeatures.CHIPS.forEach { (key, label) ->
                    TapChip(label, key in chips) {
                        chips = if (key in chips) chips - key else chips + key
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("REAL USERS")
            Spacer(modifier = Modifier.height(8.dp))
            TapChipRow {
                RealUsersTier.ALL.forEach { (key, label) ->
                    TapChip(label, realUsers == key) { realUsers = key }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                teamSizeText,
                { teamSizeText = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Team size (people)") },
                placeholder = { Text("1 if you built it alone") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("PROGRESS")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(progressText, { progressText = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth(), label = { Text("Progress (0–100)") }, singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            ProgressBar(progress / 100f)

            Spacer(modifier = Modifier.height(18.dp))
            SectionTitle("TASKS")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(taskText, { taskText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tasks — one per line") }, placeholder = { Text("Design UI\nBuild backend\nTesting\nDeploy") }, minLines = 4)

            Spacer(modifier = Modifier.height(18.dp))
            AiKeyHint(hasKey = hasAiKey, onOpenSetup = onOpenKeySetup)

            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(error, color = PrismAccent.red(), fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        error = "Please enter a project name."
                    } else {
                        val tasks = taskText.lines().map { it.trim() }.filter { it.isNotEmpty() }.mapIndexed { index, title ->
                            val old = initialProject?.tasks?.getOrNull(index)
                            ProjectTask(id = old?.id ?: (System.currentTimeMillis() + index), title = title, completed = old?.completed ?: false)
                        }
                        val base = initialProject ?: ProjectItem(
                            name = "", description = "", techStack = "", startDate = "",
                            status = "IDEA", githubUrl = "", liveUrl = "", progress = 0
                        )
                        // copy() keeps photos, proof and level of an existing project.
                        // A changed link drops the proof of the old link.
                        onSave(
                            base.copy(
                                name = name.trim(),
                                description = description.trim(),
                                techStack = techStack.trim(),
                                startDate = startDate.trim(),
                                status = status,
                                githubUrl = githubUrl.trim(),
                                liveUrl = liveUrl.trim(),
                                progress = progress,
                                tasks = tasks,
                                realUsers = realUsers,
                                teamSize = teamSize,
                                featureChips = chips.toList(),
                                githubEvidence = if (sameGithub) base.githubEvidence else emptyMap(),
                                liveCheck = if (sameLive) base.liveCheck else emptyMap()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF65A96B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SAVE PROJECT", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showInfo) {
        HowLevelsWorkDialog(
            title = "How project levels work",
            definitions = ProjectLevelEngine.PARAMETERS,
            result = preview,
            subject = "project",
            levelNames = PrismScoring.PROJECT_LEVEL_NAMES,
            extraNote = "This is an estimate from your typed words. Link checks happen after you save.",
            onDismiss = { showInfo = false }
        )
    }
}
