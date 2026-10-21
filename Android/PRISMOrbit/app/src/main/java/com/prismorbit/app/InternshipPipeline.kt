package com.prismorbit.app

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ============================================================
// BACKGROUND LEVEL WORK FOR INTERNSHIPS
// ============================================================
// Runs after the student saves. Rules always run. The student's own
// AI key is used only when the important text changed (hash) or the
// student taps "Re-check level". Nothing here blocks saving and a
// failure always falls back to rules.
// ============================================================

object InternshipPipeline {

    suspend fun process(
        context: Context,
        firestore: FirebaseFirestore,
        uid: String,
        record: InternshipRecord,
        runAi: Boolean = true,
        forceAi: Boolean = false
    ): InternshipRecord = withContext(Dispatchers.IO) {
        var item = record

        try {
            item = updateLevel(context, uid, item, runAi, forceAi)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep what we have. The level is worked out again next time.
        }

        if (item.hasLevel) {
            try {
                firestore.collection("users").document(uid)
                    .collection("internships").document(item.id)
                    .update(item.levelFieldsMap())
                    .awaitResult()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The values stay on screen.
            }
        }

        item
    }

    suspend fun updateLevel(
        context: Context,
        uid: String,
        record: InternshipRecord,
        runAi: Boolean,
        forceAi: Boolean
    ): InternshipRecord {
        val input = record.toLevelInput()
        val hash = LevelAi.internshipHash(input)
        val textChanged = hash != record.levelHash

        var aiScores: Map<String, Double> = if (textChanged) emptyMap() else record.levelAiScores
        var aiSummary: String = if (textChanged) "" else record.levelAiSummary

        if ((runAi && textChanged) || forceAi) {
            val answer = try {
                LevelAiRunner.run(
                    context = context,
                    uid = uid,
                    systemPrompt = LevelAi.internshipSystemPrompt(),
                    userPrompt = LevelAi.internshipUserPrompt(input),
                    allowed = InternshipLevelEngine.AI_PARAMETERS
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

        val result = InternshipLevelEngine.compute(
            input = input,
            aiScores = aiScores.takeIf { it.isNotEmpty() },
            aiSummary = aiSummary.takeIf { it.isNotBlank() }
        )

        return record.copy(
            levelParams = result.params,
            levelScore = result.score,
            levelFinal = result.level,
            levelReason = result.reason,
            levelSource = result.source,
            levelHash = hash,
            levelUpdatedAt = System.currentTimeMillis(),
            levelAiScores = aiScores,
            levelAiSummary = aiSummary
        )
    }
}
