package com.prismorbit.app

import com.prismorbit.app.PrismScoring.GithubStatus
import com.prismorbit.app.PrismScoring.InternshipScoreInput
import com.prismorbit.app.PrismScoring.LiveStatus
import com.prismorbit.app.PrismScoring.ProjectScoreInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismScoringTest {

    // ---------------- readiness ----------------

    @Test
    fun readinessUsesFixedWeights() {
        // 100 in every part = 100
        assertEquals(100, PrismScoring.readiness(100, 100, 100, 100, 100, 100))
        // only academics: 15%
        assertEquals(15, PrismScoring.readiness(100, 0, 0, 0, 0, 0))
        // only DSA: 25%
        assertEquals(25, PrismScoring.readiness(0, 100, 0, 0, 0, 0))
        // only projects: 20%
        assertEquals(20, PrismScoring.readiness(0, 0, 100, 0, 0, 0))
        // only internships 15%, skills 15%, evidence 10%
        assertEquals(15, PrismScoring.readiness(0, 0, 0, 100, 0, 0))
        assertEquals(15, PrismScoring.readiness(0, 0, 0, 0, 100, 0))
        assertEquals(10, PrismScoring.readiness(0, 0, 0, 0, 0, 100))
        assertEquals(0, PrismScoring.readiness(0, 0, 0, 0, 0, 0))
    }

    @Test
    fun weightsAddUpToOne() {
        val total = PrismScoring.WEIGHT_ACADEMICS + PrismScoring.WEIGHT_DSA +
                PrismScoring.WEIGHT_PROJECTS + PrismScoring.WEIGHT_INTERNSHIPS +
                PrismScoring.WEIGHT_SKILLS + PrismScoring.WEIGHT_EVIDENCE
        assertEquals(1.0, total, 1e-9)
    }

    @Test
    fun breakdownReadinessMatchesFunction() {
        val b = PrismScoring.Breakdown(87, 40, 60, 30, 70, 20)
        assertEquals(PrismScoring.readiness(87, 40, 60, 30, 70, 20), b.readiness)
    }

    // ---------------- academics / skills ----------------

    @Test
    fun academicsIsCgpaTimesTen() {
        assertEquals(87, PrismScoring.academicsScore(8.7))
        assertEquals(0, PrismScoring.academicsScore(null))
        assertEquals(100, PrismScoring.academicsScore(12.0))
        assertEquals(0, PrismScoring.academicsScore(-1.0))
    }

    @Test
    fun skillsIsAverageRatingTimesTen() {
        assertEquals(70, PrismScoring.skillsScore(listOf(6.0, 8.0)))
        assertEquals(0, PrismScoring.skillsScore(emptyList()))
        // out-of-range ratings are ignored
        assertEquals(50, PrismScoring.skillsScore(listOf(5.0, 99.0)))
    }

    // ---------------- DSA ----------------

    @Test
    fun dsaPointsUseDifficultyAndTopic() {
        assertEquals(1.0, PrismScoring.dsaProblemPoints("Easy", "Arrays"), 1e-9)
        assertEquals(2.0, PrismScoring.dsaProblemPoints("Medium", "Strings"), 1e-9)
        assertEquals(3.3, PrismScoring.dsaProblemPoints("Hard", "Hashing"), 1e-9)
        assertEquals(2.4, PrismScoring.dsaProblemPoints("Medium", "Heap"), 1e-9)
        assertEquals(3.9, PrismScoring.dsaProblemPoints("Hard", "Graphs"), 1e-9)
        assertEquals(4.2, PrismScoring.dsaProblemPoints("Hard", "Dynamic Programming"), 1e-9)
        assertEquals(1.0, PrismScoring.dsaProblemPoints("Easy", "Something else"), 1e-9)
        assertEquals(1.3, PrismScoring.dsaProblemPoints("Easy", "Tries"), 1e-9)
        assertEquals(1.2, PrismScoring.dsaProblemPoints("Easy", "Bit Manipulation"), 1e-9)
    }

    @Test
    fun storedDsaScoreComesFirst() {
        assertEquals(9.0, PrismScoring.dsaPoints(9.0, "Easy", "Arrays"), 1e-9)
        assertEquals(2.2, PrismScoring.dsaPoints(null, "Medium", "Linked List"), 1e-9)
    }

    @Test
    fun dsaScoreIsPointsOver1000AndCapped() {
        assertEquals(0, PrismScoring.dsaScore(0.0))
        assertEquals(10, PrismScoring.dsaScore(100.0))
        assertEquals(100, PrismScoring.dsaScore(1000.0))
        assertEquals(100, PrismScoring.dsaScore(5000.0))
    }

    // ---------------- evidence ----------------

    @Test
    fun countCurveIsSmooth() {
        assertEquals(0.0, PrismScoring.countCurve(0), 1e-9)
        assertEquals(50.0, PrismScoring.countCurve(4), 1e-9)
        assertEquals(20.0, PrismScoring.countCurve(1), 1e-9)
        assertTrue(PrismScoring.countCurve(100) < 100.0)
    }

    @Test
    fun learningUsesAverageLevelOrCountCurve() {
        assertEquals(60, PrismScoring.learningScore(listOf(5.0, 7.0), 2))
        assertEquals(0, PrismScoring.learningScore(emptyList(), 0))
        // records with no levels fall back to the curve: 4 records -> 50
        assertEquals(50, PrismScoring.learningScore(emptyList(), 4))
    }

    @Test
    fun evidenceIs40_30_30() {
        // 4 achievements = 50 (x0.4 = 20), 4 certs = 50 (x0.3 = 15), learning 100 x 0.3 = 30
        assertEquals(65, PrismScoring.evidenceScore(4, 4, 100))
        assertEquals(0, PrismScoring.evidenceScore(0, 0, 0))
        assertEquals(30, PrismScoring.evidenceScore(0, 0, 100))
    }

    // ---------------- levels ----------------

    @Test
    fun levelMapping() {
        assertEquals(1, PrismScoring.levelFromScore(0.0))
        assertEquals(1, PrismScoring.levelFromScore(24.0))
        assertEquals(2, PrismScoring.levelFromScore(25.0))
        assertEquals(2, PrismScoring.levelFromScore(44.0))
        assertEquals(3, PrismScoring.levelFromScore(45.0))
        assertEquals(3, PrismScoring.levelFromScore(64.0))
        assertEquals(4, PrismScoring.levelFromScore(65.0))
        assertEquals(4, PrismScoring.levelFromScore(84.0))
        assertEquals(5, PrismScoring.levelFromScore(85.0))
        assertEquals(5, PrismScoring.levelFromScore(100.0))
        assertEquals(1, PrismScoring.levelFromScore(-5.0))
        assertEquals(5, PrismScoring.levelFromScore(150.0))
    }

    @Test
    fun levelFactorsAndNames() {
        assertEquals(0.70, PrismScoring.levelFactor(1), 1e-9)
        assertEquals(0.85, PrismScoring.levelFactor(2), 1e-9)
        assertEquals(1.00, PrismScoring.levelFactor(3), 1e-9)
        assertEquals(1.25, PrismScoring.levelFactor(4), 1e-9)
        assertEquals(1.50, PrismScoring.levelFactor(5), 1e-9)
        assertEquals(1.50, PrismScoring.levelFactor(99), 1e-9)
        assertEquals(0.70, PrismScoring.levelFactor(-3), 1e-9)
        assertEquals("Practice", PrismScoring.projectLevelName(1))
        assertEquals("Professional", PrismScoring.projectLevelName(5))
        assertEquals("Small", PrismScoring.internshipLevelName(1))
        assertEquals("Top", PrismScoring.internshipLevelName(5))
        assertEquals("Level 4 - Advanced", PrismScoring.levelLabel(4, "Advanced"))
    }

    @Test
    fun statusGate() {
        assertEquals(1.0, PrismScoring.internshipGate("SELECTED"), 1e-9)
        assertEquals(1.0, PrismScoring.internshipGate("selected"), 1e-9)
        assertEquals(0.3, PrismScoring.internshipGate("APPLIED"), 1e-9)
        assertEquals(0.3, PrismScoring.internshipGate("INTERVIEW"), 1e-9)
        assertEquals(0.3, PrismScoring.internshipGate("INTERESTED"), 1e-9)
        // Level 5 (x1.5) with a 30% gate = x1.15
        assertEquals(1.15, PrismScoring.effectiveFactor(5, 0.3), 1e-9)
        // Level 1 (x0.7) with a 30% gate = x0.91
        assertEquals(0.91, PrismScoring.effectiveFactor(1, 0.3), 1e-9)
        assertEquals(1.5, PrismScoring.effectiveFactor(5, 1.0), 1e-9)
    }

    // ---------------- projects ----------------

    private val strongProject = ProjectScoreInput(
        status = "COMPLETED",
        progress = 100,
        techStackFilled = true,
        githubStatus = GithubStatus.VERIFIED,
        liveStatus = LiveStatus.WORKS,
        hasTasks = true
    )

    @Test
    fun projectBaseScoreAddsUpTo100() {
        assertEquals(100.0, PrismScoring.projectBaseScore(strongProject), 1e-9)
        assertEquals(0.0, PrismScoring.projectBaseScore(ProjectScoreInput()), 1e-9)
    }

    @Test
    fun projectLinkCredit() {
        val base = ProjectScoreInput(status = "IDEA", progress = 0)
        // verified: full 15
        assertEquals(15.0, PrismScoring.projectBaseScore(base.copy(githubStatus = GithubStatus.VERIFIED)), 1e-9)
        // saved but not verified, could not be checked, or not found: 40% = 6
        assertEquals(6.0, PrismScoring.projectBaseScore(base.copy(githubStatus = GithubStatus.UNCHECKED)), 1e-9)
        assertEquals(6.0, PrismScoring.projectBaseScore(base.copy(githubStatus = GithubStatus.COULD_NOT_CHECK)), 1e-9)
        assertEquals(6.0, PrismScoring.projectBaseScore(base.copy(githubStatus = GithubStatus.NOT_FOUND)), 1e-9)
        // not your repo and empty: 0
        assertEquals(0.0, PrismScoring.projectBaseScore(base.copy(githubStatus = GithubStatus.NOT_YOURS)), 1e-9)
        assertEquals(0.0, PrismScoring.projectBaseScore(base.copy(githubStatus = GithubStatus.NONE)), 1e-9)
        // live link
        assertEquals(15.0, PrismScoring.projectBaseScore(base.copy(liveStatus = LiveStatus.WORKS)), 1e-9)
        assertEquals(6.0, PrismScoring.projectBaseScore(base.copy(liveStatus = LiveStatus.NOT_REACHABLE)), 1e-9)
        assertEquals(6.0, PrismScoring.projectBaseScore(base.copy(liveStatus = LiveStatus.UNCHECKED)), 1e-9)
        assertEquals(0.0, PrismScoring.projectBaseScore(base.copy(liveStatus = LiveStatus.NONE)), 1e-9)
    }

    @Test
    fun projectProgressIsThirtyPercentOfProgress() {
        assertEquals(15.0, PrismScoring.projectBaseScore(ProjectScoreInput(progress = 50)), 1e-9)
        // progress above 100 is clamped
        assertEquals(30.0, PrismScoring.projectBaseScore(ProjectScoreInput(progress = 500)), 1e-9)
    }

    @Test
    fun projectLevelMultipliesAndClamps() {
        val mid = ProjectScoreInput(status = "ONGOING", progress = 60, techStackFilled = true) // 15 + 18 = 33
        assertEquals(33.0, PrismScoring.projectContribution(mid.copy(level = 3)), 1e-9)
        assertEquals(33.0 * 1.25, PrismScoring.projectContribution(mid.copy(level = 4)), 1e-9)
        assertEquals(33.0 * 0.70, PrismScoring.projectContribution(mid.copy(level = 1)), 1e-9)
        // a perfect project at level 5 is clamped to 100, never above
        assertEquals(100.0, PrismScoring.projectContribution(strongProject.copy(level = 5)), 1e-9)
    }

    @Test
    fun projectsScoreIsAverageOfContributions() {
        assertEquals(0, PrismScoring.projectsScore(emptyList()))
        val list = listOf(strongProject, ProjectScoreInput())
        assertEquals(50, PrismScoring.projectsScore(list))
    }

    @Test
    fun oneHighLevelProjectCannotDominate() {
        // 1 great project + 9 empty ones: the score stays low even at level 5
        val list = listOf(strongProject.copy(level = 5)) + List(9) { ProjectScoreInput() }
        assertEquals(10, PrismScoring.projectsScore(list))
    }

    @Test
    fun missingLevelCountsAsLevelThree() {
        val withDefault = ProjectScoreInput(status = "ONGOING", progress = 40)
        assertEquals(3, withDefault.level)
        assertEquals(
            PrismScoring.projectBaseScore(withDefault),
            PrismScoring.projectContribution(withDefault),
            1e-9
        )
    }

    // ---------------- internships ----------------

    @Test
    fun zeroInternshipsIsZeroEvenWithProjects() {
        assertEquals(0, PrismScoring.internshipsScore(emptyList(), 10))
    }

    @Test
    fun internshipBaseScore() {
        // 1 selected, complete, level 3: apps 1/10*25=2.5, interviews 1/5*20=4,
        // outcomes 1/2*20=10, projects 4/4*20=20, details 15 => 51.5 -> 52
        val one = listOf(InternshipScoreInput(status = "SELECTED", detailsComplete = true))
        assertEquals(52, PrismScoring.internshipsScore(one, 4))
    }

    @Test
    fun internshipFullMarks() {
        val ten = List(10) { InternshipScoreInput(status = "SELECTED", detailsComplete = true) }
        assertEquals(100, PrismScoring.internshipsScore(ten, 4))
    }

    @Test
    fun internshipLevelIsGatedByStatus() {
        val applied5 = listOf(InternshipScoreInput(status = "APPLIED", level = 5))
        val applied3 = listOf(InternshipScoreInput(status = "APPLIED", level = 3))
        val selected5 = listOf(InternshipScoreInput(status = "SELECTED", level = 5))
        val selected3 = listOf(InternshipScoreInput(status = "SELECTED", level = 3))

        // applied level 5 counts as x1.15 (not x1.5): apps = 1.15/10*25 = 2.875 -> 3
        assertEquals(3, PrismScoring.internshipsScore(applied5, 0))
        assertEquals(3, PrismScoring.internshipsScore(applied3, 0)) // 2.5 -> 3 (rounds half up)

        // selected level 5 counts fully: apps 3.75 + interviews 6 + outcomes 15 = 24.75 -> 25
        assertEquals(25, PrismScoring.internshipsScore(selected5, 0))
        // selected level 3: 2.5 + 4 + 10 = 16.5 -> 17 (roundToInt rounds half up)
        assertEquals(17, PrismScoring.internshipsScore(selected3, 0))
        assertTrue(
            PrismScoring.internshipsScore(selected5, 0) > PrismScoring.internshipsScore(applied5, 0)
        )
    }

    @Test
    fun internshipScoreStaysBetween0And100() {
        val many = List(30) { InternshipScoreInput(status = "SELECTED", detailsComplete = true, level = 5) }
        val score = PrismScoring.internshipsScore(many, 50)
        assertTrue(score in 0..100)
        assertEquals(100, score)
    }

    @Test
    fun interviewAndOutcomeUseGatedFactor() {
        // INTERVIEW level 1: factor = 1 - 0.3*0.3 = 0.91 -> interviews 0.91/5*20 = 3.64, apps 2.275
        val items = listOf(InternshipScoreInput(status = "INTERVIEW", level = 1))
        // 2.275 + 3.64 = 5.915 -> 6
        assertEquals(6, PrismScoring.internshipsScore(items, 0))
    }

    @Test
    fun detailsCompleteNeedsFourFields() {
        assertTrue(PrismScoring.internshipDetailsComplete("A", "B", "1/1/2025", "https://x"))
        assertTrue(!PrismScoring.internshipDetailsComplete("A", "", "1/1/2025", "https://x"))
        assertTrue(!PrismScoring.internshipDetailsComplete("A", "B", " ", "https://x"))
        assertTrue(!PrismScoring.internshipDetailsComplete("A", "B", "1/1/2025", ""))
    }
}
