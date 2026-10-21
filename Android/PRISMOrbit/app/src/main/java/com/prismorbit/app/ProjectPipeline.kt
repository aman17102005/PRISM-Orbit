package com.prismorbit.app

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ============================================================
// BACKGROUND WORK FOR PROJECTS AND INTERNSHIPS
// ============================================================
// Everything here runs off the main thread, after the student
// saves. Nothing here can block saving, and nothing here is
// allowed to crash the app: every failure falls back to rules.
// ============================================================

internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}

/** The small part of the profile the checks need. */
data class ProfileFacts(val githubProfileUrl: String = "", val fullName: String = "")

object ProfileFactsLoader {
    suspend fun load(firestore: FirebaseFirestore, uid: String): ProfileFacts = try {
        val doc = firestore.collection("users").document(uid).get().awaitResult()
        ProfileFacts(
            githubProfileUrl = doc.getString("githubProfileUrl").orEmpty(),
            fullName = doc.getString("name").orEmpty()
        )
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        ProfileFacts()
    }
}

/** Asks the student's own AI (their saved key) for scores. Returns null on any problem. */
object LevelAiRunner {

    fun hasKey(context: Context, uid: String): Boolean =
        try {
            AiApiKeyManager.hasKey(context, uid)
        } catch (_: Exception) {
            false
        }

    suspend fun run(
        context: Context,
        uid: String,
        systemPrompt: String,
        userPrompt: String,
        allowed: Set<String>
    ): AiLevelScores? {
        val stored = try {
            AiApiKeyManager.getKey(context, uid)
        } catch (_: Exception) {
            null
        } ?: return null

        val reply = AiChatService.completeJson(
            provider = stored.provider,
            apiKey = stored.apiKey,
            systemText = systemPrompt,
            userText = userPrompt
        ).getOrNull()

        return LevelAi.parse(reply, allowed)
    }
}

enum class LinkCheck {
    /** Only local work. Used when a task is ticked. */
    NEVER,

    /** Check a link only if it is new, changed, or could not be checked last time. */
    IF_CHANGED,

    /** The "Re-check" button. */
    FORCE
}

object ProjectPipeline {

    /**
     * Brings one project up to date: GitHub proof, live link proof and level.
     * Returns the new item and saves only the background fields to Firestore.
     */
    suspend fun process(
        context: Context,
        firestore: FirebaseFirestore,
        uid: String,
        project: ProjectItem,
        previous: ProjectItem? = null,
        linkCheck: LinkCheck = LinkCheck.IF_CHANGED,
        runAi: Boolean = true,
        forceAi: Boolean = false,
        profile: ProfileFacts? = null
    ): ProjectItem = withContext(Dispatchers.IO) {
        var item = project

        try {
            val facts = profile
                ?: if (linkCheck != LinkCheck.NEVER || item.githubEvidence.isNotEmpty()) {
                    ProfileFactsLoader.load(firestore, uid)
                } else {
                    ProfileFacts()
                }

            item = checkLinks(item, previous, linkCheck, facts)
            item = updateLevel(context, uid, item, runAi = runAi, forceAi = forceAi)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Never crash and never block the student. Keep what we have.
        }

        try {
            firestore.collection("users").document(uid)
                .collection("projects").document(item.documentId)
                .update(item.backgroundFieldsMap())
                .awaitResult()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The values stay on screen and are worked out again next time.
        }

        item
    }

    private suspend fun checkLinks(
        project: ProjectItem,
        previous: ProjectItem?,
        mode: LinkCheck,
        facts: ProfileFacts
    ): ProjectItem {
        var item = project

        // ---- GitHub
        if (item.githubUrl.isBlank()) {
            item = item.copy(githubEvidence = emptyMap())
        } else {
            val urlChanged = previous != null && previous.githubUrl.trim() != item.githubUrl.trim()
            val evidence = item.githubEvidence
            val retry = evidence.isEmpty() ||
                    evidence["lastAttempt"] != null ||
                    evidence["checkStatus"]?.toString() == "COULD_NOT_CHECK"
            val shouldCheck = when (mode) {
                LinkCheck.FORCE -> true
                LinkCheck.IF_CHANGED -> urlChanged || retry || (previous == null && evidence.isEmpty())
                LinkCheck.NEVER -> false
            }
            item = if (shouldCheck) {
                // A changed link must not keep the old link's proof as "last good".
                val lastGood = if (urlChanged) emptyMap() else evidence
                item.copy(
                    githubEvidence = ProjectVerifier.checkGithub(item.githubUrl, facts.githubProfileUrl, lastGood)
                )
            } else {
                item.copy(githubEvidence = ProjectVerifier.applyOwnerCheck(evidence, facts.githubProfileUrl))
            }
        }

        // ---- Live link
        if (item.liveUrl.isBlank()) {
            item = item.copy(liveCheck = emptyMap())
        } else {
            val urlChanged = previous != null && previous.liveUrl.trim() != item.liveUrl.trim()
            val status = item.liveCheck["status"]?.toString()
            val shouldCheck = when (mode) {
                LinkCheck.FORCE -> true
                LinkCheck.IF_CHANGED -> urlChanged || status != "WORKS"
                LinkCheck.NEVER -> false
            }
            if (shouldCheck) {
                item = item.copy(liveCheck = ProjectVerifier.checkLive(item.liveUrl))
            }
        }

        return item
    }

    /**
     * Works out the level. Rules always run. The student's AI runs only when
     * the important text changed (hash) or the student asked for a re-check.
     */
    suspend fun updateLevel(
        context: Context,
        uid: String,
        project: ProjectItem,
        runAi: Boolean,
        forceAi: Boolean
    ): ProjectItem {
        val input = project.toLevelInput()
        val hash = LevelAi.projectHash(input)
        val textChanged = hash != project.levelHash

        var aiScores: Map<String, Double> = if (textChanged) emptyMap() else project.levelAiScores
        var aiSummary: String = if (textChanged) "" else project.levelAiSummary

        if ((runAi && textChanged) || forceAi) {
            val answer = try {
                LevelAiRunner.run(
                    context = context,
                    uid = uid,
                    systemPrompt = LevelAi.projectSystemPrompt(),
                    userPrompt = LevelAi.projectUserPrompt(input),
                    allowed = ProjectLevelEngine.AI_PARAMETERS
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (answer != null) {
                aiScores = answer.scores
                aiSummary = answer.summary
            }
        }

        val result = ProjectLevelEngine.compute(
            input = input,
            aiScores = aiScores.takeIf { it.isNotEmpty() },
            aiSummary = aiSummary.takeIf { it.isNotBlank() }
        )

        return project.withLevel(
            result = result,
            aiScores = aiScores,
            aiSummary = aiSummary,
            hash = hash,
            now = System.currentTimeMillis()
        )
    }
}
