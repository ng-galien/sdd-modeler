plugins {
    id("java")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

// Acts as a shared source holder; tests/resources are consumed by other modules.
tasks.test { enabled = false }
