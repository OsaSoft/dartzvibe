plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()

    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint(ktlintVersion)
            .editorConfigOverride(
                mapOf(
                    // enforces all multiline expressions to be wrapped, disable as we use this for readability
                    "ktlint_standard_multiline-expression-wrapping" to "disabled",
                    // enforces function declarations with more than 2 args to be multiline, disable
                    "ktlint_standard_function-signature" to "disabled",
                    // enforces annotations to be on a separate line, disable
                    "ktlint_standard_annotation" to "disabled",
                    // enforces no empty first line in class body, disable as we use this for readability
                    "ktlint_standard_no-empty-first-line-in-class-body" to "disabled",
                    // enforces no comments in certain locations, but that includes eg. TODO comment on same line
                    "ktlint_standard_discouraged-comment-location" to "disabled",
                    // enforces functions with only return or throw to be expression body,
                    // disable as we use this for readability and to avoid very long lines
                    // also the linter doesn't take the max line length into account
                    "ktlint_standard_function-expression-body" to "disabled",
                    // enforces class signatures to be on a single line, disable as we use this for readability
                    "ktlint_standard_class-signature" to "disabled",
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**/*.kts")
        ktlint(ktlintVersion)
        trimTrailingWhitespace()
        endWithNewline()
    }
}
