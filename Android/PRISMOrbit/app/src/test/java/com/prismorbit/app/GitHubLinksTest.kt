package com.prismorbit.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubLinksTest {

    @Test
    fun parsesRepositoryLinks() {
        assertEquals("aman" to "app", GitHubUrls.parseRepository("https://github.com/aman/app"))
        assertEquals("aman" to "app", GitHubUrls.parseRepository("https://github.com/aman/app.git"))
        assertEquals("aman" to "app", GitHubUrls.parseRepository("github.com/aman/app/tree/main/src"))
        assertEquals("aman" to "app", GitHubUrls.parseRepository("https://www.github.com/aman/app/?tab=readme"))
        assertNull(GitHubUrls.parseRepository("https://gitlab.com/aman/app"))
        assertNull(GitHubUrls.parseRepository("https://github.com/aman"))
        assertNull(GitHubUrls.parseRepository(""))
        assertNull(GitHubUrls.parseRepository("not a url at all"))
    }

    @Test
    fun parsesProfileOwner() {
        assertEquals("aman", GitHubUrls.parseProfileOwner("https://github.com/aman"))
        assertEquals("aman", GitHubUrls.parseProfileOwner("https://github.com/aman/"))
        assertEquals("aman", GitHubUrls.parseProfileOwner("github.com/aman?tab=repositories"))
        assertNull(GitHubUrls.parseProfileOwner(""))
        assertNull(GitHubUrls.parseProfileOwner("https://example.com/aman"))
    }

    @Test
    fun ownerMatchIgnoresCase() {
        assertEquals(true, GitHubUrls.ownerMatches("Aman17102005", "https://github.com/aman17102005"))
        assertEquals(false, GitHubUrls.ownerMatches("someoneelse", "https://github.com/aman17102005"))
        // profile URL missing -> unknown
        assertNull(GitHubUrls.ownerMatches("aman", ""))
        assertNull(GitHubUrls.ownerMatches("aman", "https://example.com/x"))
    }

    @Test
    fun recognisesStoreLinks() {
        assertTrue(LiveLinks.isStoreLink("https://play.google.com/store/apps/details?id=com.x"))
        assertTrue(LiveLinks.isStoreLink("https://apps.apple.com/in/app/x/id123"))
        assertTrue(LiveLinks.isStoreLink("apps.apple.com/app/x"))
        assertFalse(LiveLinks.isStoreLink("https://myapp.vercel.app"))
        assertFalse(LiveLinks.isStoreLink(""))
    }

    @Test
    fun webUrlCheck() {
        assertTrue(LiveLinks.isWebUrl("https://example.com"))
        assertTrue(LiveLinks.isWebUrl("example.com/page"))
        assertFalse(LiveLinks.isWebUrl("ftp://example.com"))
        assertFalse(LiveLinks.isWebUrl("not a link"))
    }

    // ---------- owner check on saved evidence ----------

    private val evidence = mapOf<String, Any?>("isAccessible" to true, "owner" to "Aman", "isValidUrl" to true)

    @Test
    fun ownerCheckMatches() {
        val result = ProjectVerifier.applyOwnerCheck(evidence, "https://github.com/aman")
        assertEquals("yes", result["ownerMatch"])
        assertEquals("VERIFIED", result["checkStatus"])
    }

    @Test
    fun ownerCheckNotYourRepo() {
        val result = ProjectVerifier.applyOwnerCheck(evidence, "https://github.com/other")
        assertEquals("no", result["ownerMatch"])
        assertEquals("NOT_YOURS", result["checkStatus"])
        assertEquals(PrismScoring.GithubStatus.NOT_YOURS, GithubEvidenceInput.githubStatus("https://github.com/Aman/x", result))
        assertEquals(GithubChip.NOT_YOURS, ProjectVerifier.githubChip("https://github.com/Aman/x", result))
    }

    @Test
    fun ownerCheckProfileMissingIsNeutral() {
        val result = ProjectVerifier.applyOwnerCheck(evidence, "")
        assertEquals("unknown", result["ownerMatch"])
        assertEquals("VERIFIED", result["checkStatus"])
        assertTrue(ProjectVerifier.needsProfileUrlNote("https://github.com/Aman/x", result))
        // half credit for the owner part of the GitHub proof
        val input = GithubEvidenceInput.fromMap(result)!!
        assertNull(input.ownerMatches)
    }

    @Test
    fun inaccessibleEvidenceIsLeftAlone() {
        val failed = mapOf<String, Any?>("isAccessible" to false)
        assertEquals(failed, ProjectVerifier.applyOwnerCheck(failed, "https://github.com/x"))
        assertEquals(emptyMap<String, Any?>(), ProjectVerifier.applyOwnerCheck(emptyMap(), "https://github.com/x"))
    }

    // ---------- chips and status ----------

    @Test
    fun githubChips() {
        val url = "https://github.com/a/b"
        assertEquals(GithubChip.NONE, ProjectVerifier.githubChip("", emptyMap()))
        assertEquals(GithubChip.NOT_CHECKED, ProjectVerifier.githubChip(url, emptyMap()))
        assertEquals(GithubChip.VERIFIED, ProjectVerifier.githubChip(url, mapOf("checkStatus" to "VERIFIED")))
        assertEquals(GithubChip.NOT_FOUND, ProjectVerifier.githubChip(url, mapOf("checkStatus" to "NOT_FOUND")))
        assertEquals(GithubChip.COULD_NOT_CHECK, ProjectVerifier.githubChip(url, mapOf("checkStatus" to "COULD_NOT_CHECK")))
        // rate limit after a good check: the last good proof is kept but the chip says "try later"
        val keep = mapOf<String, Any?>("checkStatus" to "VERIFIED", "isAccessible" to true, "lastAttempt" to "COULD_NOT_CHECK")
        assertEquals(GithubChip.COULD_NOT_CHECK, ProjectVerifier.githubChip(url, keep))
        assertEquals(PrismScoring.GithubStatus.VERIFIED, GithubEvidenceInput.githubStatus(url, keep))
    }

    @Test
    fun oldEvidenceFormatStillWorks() {
        val url = "https://github.com/a/b"
        // written by the old portfolio screen: no checkStatus
        assertEquals(
            PrismScoring.GithubStatus.VERIFIED,
            GithubEvidenceInput.githubStatus(url, mapOf("isAccessible" to true))
        )
        assertEquals(
            PrismScoring.GithubStatus.NOT_FOUND,
            GithubEvidenceInput.githubStatus(url, mapOf("isAccessible" to false, "errorMessage" to "GitHub API 404: Not Found"))
        )
        assertEquals(
            PrismScoring.GithubStatus.COULD_NOT_CHECK,
            GithubEvidenceInput.githubStatus(url, mapOf("isAccessible" to false, "errorMessage" to "GitHub API 403: rate limit"))
        )
        assertEquals(PrismScoring.GithubStatus.NONE, GithubEvidenceInput.githubStatus("", mapOf("isAccessible" to true)))
        assertEquals(PrismScoring.GithubStatus.UNCHECKED, GithubEvidenceInput.githubStatus(url, null))
    }

    @Test
    fun liveChips() {
        val url = "https://x.com"
        assertEquals(LiveChip.NONE, ProjectVerifier.liveChip("", emptyMap()))
        assertEquals(LiveChip.NOT_CHECKED, ProjectVerifier.liveChip(url, emptyMap()))
        assertEquals(LiveChip.WORKS, ProjectVerifier.liveChip(url, mapOf("status" to "WORKS")))
        assertEquals(LiveChip.STORE_WORKS, ProjectVerifier.liveChip(url, mapOf("status" to "WORKS", "isStore" to true)))
        assertEquals(LiveChip.NOT_REACHABLE, ProjectVerifier.liveChip(url, mapOf("status" to "NOT_REACHABLE")))
        assertEquals(PrismScoring.LiveStatus.WORKS, PrismScoreLoader.liveStatus(url, mapOf("status" to "WORKS")))
        assertEquals(PrismScoring.LiveStatus.UNCHECKED, PrismScoreLoader.liveStatus(url, null))
        assertEquals(PrismScoring.LiveStatus.NONE, PrismScoreLoader.liveStatus("", mapOf("status" to "WORKS")))
    }

    // ---------- feature detection ----------

    @Test
    fun detectsFeaturesFromPathsAndManifests() {
        val paths = listOf(
            "package.json", "Dockerfile", "server/index.js", "src/__tests__/app.test.js",
            "src/components/Login.jsx", "prisma/schema.prisma"
        )
        val manifests = mapOf("package.json" to """{"dependencies": {"express": "^4", "stripe": "^1", "socket.io": "^4", "openai": "^4"}}""")
        val found = ProjectFeatureDetector.detect(paths, manifests)
        assertTrue(ProjectFeatures.AUTH in found)
        assertTrue(ProjectFeatures.DATABASE in found)
        assertTrue(ProjectFeatures.BACKEND_API in found)
        assertTrue(ProjectFeatures.AI_ML in found)
        assertTrue(ProjectFeatures.PAYMENTS in found)
        assertTrue(ProjectFeatures.REALTIME in found)
        assertTrue(ProjectFeatures.DEPLOYMENT in found)
        assertTrue(ProjectFeatures.TESTS in found)
        assertFalse(ProjectFeatures.MOBILE_APP in found)
    }

    @Test
    fun detectsAndroidApp() {
        val paths = listOf("app/src/main/AndroidManifest.xml", "app/build.gradle.kts", "app/src/test/java/ExampleTest.kt")
        val manifests = mapOf("app/build.gradle.kts" to "implementation(\"com.google.firebase:firebase-firestore\")\nid(\"com.android.application\")")
        val found = ProjectFeatureDetector.detect(paths, manifests)
        assertTrue(ProjectFeatures.MOBILE_APP in found)
        assertTrue(ProjectFeatures.DATABASE in found)
        assertTrue(ProjectFeatures.TESTS in found)
    }

    @Test
    fun emptyRepoHasNoFeatures() {
        assertTrue(ProjectFeatureDetector.detect(listOf("README.md", "notes.txt"), emptyMap()).isEmpty())
    }

    @Test
    fun picksFewManifests() {
        val paths = listOf("package.json", "app/build.gradle.kts", "build.gradle.kts", "requirements.txt", "web/package.json")
        val picked = ProjectFeatureDetector.pickManifests(paths, limit = 2)
        assertEquals(2, picked.size)
        assertEquals("package.json", picked[0])
    }
}
