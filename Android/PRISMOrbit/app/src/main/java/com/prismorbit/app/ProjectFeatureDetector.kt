package com.prismorbit.app

// ============================================================
// PROJECT FEATURE DETECTOR (plain Kotlin, no Android)
// ============================================================
// Finds features (auth, database, backend_api, ai_ml, payments,
// realtime, mobile_app, deployment, tests) from the repository's
// file paths and from a few small manifest files. The paths come
// from the file tree the analyzer already loaded, so this costs
// no extra GitHub requests.
// ============================================================

object ProjectFeatureDetector {

    /** File names whose content is worth reading (at most 3 are fetched). */
    val MANIFEST_NAMES = listOf(
        "package.json", "requirements.txt", "pubspec.yaml", "pom.xml",
        "build.gradle.kts", "build.gradle"
    )

    /** Picks up to [limit] manifest paths: shallow files first. */
    fun pickManifests(paths: List<String>, limit: Int = 3): List<String> {
        val picked = mutableListOf<String>()
        val byName = paths.groupBy { it.substringAfterLast('/').lowercase() }
        for (name in MANIFEST_NAMES) {
            val best = byName[name]
                ?.filterNot { it.contains("node_modules/") }
                // prefer app-level gradle files, then the shallowest path
                ?.minByOrNull { path ->
                    val depth = path.count { it == '/' }
                    if (name.startsWith("build.gradle") && path.startsWith("app/")) depth - 1 else depth
                }
            if (best != null) picked += best
            if (picked.size >= limit) break
        }
        return picked
    }

    fun detect(paths: List<String>, manifests: Map<String, String>): Set<String> {
        val lowerPaths = paths.map { it.lowercase() }
        val manifestText = manifests.values.joinToString("\n").lowercase()
        val found = linkedSetOf<String>()

        fun hasPath(vararg parts: String) =
            lowerPaths.any { path -> parts.any { part -> path.contains(part) } }

        fun hasFile(vararg names: String) =
            lowerPaths.any { path -> names.any { n -> path.substringAfterLast('/') == n } }

        fun hasText(vararg words: String) = words.any { manifestText.contains(it) }

        // ---- auth
        if (hasText(
                "firebase-auth", "firebase_auth", "passport", "jsonwebtoken", "bcrypt", "next-auth",
                "auth0", "clerk", "spring-boot-starter-security", "flask-login", "django.contrib.auth",
                "play-services-auth", "credentials", "oauth"
            ) ||
            hasPath("/login", "login.", "/auth/", "auth.", "signin", "sign_in", "signup", "sign_up", "authentication")
        ) found += ProjectFeatures.AUTH

        // ---- database
        if (hasText(
                "firebase-firestore", "cloud_firestore", "firebase-database", "androidx.room", "room-runtime",
                "mongoose", "mongodb", "pymongo", "sequelize", "prisma", "sqlalchemy", "psycopg", "mysql",
                "sqlite", "supabase", "typeorm", "hibernate", "spring-boot-starter-data", "pg\""
            ) ||
            hasPath(".sql", "schema.prisma", "/migrations/", "/models/", "database.", "/db/")
        ) found += ProjectFeatures.DATABASE

        // ---- backend api
        if (hasText(
                "\"express\"", "fastapi", "flask", "django", "\"koa\"", "@nestjs", "spring-boot-starter-web",
                "retrofit", "axios", "ktor", "apollo", "graphql"
            ) ||
            hasPath("/server/", "/backend/", "/api/", "/routes/", "/controllers/") ||
            hasFile("server.js", "app.py", "main.py", "server.ts")
        ) found += ProjectFeatures.BACKEND_API

        // ---- ai / ml
        if (hasText(
                "tensorflow", "torch", "keras", "scikit-learn", "sklearn", "transformers", "openai",
                "langchain", "huggingface", "mlkit", "tflite", "opencv", "generativeai", "gemini",
                "@google/genai", "anthropic", "xgboost", "nltk", "spacy"
            ) ||
            hasPath(".ipynb", ".h5", ".tflite", ".onnx", ".pt")
        ) found += ProjectFeatures.AI_ML

        // ---- payments
        if (hasText("stripe", "razorpay", "paypal", "braintree", "cashfree", "paytm", "play-services-wallet", "billing")) {
            found += ProjectFeatures.PAYMENTS
        }

        // ---- realtime
        if (hasText(
                "socket.io", "websocket", "firebase-database", "signalr", "pusher", "webrtc", "mqtt", "\"ws\""
            ) ||
            hasPath("websocket", "socket.")
        ) found += ProjectFeatures.REALTIME

        // ---- mobile app
        if (hasFile("androidmanifest.xml", "pubspec.yaml", "info.plist", "app.json") ||
            hasPath(".xcodeproj", "/android/app/", "/ios/") ||
            hasText("com.android.application", "react-native", "\"expo\"", "flutter")
        ) found += ProjectFeatures.MOBILE_APP

        // ---- deployment
        if (hasFile(
                "dockerfile", "docker-compose.yml", "docker-compose.yaml", "vercel.json", "netlify.toml",
                "procfile", "render.yaml", "fly.toml", "serverless.yml", "app.yaml", "jenkinsfile",
                "firebase.json", "heroku.yml"
            ) ||
            hasPath(".github/workflows/")
        ) found += ProjectFeatures.DEPLOYMENT

        // ---- tests
        if (hasPath(
                "/test/", "/tests/", "__tests__", "/androidtest/", ".test.js", ".test.ts", ".test.tsx",
                ".spec.js", ".spec.ts", "_test.py", "_test.go"
            ) || paths.any { it.endsWith("Test.kt") || it.endsWith("Test.java") } ||
            lowerPaths.any { it.startsWith("test/") || it.startsWith("tests/") || it.substringAfterLast('/').startsWith("test_") } ||
            hasText("pytest", "\"jest\"", "junit", "mocha", "vitest")
        ) found += ProjectFeatures.TESTS

        return found
    }
}
