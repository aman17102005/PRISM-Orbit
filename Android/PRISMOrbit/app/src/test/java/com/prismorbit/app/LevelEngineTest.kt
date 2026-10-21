package com.prismorbit.app

import com.prismorbit.app.PrismScoring.GithubStatus
import com.prismorbit.app.PrismScoring.LiveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LevelEngineTest {

    private val today = LocalDate.of(2026, 6, 1)

    private val goodEvidence = GithubEvidenceInput(
        isValidUrl = true, isPublic = true, isAccessible = true, hasReadme = true,
        readmeMeaningful = true, meaningfulCode = true, sourceSamples = 3, sourceFiles = 40,
        recentCommits = 10, languageCount = 2, sourceCharacters = 40000,
        detectedFeatures = setOf("auth", "database", "backend_api", "tests", "deployment"),
        ownerMatches = true
    )

    private val strong = ProjectLevelInput(
        name = "Campus Helper",
        description = "A mobile app that helps students find study partners. It solves the problem of lonely exam preparation. " +
                "Students can chat, share notes and schedule sessions with real users on campus.",
        techStack = "Kotlin, Jetpack Compose, Firebase",
        startDate = "01/01/2026",
        status = "COMPLETED",
        progress = 100,
        tasksTotal = 4, tasksDone = 4,
        liveStatus = LiveStatus.WORKS,
        isStoreLink = true,
        realUsers = RealUsersTier.FROM_100,
        teamSize = 4,
        featureChips = setOf("auth", "database"),
        evidence = goodEvidence,
        githubStatus = GithubStatus.VERIFIED
    )

    // ---------------- weights ----------------

    @Test
    fun projectWeightsAddUpTo100() {
        assertEquals(100, ProjectLevelEngine.PARAMETERS.sumOf { it.weight })
        assertEquals(100, InternshipLevelEngine.PARAMETERS.sumOf { it.weight })
        assertEquals(
            listOf(20, 20, 15, 15, 10, 10, 5, 5),
            ProjectLevelEngine.PARAMETERS.map { it.weight }
        )
        assertEquals(
            listOf(30, 20, 15, 10, 10, 5, 5, 5),
            InternshipLevelEngine.PARAMETERS.map { it.weight }
        )
    }

    // ---------------- project parameters ----------------

    @Test
    fun githubProofFullMarksAndNothing() {
        assertEquals(10.0, ProjectLevelEngine.githubProof(strong), 1e-9)
        assertEquals(0.0, ProjectLevelEngine.githubProof(ProjectLevelInput()), 0.0)
        // evidence that was not accessible counts nothing
        val failed = strong.copy(evidence = goodEvidence.copy(isAccessible = false))
        assertEquals(0.0, ProjectLevelEngine.githubProof(failed), 0.0)
    }

    @Test
    fun githubProofOwnerRules() {
        val unknown = strong.copy(evidence = goodEvidence.copy(ownerMatches = null))
        assertEquals(9.75, ProjectLevelEngine.githubProof(unknown), 1e-9) // half of the 5 owner points
        val different = strong.copy(evidence = goodEvidence.copy(ownerMatches = false))
        assertEquals(9.5, ProjectLevelEngine.githubProof(different), 1e-9)
        val notYours = strong.copy(githubStatus = GithubStatus.NOT_YOURS)
        assertEquals(0.0, ProjectLevelEngine.githubProof(notYours), 0.0) // no GitHub proof credit
    }

    @Test
    fun technicalDifficultyDetectedFullChipsHalf() {
        val detectedOnly = ProjectLevelInput(evidence = goodEvidence.copy(detectedFeatures = setOf("auth", "database")))
        assertEquals(4.0, ProjectLevelEngine.technicalDifficulty(detectedOnly), 1e-9)
        val chipsOnly = ProjectLevelInput(featureChips = setOf("auth", "database"))
        assertEquals(2.0, ProjectLevelEngine.technicalDifficulty(chipsOnly), 1e-9)
        // a chip that is also detected counts once, fully
        val both = ProjectLevelInput(
            evidence = goodEvidence.copy(detectedFeatures = setOf("auth")),
            featureChips = setOf("auth")
        )
        assertEquals(2.0, ProjectLevelEngine.technicalDifficulty(both), 1e-9)
        // 5 or more features = full
        assertEquals(10.0, ProjectLevelEngine.technicalDifficulty(strong.copy(featureChips = setOf("ai_ml", "payments", "realtime"))), 1e-9)
    }

    @Test
    fun realWorldUse() {
        assertEquals(6.0, ProjectLevelEngine.realWorldUse(ProjectLevelInput(liveStatus = LiveStatus.WORKS)), 1e-9)
        assertEquals(8.0, ProjectLevelEngine.realWorldUse(ProjectLevelInput(liveStatus = LiveStatus.WORKS, isStoreLink = true)), 1e-9)
        assertEquals(10.0, ProjectLevelEngine.realWorldUse(ProjectLevelInput(liveStatus = LiveStatus.WORKS, isStoreLink = true, realUsers = RealUsersTier.FROM_1000)), 1e-9)
        // not verified = low
        assertEquals(0.0, ProjectLevelEngine.realWorldUse(ProjectLevelInput(liveStatus = LiveStatus.NOT_REACHABLE)), 1e-9)
        assertEquals(0.0, ProjectLevelEngine.realWorldUse(ProjectLevelInput(liveStatus = LiveStatus.UNCHECKED, isStoreLink = true)), 1e-9)
        // the Deployed chip adds a little
        assertEquals(0.5, ProjectLevelEngine.realWorldUse(ProjectLevelInput(featureChips = setOf("deployment"))), 1e-9)
    }

    @Test
    fun problemValueNeutralLowAndHigh() {
        assertEquals(5.0, ProjectLevelEngine.problemValue(ProjectLevelInput(name = "Weather", description = "Shows the weather.")), 1e-9)
        assertTrue(ProjectLevelEngine.problemValue(ProjectLevelInput(name = "Todo app", description = "A simple todo list clone")) < 3.0)
        assertTrue(ProjectLevelEngine.problemValue(ProjectLevelInput(name = "Calculator", description = "practice project")) < 3.0)
        assertTrue(
            ProjectLevelEngine.problemValue(
                ProjectLevelInput(name = "Farm Help", description = "Helps farmers solve the problem of late rain alerts and saves time")
            ) > 6.0
        )
        // never above 8 and never below 1 without AI
        assertTrue(ProjectLevelEngine.problemValue(strong) <= 8.0)
    }

    @Test
    fun completenessBlend() {
        assertEquals(10.0, ProjectLevelEngine.completeness(ProjectLevelInput(status = "COMPLETED", progress = 100, tasksTotal = 2, tasksDone = 2)), 1e-9)
        assertEquals(0.9, ProjectLevelEngine.completeness(ProjectLevelInput(status = "IDEA", progress = 0)), 1e-9)
        assertTrue(
            ProjectLevelEngine.completeness(ProjectLevelInput(status = "ONGOING", progress = 50)) >
                    ProjectLevelEngine.completeness(ProjectLevelInput(status = "PLANNING", progress = 50))
        )
        val planning = ProjectLevelEngine.completeness(ProjectLevelInput(status = "PLANNING", progress = 0))
        val ongoing = ProjectLevelEngine.completeness(ProjectLevelInput(status = "ONGOING", progress = 0))
        assertTrue(planning < ongoing)
    }

    @Test
    fun effortScaleUsesDaysCommitsSizeTeam() {
        // 120+ days is full for the days part (40%)
        val daysOnly = ProjectLevelInput(startDate = "01/01/2026")
        assertEquals(4.0, ProjectLevelEngine.effortScale(daysOnly, today), 1e-9)
        val none = ProjectLevelInput()
        assertEquals(0.0, ProjectLevelEngine.effortScale(none, today), 1e-9)
        assertEquals(10.0, ProjectLevelEngine.effortScale(strong, today), 1e-9)
        // team of 4 or more = full team part (15%)
        assertEquals(1.5, ProjectLevelEngine.effortScale(ProjectLevelInput(teamSize = 4), today), 1e-9)
        assertEquals(0.0, ProjectLevelEngine.effortScale(ProjectLevelInput(teamSize = 1), today), 1e-9)
    }

    @Test
    fun stackRelevance() {
        assertEquals(10.0, ProjectLevelEngine.stackRelevance("Kotlin, Jetpack Compose, Firebase"), 1e-9)
        assertEquals(5.0, ProjectLevelEngine.stackRelevance("Kotlin, MyCustomTool"), 1e-9)
        assertEquals(0.0, ProjectLevelEngine.stackRelevance(""), 0.0)
        assertEquals(0.0, ProjectLevelEngine.stackRelevance("zzzzz"), 0.0)
        assertEquals(10.0, ProjectLevelEngine.stackRelevance("React and Node.js"), 1e-9)
    }

    @Test
    fun descriptionClarity() {
        assertEquals(0.0, ProjectLevelEngine.descriptionClarity(""), 0.0)
        assertEquals(0.0, ProjectLevelEngine.descriptionClarity("short"), 0.0)
        val good = "This app helps students plan study time. It sends reminders before exams. It also tracks progress each week."
        assertTrue(ProjectLevelEngine.descriptionClarity(good) > 7.0)
        val huge = "word ".repeat(1000)
        assertTrue(ProjectLevelEngine.descriptionClarity(huge) < ProjectLevelEngine.descriptionClarity(good))
    }

    // ---------------- dates ----------------

    @Test
    fun dateParsing() {
        assertEquals(LocalDate.of(2026, 1, 5), ProjectLevelEngine.parseDate("05/01/2026"))
        assertEquals(LocalDate.of(2026, 1, 5), ProjectLevelEngine.parseDate("5-1-2026"))
        assertEquals(LocalDate.of(2026, 1, 5), ProjectLevelEngine.parseDate("2026-01-05"))
        assertEquals(null, ProjectLevelEngine.parseDate("31/02/2026"))
        assertEquals(null, ProjectLevelEngine.parseDate("soon"))
        assertEquals(0L, ProjectLevelEngine.daysSince("01/06/2027", today)) // a future date counts as 0
        assertEquals(151L, ProjectLevelEngine.daysSince("01/01/2026", today))
    }

    // ---------------- whole project level ----------------

    @Test
    fun strongProjectIsHighLevel() {
        val result = ProjectLevelEngine.compute(strong, today)
        assertTrue("score ${result.score}", result.score >= 65)
        assertTrue(result.level >= 4)
        assertEquals("rules", result.source)
        assertEquals(2, result.tips.size)
    }

    @Test
    fun emptyProjectIsLevelOne() {
        val result = ProjectLevelEngine.compute(ProjectLevelInput(name = "x"), today)
        assertEquals(1, result.level)
        assertEquals("Practice", result.name)
        assertTrue(result.reason.isNotBlank())
        assertTrue(result.tips.first().text.startsWith("To reach Level 2"))
    }

    @Test
    fun tipsAreTheTwoLowestParameters() {
        val result = ProjectLevelEngine.compute(strong.copy(liveStatus = LiveStatus.NONE, isStoreLink = false, realUsers = RealUsersTier.NONE), today)
        val lowest = result.params.entries.sortedBy { it.value }.take(2).map { it.key }.toSet()
        assertEquals(lowest, result.tips.map { it.key }.toSet())
    }

    // ---------------- AI merge rules ----------------

    @Test
    fun aiCanOnlyMoveAllowedParameters() {
        val rules = ProjectLevelEngine.rulesParameters(strong, today)
        val merged = ProjectLevelEngine.mergeAi(
            rules,
            mapOf(
                ProjectLevelEngine.GITHUB_PROOF to 0.0,        // evidence: never set by AI
                ProjectLevelEngine.REAL_WORLD_USE to 0.0,      // evidence: never set by AI
                ProjectLevelEngine.PROBLEM_VALUE to 9.0,
                ProjectLevelEngine.DESCRIPTION_CLARITY to 2.0,
                ProjectLevelEngine.COMPLETENESS to 0.0
            )
        )
        assertEquals(rules[ProjectLevelEngine.GITHUB_PROOF], merged[ProjectLevelEngine.GITHUB_PROOF])
        assertEquals(rules[ProjectLevelEngine.REAL_WORLD_USE], merged[ProjectLevelEngine.REAL_WORLD_USE])
        assertEquals(rules[ProjectLevelEngine.COMPLETENESS], merged[ProjectLevelEngine.COMPLETENESS])
        assertEquals(9.0, merged[ProjectLevelEngine.PROBLEM_VALUE]!!, 1e-9)
        assertEquals(2.0, merged[ProjectLevelEngine.DESCRIPTION_CLARITY]!!, 1e-9)
    }

    @Test
    fun aiTechnicalDifficultyMovesAtMostTwoPoints() {
        val rules = mapOf(ProjectLevelEngine.TECHNICAL_DIFFICULTY to 4.0)
        assertEquals(6.0, ProjectLevelEngine.mergeAi(rules, mapOf(ProjectLevelEngine.TECHNICAL_DIFFICULTY to 10.0))[ProjectLevelEngine.TECHNICAL_DIFFICULTY]!!, 1e-9)
        assertEquals(2.0, ProjectLevelEngine.mergeAi(rules, mapOf(ProjectLevelEngine.TECHNICAL_DIFFICULTY to 0.0))[ProjectLevelEngine.TECHNICAL_DIFFICULTY]!!, 1e-9)
        assertEquals(5.0, ProjectLevelEngine.mergeAi(rules, mapOf(ProjectLevelEngine.TECHNICAL_DIFFICULTY to 5.0))[ProjectLevelEngine.TECHNICAL_DIFFICULTY]!!, 1e-9)
        assertEquals(10.0, ProjectLevelEngine.mergeAi(mapOf(ProjectLevelEngine.TECHNICAL_DIFFICULTY to 9.5), mapOf(ProjectLevelEngine.TECHNICAL_DIFFICULTY to 99.0))[ProjectLevelEngine.TECHNICAL_DIFFICULTY]!!, 1e-9)
    }

    @Test
    fun aiSourceAndSummaryAreUsedOnlyWhenAiChangedSomething() {
        val withAi = ProjectLevelEngine.compute(strong, today, mapOf(ProjectLevelEngine.PROBLEM_VALUE to 9.0), "Solves a real student problem")
        assertEquals("ai+rules", withAi.source)
        assertEquals("Solves a real student problem", withAi.reason)
        val without = ProjectLevelEngine.compute(strong, today, null, "ignored summary")
        assertEquals("rules", without.source)
        assertTrue(without.reason != "ignored summary")
    }

    @Test
    fun levelIsAlwaysFromWeightedScoreNotFromAiText() {
        // even if an answer tries to say "level 5", only allowed parameters count
        val result = ProjectLevelEngine.compute(ProjectLevelInput(name = "x"), today, mapOf("level" to 10.0, "levelFinal" to 5.0), "Level 5!")
        assertEquals(1, result.level)
    }

    // ---------------- internships ----------------

    @Test
    fun durationParsing() {
        assertEquals(90.0, InternshipLevelEngine.parseDurationDays("3 months")!!, 1e-9)
        assertEquals(180.0, InternshipLevelEngine.parseDurationDays("6 Months")!!, 1e-9)
        assertEquals(56.0, InternshipLevelEngine.parseDurationDays("8 weeks")!!, 1e-9)
        assertEquals(45.0, InternshipLevelEngine.parseDurationDays("1.5 months")!!, 1e-9)
        assertEquals(75.0, InternshipLevelEngine.parseDurationDays("2-3 months")!!, 1e-9)
        assertEquals(10.0, InternshipLevelEngine.parseDurationDays("10 days")!!, 1e-9)
        assertEquals(14.0, InternshipLevelEngine.parseDurationDays("2 wks")!!, 1e-9)
        assertEquals(null, InternshipLevelEngine.parseDurationDays(""))
        assertEquals(null, InternshipLevelEngine.parseDurationDays("a while"))
    }

    @Test
    fun durationScoreBands() {
        assertEquals(1.0, InternshipLevelEngine.durationScore("1 week"), 1e-9)   // under 2 weeks = 10%
        assertEquals(4.0, InternshipLevelEngine.durationScore("3 weeks"), 1e-9)  // 2-4 weeks = 40%
        assertEquals(4.0, InternshipLevelEngine.durationScore("4 weeks"), 1e-9)
        assertEquals(7.0, InternshipLevelEngine.durationScore("1 month"), 1e-9)  // 1-2 months = 70%
        assertEquals(7.0, InternshipLevelEngine.durationScore("2 months"), 1e-9)
        assertEquals(10.0, InternshipLevelEngine.durationScore("3 months"), 1e-9) // 3+ months = 100%
        assertEquals(10.0, InternshipLevelEngine.durationScore("1 year"), 1e-9)
        assertEquals(0.0, InternshipLevelEngine.durationScore("unknown"), 1e-9)
    }

    @Test
    fun paidScoreNeverJudgesAmounts() {
        assertEquals(0.0, InternshipLevelEngine.paidScore(""), 0.0)
        assertEquals(0.0, InternshipLevelEngine.paidScore("0"), 0.0)
        assertEquals(0.0, InternshipLevelEngine.paidScore("Unpaid"), 0.0)
        assertEquals(0.0, InternshipLevelEngine.paidScore("unpaid internship"), 0.0)
        assertEquals(0.0, InternshipLevelEngine.paidScore("₹0"), 0.0)
        assertEquals(0.0, InternshipLevelEngine.paidScore("none"), 0.0)
        assertEquals(10.0, InternshipLevelEngine.paidScore("5000"), 0.0)
        assertEquals(10.0, InternshipLevelEngine.paidScore("1"), 0.0)       // any amount is the same
        assertEquals(10.0, InternshipLevelEngine.paidScore("100000/month"), 0.0)
        assertEquals(10.0, InternshipLevelEngine.paidScore("Performance based"), 0.0)
    }

    @Test
    fun workModeShares() {
        assertEquals(1.0, WorkMode.share("ONSITE"), 0.0)
        assertEquals(0.7, WorkMode.share("hybrid"), 0.0)
        assertEquals(0.5, WorkMode.share("REMOTE"), 0.0)
        assertEquals(0.0, WorkMode.share(""), 0.0)
    }

    @Test
    fun companyStrengthChipAndList() {
        fun input(company: String, type: String = "") = InternshipLevelInput(company = company, companyType = type)
        assertEquals(10.0, InternshipLevelEngine.companyStrength(input("Google India Pvt Ltd")), 1e-9)
        assertEquals(2.0, InternshipLevelEngine.companyStrength(input("Ravi Web Studio")), 1e-9) // unknown, no chip = Local
        assertEquals(5.0, InternshipLevelEngine.companyStrength(input("Ravi Web Studio", "STARTUP")), 1e-9)
        assertEquals(7.0, InternshipLevelEngine.companyStrength(input("Ravi Web Studio", "MEDIUM")), 1e-9)
        assertEquals(10.0, InternshipLevelEngine.companyStrength(input("Ravi Web Studio", "BIG")), 1e-9)
        // the higher of the list and the chip
        assertEquals(10.0, InternshipLevelEngine.companyStrength(input("Microsoft", "LOCAL")), 1e-9)
    }

    @Test
    fun proofScoreStates() {
        fun input(status: String, link: Boolean = false) =
            InternshipLevelInput(proof = ProofSummary(status = status), hasProofLink = link)
        assertEquals(10.0, InternshipLevelEngine.proofScore(input("OK")), 1e-9)
        assertEquals(6.0, InternshipLevelEngine.proofScore(input("PARTIAL")), 1e-9)
        assertEquals(5.0, InternshipLevelEngine.proofScore(input("", true)), 1e-9)
        assertEquals(5.0, InternshipLevelEngine.proofScore(input("UNREADABLE", true)), 1e-9)
        assertEquals(0.0, InternshipLevelEngine.proofScore(input("")), 1e-9)
        assertEquals(0.0, InternshipLevelEngine.proofScore(input("UNREADABLE")), 1e-9)
        assertEquals(10.0, InternshipLevelEngine.proofScore(input("OK", true)), 1e-9)
    }

    @Test
    fun selectionDifficultyUsesDatesAndStatus() {
        assertEquals(0.0, InternshipLevelEngine.selectionDifficulty(InternshipLevelInput()), 1e-9)
        assertEquals(3.5, InternshipLevelEngine.selectionDifficulty(InternshipLevelInput(interviewDate = "1/1/2026")), 1e-9)
        assertEquals(10.0, InternshipLevelEngine.selectionDifficulty(InternshipLevelInput(interviewDate = "1", offerDate = "2", status = "SELECTED")), 1e-9)
        assertEquals(1.5, InternshipLevelEngine.selectionDifficulty(InternshipLevelInput(status = "INTERVIEW")), 1e-9)
    }

    @Test
    fun roleDepthAndResultImpact() {
        assertEquals(0.0, InternshipLevelEngine.roleWorkDepth(InternshipLevelInput()), 0.0)
        val deep = InternshipLevelInput(
            responsibilities = "Built the Android login flow using Kotlin and Firebase and wrote REST API calls with unit tests. " +
                    "Fixed many bugs in the backend database layer and deployed new builds using git and docker every week for the team.",
            problemSolved = "Slow loading screens because the app loaded all data at once; I added paging and caching to the database queries.",
            technicalSkillsUsed = "Kotlin, Firebase, REST, Git, Docker, SQL"
        )
        assertTrue(InternshipLevelEngine.roleWorkDepth(deep) > 8.0)
        assertTrue(InternshipLevelEngine.roleWorkDepth(InternshipLevelInput(responsibilities = "helped")) < 1.0)

        assertEquals(0.0, InternshipLevelEngine.resultImpact(InternshipLevelInput()), 0.0)
        assertTrue(InternshipLevelEngine.resultImpact(InternshipLevelInput(outcomeImpact = "Reduced app start time by 40% and shipped the feature to 5000 users, which improved ratings.")) > 8.0)
        assertTrue(InternshipLevelEngine.resultImpact(InternshipLevelInput(outcomeImpact = "good")) < 2.0)
    }

    @Test
    fun internshipLevelNamesAndMapping() {
        val top = InternshipLevelInput(
            company = "Google", companyType = "BIG", role = "SDE Intern", duration = "6 months",
            technicalSkillsUsed = "Kotlin, Firebase, REST, Git, Docker",
            responsibilities = "Built Android features with Kotlin and Firebase, wrote REST API tests, fixed backend database bugs and used git and docker for deploy every week. ".repeat(2),
            problemSolved = "Slow startup in the Android app.",
            outcomeImpact = "Reduced startup time by 40% and shipped to 10000 users.",
            interviewDate = "1/1/2026", offerDate = "2/2/2026", stipend = "50000", status = "SELECTED",
            workMode = "ONSITE", proof = ProofSummary(status = "OK")
        )
        val result = InternshipLevelEngine.compute(top)
        assertEquals(5, result.level)
        assertEquals("Top", result.name)
        assertEquals("Level 5 - Top", result.label)

        val weak = InternshipLevelEngine.compute(InternshipLevelInput(company = "Local Shop"))
        assertEquals(1, weak.level)
        assertEquals("Small", weak.name)
    }

    @Test
    fun internshipAiOnlyMovesWorkAndResult() {
        val rules = InternshipLevelEngine.rulesParameters(InternshipLevelInput(company = "Local Shop"))
        val merged = InternshipLevelEngine.mergeAi(
            rules,
            mapOf(
                InternshipLevelEngine.ROLE_WORK_DEPTH to 9.0,
                InternshipLevelEngine.RESULT_IMPACT to 8.0,
                InternshipLevelEngine.COMPANY_STRENGTH to 10.0,
                InternshipLevelEngine.PROOF to 10.0,
                InternshipLevelEngine.DURATION to 10.0
            )
        )
        assertEquals(9.0, merged[InternshipLevelEngine.ROLE_WORK_DEPTH]!!, 1e-9)
        assertEquals(8.0, merged[InternshipLevelEngine.RESULT_IMPACT]!!, 1e-9)
        assertEquals(rules[InternshipLevelEngine.COMPANY_STRENGTH], merged[InternshipLevelEngine.COMPANY_STRENGTH])
        assertEquals(rules[InternshipLevelEngine.PROOF], merged[InternshipLevelEngine.PROOF])
        assertEquals(rules[InternshipLevelEngine.DURATION], merged[InternshipLevelEngine.DURATION])
    }
}
