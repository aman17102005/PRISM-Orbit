package com.prismorbit.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// ============================================================
// SHARED LEVEL / CHIP UI
// ============================================================
// Used by the project and internship screens. Colours are chosen
// for both the dark and the light theme.
// ============================================================

internal object PrismAccent {
    @Composable
    fun isLight(): Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f

    @Composable
    fun green(): Color = if (isLight()) Color(0xFF1E7B34) else Color(0xFF65E572)

    @Composable
    fun amber(): Color = if (isLight()) Color(0xFF8A5A00) else Color(0xFFFFD23F)

    @Composable
    fun red(): Color = if (isLight()) Color(0xFFB3261E) else Color(0xFFFF7B72)

    @Composable
    fun purple(): Color = if (isLight()) Color(0xFF6B3FA0) else Color(0xFFB76CFF)

    @Composable
    fun blue(): Color = if (isLight()) Color(0xFF00688A) else Color(0xFF00D9FF)

    @Composable
    fun forLevel(level: Int): Color = when (level) {
        1 -> MaterialTheme.colorScheme.onSurfaceVariant
        2 -> blue()
        3 -> green()
        4 -> purple()
        else -> amber()
    }
}

internal fun openLink(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LiveLinks.normalize(url))))
    } catch (_: Exception) {
        // No browser installed or a bad link: do nothing.
    }
}

/** A small tappable chip. */
@Composable
internal fun LinkChip(text: String, color: Color, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.14f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

/** A tap-only choice chip (select / unselect). */
@Composable
internal fun TapChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val accent = PrismAccent.purple()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .border(
                BorderStroke(1.dp, if (selected) accent else MaterialTheme.colorScheme.outline),
                RoundedCornerShape(50.dp)
            )
            .background(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp)
    ) {
        Text(
            if (selected) "✓ $text" else text,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
internal fun TapChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
internal fun LevelBadge(level: Int, name: String, modifier: Modifier = Modifier) {
    val color = PrismAccent.forLevel(level)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            PrismScoring.levelLabel(level, name).uppercase(Locale.ROOT),
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

/** Round "i" button that opens the "How levels work" sheet. */
@Composable
internal fun InfoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("i", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** "Level 4 gives x1.25 ..." in simple words. */
internal fun levelImpactText(level: Int, subject: String, gate: Double = 1.0): String {
    val factor = PrismScoring.levelFactor(level)
    val factorText = "x" + String.format(Locale.US, "%.2f", factor)
    return if (gate >= 1.0) {
        "Level $level gives $factorText to this $subject's marks."
    } else {
        val effective = "x" + String.format(Locale.US, "%.2f", PrismScoring.effectiveFactor(level, gate))
        "Level $level gives $factorText once selected. Until then only ${(gate * 100).toInt()}% of it counts ($effective)."
    }
}

/** Shown when no AI key is saved. Opens the existing key setup screen. */
@Composable
internal fun AiKeyHint(hasKey: Boolean, onOpenSetup: () -> Unit) {
    if (hasKey) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Add your free AI key for smarter levels",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = onOpenSetup, shape = RoundedCornerShape(11.dp)) {
            Text("ADD AI KEY", fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** The "How levels work" sheet: parameters, weights and the 2 lowest scoring tips. */
@Composable
internal fun HowLevelsWorkDialog(
    title: String,
    definitions: List<LevelParameter>,
    result: LevelResult?,
    subject: String,
    levelNames: List<String>,
    extraNote: String? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Your $subject gets a level from 1 to 5. A higher level adds more to your marks. " +
                            "The app works out the level from the facts below. You cannot set it by hand.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Text("WHAT COUNTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                definitions.forEach { def ->
                    val value = result?.params?.get(def.key)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(def.label, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(
                            if (value != null) {
                                "${String.format(Locale.US, "%.1f", value)}/10 · ${def.weight}%"
                            } else {
                                "${def.weight}%"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text("LEVELS", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                levelNames.forEachIndexed { index, name ->
                    val level = index + 1
                    Text(
                        "Level $level - $name: x" + String.format(Locale.US, "%.2f", PrismScoring.levelFactor(level)),
                        fontSize = 11.sp
                    )
                }

                extraNote?.let { Text(it, fontSize = 11.sp, lineHeight = 16.sp) }

                if (result != null && result.tips.isNotEmpty()) {
                    Text("HOW TO GROW", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    result.tips.forEach { tip ->
                        Text(
                            "• ${tip.text.replaceFirstChar { it.uppercase() }}",
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    "An optional AI key can help read your text. The AI never sets the level.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    )
}

internal fun githubChipColorKey(chip: GithubChip): Int = when (chip) {
    GithubChip.VERIFIED -> 0
    GithubChip.NOT_FOUND, GithubChip.NOT_YOURS -> 2
    else -> 1
}

@Composable
internal fun githubChipColor(chip: GithubChip): Color = when (githubChipColorKey(chip)) {
    0 -> PrismAccent.green()
    2 -> PrismAccent.red()
    else -> PrismAccent.amber()
}

@Composable
internal fun liveChipColor(chip: LiveChip): Color = when (chip) {
    LiveChip.WORKS, LiveChip.STORE_WORKS -> PrismAccent.green()
    LiveChip.NOT_REACHABLE -> PrismAccent.red()
    else -> PrismAccent.amber()
}
