package com.prismorbit.app

import java.net.URI

// ============================================================
// GITHUB / LIVE URL HELPERS (plain Kotlin, no Android)
// ============================================================

object GitHubUrls {

    private fun withScheme(url: String): String {
        val t = url.trim()
        return if (t.contains("://")) t else "https://$t"
    }

    private fun hostAndSegments(url: String): Pair<String, List<String>>? {
        val uri = try {
            URI(withScheme(url).substringBefore('#').substringBefore('?'))
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.lowercase() ?: return null
        val segments = (uri.path ?: "").split('/').filter { it.isNotBlank() }
        return host to segments
    }

    private fun isGitHubHost(host: String) = host == "github.com" || host == "www.github.com"

    /** owner and repository name from https://github.com/owner/repo(.git)(/...). */
    fun parseRepository(url: String): Pair<String, String>? {
        if (url.isBlank()) return null
        val (host, segments) = hostAndSegments(url) ?: return null
        if (!isGitHubHost(host) || segments.size < 2) return null
        val owner = segments[0].trim()
        val repo = segments[1].trim().removeSuffix(".git")
        if (owner.isBlank() || repo.isBlank()) return null
        return owner to repo
    }

    /** user name from https://github.com/username(/...). Null if the link is not a profile link. */
    fun parseProfileOwner(profileUrl: String): String? {
        if (profileUrl.isBlank()) return null
        val (host, segments) = hostAndSegments(profileUrl) ?: return null
        if (!isGitHubHost(host) || segments.isEmpty()) return null
        return segments[0].trim().ifBlank { null }
    }

    /**
     * Compares the repository owner with the profile owner, ignoring case.
     * true = same, false = different, null = profile URL missing or not readable.
     */
    fun ownerMatches(repoOwner: String, profileUrl: String): Boolean? {
        val profileOwner = parseProfileOwner(profileUrl) ?: return null
        return repoOwner.equals(profileOwner, ignoreCase = true)
    }

    fun normalizedRepositoryUrl(url: String): String? =
        parseRepository(url)?.let { "https://github.com/${it.first}/${it.second}" }
}

object LiveLinks {

    fun normalize(url: String): String {
        val t = url.trim()
        return if (t.contains("://")) t else "https://$t"
    }

    /** Play Store and App Store links are store links. */
    fun isStoreLink(url: String): Boolean {
        if (url.isBlank()) return false
        val host = try {
            URI(normalize(url)).host?.lowercase().orEmpty()
        } catch (_: Exception) {
            return false
        }
        return host == "play.google.com" ||
                host == "apps.apple.com" ||
                host == "itunes.apple.com" ||
                host == "testflight.apple.com"
    }

    fun isWebUrl(url: String): Boolean {
        val t = normalize(url)
        return try {
            val uri = URI(t)
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
