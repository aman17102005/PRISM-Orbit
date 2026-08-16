package com.prismorbit.app

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GitHubAnalyzerTest {

    @Test
    fun analyzePublicPrismOrbitRepository() = runBlocking {

        val repositoryUrl =
            "https://github.com/aman17102005/PRISM-Orbit"

        val evidence =
            GitHubAnalyzer.analyzeRepository(repositoryUrl)

        Log.d(
            "PRISM_GITHUB_TEST",
            """
            ================================
            GITHUB ANALYZER TEST — G1
            ================================

            Valid URL:
            ${evidence.isValidUrl}

            Public:
            ${evidence.isPublic}

            Accessible:
            ${evidence.isAccessible}

            Owner:
            ${evidence.owner}

            Repository:
            ${evidence.repository}

            Default Branch:
            ${evidence.defaultBranch}

            Description:
            ${evidence.description}

            Stars:
            ${evidence.stars}

            Forks:
            ${evidence.forks}

            README Exists:
            ${evidence.hasReadme}

            README Content Length:
            ${evidence.readmeContentLength}

            README Meaningful:
            ${evidence.readmeHasMeaningfulContent}

            Total Files:
            ${evidence.totalFileCount}

            Source Files:
            ${evidence.sourceFileCount}

            Languages:
            ${evidence.languages}

            Recent Commit Sample:
            ${evidence.recentCommitSampleSize}

            Source Content Samples:
            ${evidence.sourceContentSampleCount}

            Non-empty Source Samples:
            ${evidence.sourceContentNonEmptyCount}

            Source Characters:
            ${evidence.sourceContentCharacters}

            Meaningful Source Code:
            ${evidence.sourceContentHasMeaningfulCode}

            Sampled Source Files:
            ${evidence.sampledSourceFiles}

            Error:
            ${evidence.errorMessage}

            ================================
            """.trimIndent()
        )

        // ------------------------------------------------------------
        // Core assertions
        // ------------------------------------------------------------

        assertTrue(
            "GitHub URL should be valid.",
            evidence.isValidUrl
        )

        assertTrue(
            "Repository should be publicly accessible.",
            evidence.isPublic && evidence.isAccessible
        )

        assertFalse(
            "Repository name should not be empty.",
            evidence.repository.isBlank()
        )

        assertTrue(
            "Repository should contain at least one detected language.",
            evidence.languageCount > 0
        )

        assertTrue(
            "Repository should contain files.",
            evidence.totalFileCount > 0
        )

        assertTrue(
            "Repository should contain at least one detected source file.",
            evidence.sourceFileCount > 0
        )

        assertTrue(
            "Analyzer should retrieve at least one recent commit.",
            evidence.recentCommitSampleSize > 0
        )

        // ------------------------------------------------------------
        // Actual content-analysis checks
        // ------------------------------------------------------------

        assertTrue(
            "README should exist in the public repository.",
            evidence.hasReadme
        )

        assertTrue(
            "README content should be successfully analysed.",
            evidence.readmeContentLength > 0
        )

        assertTrue(
            "At least one source file should be sampled.",
            evidence.sourceContentSampleCount > 0
        )

        assertTrue(
            "At least one sampled source file should contain content.",
            evidence.sourceContentNonEmptyCount > 0
        )

        assertTrue(
            "Source content should contain characters.",
            evidence.sourceContentCharacters > 0
        )

        assertTrue(
            "Sampled source content should contain meaningful implementation evidence.",
            evidence.sourceContentHasMeaningfulCode
        )


    }

    @Test
    fun rejectInvalidGitHubUrl() = runBlocking {

        val invalidUrl =
            "https://example.com/not-a-github-repository"

        val evidence =
            GitHubAnalyzer.analyzeRepository(invalidUrl)

        Log.d(
            "PRISM_GITHUB_TEST",
            """
            =================================
            GITHUB ANALYZER TEST — G2
            =================================

            Test:
            Invalid / Non-GitHub URL

            Input URL:
            $invalidUrl

            Valid URL:
            ${evidence.isValidUrl}

            Public:
            ${evidence.isPublic}

            Accessible:
            ${evidence.isAccessible}

            Repository:
            ${evidence.repository}

            Total Files:
            ${evidence.totalFileCount}

            Source Files:
            ${evidence.sourceFileCount}

            Error:
            ${evidence.errorMessage}

            =================================
            """.trimIndent()
        )

        assertFalse(
            "Non-GitHub URL should be rejected.",
            evidence.isValidUrl
        )
    }

    @Test
    fun handleNonExistentGitHubRepository() = runBlocking {

        val unavailableUrl =
            "https://github.com/this-repository-definitely-does-not-exist-999999/PRISMOrbitTest"

        val evidence =
            GitHubAnalyzer.analyzeRepository(unavailableUrl)

        Log.d(
            "PRISM_GITHUB_TEST",
            """
            =================================
            GITHUB ANALYZER TEST — G3
            =================================

            Test:
            Valid GitHub URL / Non-existent Repository

            Input URL:
            $unavailableUrl

            Valid URL:
            ${evidence.isValidUrl}

            Public:
            ${evidence.isPublic}

            Accessible:
            ${evidence.isAccessible}

            Owner:
            ${evidence.owner}

            Repository:
            ${evidence.repository}

            Total Files:
            ${evidence.totalFileCount}

            Source Files:
            ${evidence.sourceFileCount}

            Languages:
            ${evidence.languages}

            Recent Commit Sample:
            ${evidence.recentCommitSampleSize}

            Error:
            ${evidence.errorMessage}

            =================================
            """.trimIndent()
        )

        assertTrue(
            "GitHub-format URL should still be recognised as a valid URL.",
            evidence.isValidUrl
        )

        assertFalse(
            "Non-existent repository should not be reported as accessible.",
            evidence.isAccessible
        )

        assertTrue(
            "Non-existent repository should not produce source-file evidence.",
            evidence.sourceFileCount == 0
        )

        assertTrue(
            "Non-existent repository should not produce commit evidence.",
            evidence.recentCommitSampleSize == 0
        )
    }

    @Test
    fun analyzeMultiLanguageRepository() = runBlocking {

        val repositoryUrl =
            "https://github.com/splunk/vscode-extension-splunk"

        val evidence =
            GitHubAnalyzer.analyzeRepository(repositoryUrl)

        Log.d(
            "PRISM_GITHUB_TEST",
            """
            =================================
            GITHUB ANALYZER TEST — G5
            =================================

            Test:
            Multi-language Public Repository

            Input URL:
            $repositoryUrl

            Valid URL:
            ${evidence.isValidUrl}

            Public:
            ${evidence.isPublic}

            Accessible:
            ${evidence.isAccessible}

            Owner:
            ${evidence.owner}

            Repository:
            ${evidence.repository}

            Languages:
            ${evidence.languages}

            Language Count:
            ${evidence.languageCount}

            Total Files:
            ${evidence.totalFileCount}

            Source Files:
            ${evidence.sourceFileCount}

            README Exists:
            ${evidence.hasReadme}

            README Content Length:
            ${evidence.readmeContentLength}

            README Meaningful:
            ${evidence.readmeHasMeaningfulContent}

            Recent Commit Sample:
            ${evidence.recentCommitSampleSize}

            Source Content Samples:
            ${evidence.sourceContentSampleCount}

            Non-empty Source Samples:
            ${evidence.sourceContentNonEmptyCount}

            Source Characters:
            ${evidence.sourceContentCharacters}

            Meaningful Source Code:
            ${evidence.sourceContentHasMeaningfulCode}

            Sampled Source Files:
            ${evidence.sampledSourceFiles}

            Error:
            ${evidence.errorMessage}

            =================================
            """.trimIndent()
        )

        // ------------------------------------------------------------
        // Basic repository checks
        // ------------------------------------------------------------

        assertTrue(
            "Repository URL should be valid.",
            evidence.isValidUrl
        )

        assertTrue(
            "Repository should be publicly accessible.",
            evidence.isPublic && evidence.isAccessible
        )

        assertFalse(
            "Repository name should not be empty.",
            evidence.repository.isBlank()
        )

        // ------------------------------------------------------------
        // Multi-language checks
        // ------------------------------------------------------------

        assertTrue(
            "Repository should contain multiple detected languages.",
            evidence.languageCount >= 2
        )

        assertTrue(
            "Python should be detected.",
            evidence.languages.any {
                it.equals("Python", ignoreCase = true)
            }
        )

        assertTrue(
            "JavaScript should be detected.",
            evidence.languages.any {
                it.equals("JavaScript", ignoreCase = true)
            }
        )

        assertTrue(
            "TypeScript should be detected.",
            evidence.languages.any {
                it.equals("TypeScript", ignoreCase = true)
            }
        )

        // ------------------------------------------------------------
        // Repository evidence checks
        // ------------------------------------------------------------

        assertTrue(
            "Repository should contain files.",
            evidence.totalFileCount > 0
        )

        assertTrue(
            "Repository should contain detected source files.",
            evidence.sourceFileCount > 0
        )

        assertTrue(
            "Analyzer should retrieve recent commits.",
            evidence.recentCommitSampleSize > 0
        )

        // ------------------------------------------------------------
        // Actual content-analysis checks
        // ------------------------------------------------------------

        assertTrue(
            "Analyzer should sample source-file contents.",
            evidence.sourceContentSampleCount > 0
        )

        assertTrue(
            "Sampled source files should contain content.",
            evidence.sourceContentNonEmptyCount > 0
        )

        assertTrue(
            "Source content should contain characters.",
            evidence.sourceContentCharacters > 0
        )

        assertTrue(
            "Source content should contain meaningful implementation.",
            evidence.sourceContentHasMeaningfulCode
        )
    }

    @Test
    fun analyzeGitHubRepositoryWithTrailingSlash() = runBlocking {

        val repositoryUrl =
            "https://github.com/aman17102005/PRISM-Orbit/"

        val evidence =
            GitHubAnalyzer.analyzeRepository(repositoryUrl)

        Log.d(
            "PRISM_GITHUB_TEST",
            """
            =================================
            GITHUB ANALYZER TEST — G6
            =================================

            Test:
            Valid Public Repository + Trailing Slash

            Input URL:
            $repositoryUrl

            Valid URL:
            ${evidence.isValidUrl}

            Public:
            ${evidence.isPublic}

            Accessible:
            ${evidence.isAccessible}

            Owner:
            ${evidence.owner}

            Repository:
            ${evidence.repository}

            Total Files:
            ${evidence.totalFileCount}

            Source Files:
            ${evidence.sourceFileCount}

            Languages:
            ${evidence.languages}

            README Exists:
            ${evidence.hasReadme}

            README Meaningful:
            ${evidence.readmeHasMeaningfulContent}

            Recent Commit Sample:
            ${evidence.recentCommitSampleSize}

            Source Content Samples:
            ${evidence.sourceContentSampleCount}

            Non-empty Source Samples:
            ${evidence.sourceContentNonEmptyCount}

            Source Characters:
            ${evidence.sourceContentCharacters}

            Meaningful Source Code:
            ${evidence.sourceContentHasMeaningfulCode}

            Error:
            ${evidence.errorMessage}

            =================================
            """.trimIndent()
        )

        assertTrue(
            "GitHub URL with trailing slash should be recognised as valid.",
            evidence.isValidUrl
        )

        assertTrue(
            "Public repository should be accessible.",
            evidence.isPublic && evidence.isAccessible
        )

        assertFalse(
            "Repository name should not be empty.",
            evidence.repository.isBlank()
        )

        assertTrue(
            "Analyzer should detect repository files.",
            evidence.totalFileCount > 0
        )

        assertTrue(
            "Analyzer should detect source files.",
            evidence.sourceFileCount > 0
        )

        assertTrue(
            "Analyzer should retrieve recent commits.",
            evidence.recentCommitSampleSize > 0
        )

        assertTrue(
            "Analyzer should sample source content.",
            evidence.sourceContentSampleCount > 0
        )

        assertTrue(
            "Source samples should contain content.",
            evidence.sourceContentNonEmptyCount > 0
        )

        assertTrue(
            "Source content should contain characters.",
            evidence.sourceContentCharacters > 0
        )

        assertTrue(
            "Analyzer should detect meaningful source code.",
            evidence.sourceContentHasMeaningfulCode
        )
    }
}