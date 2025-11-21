plugins {
    application
    alias(libs.plugins.graalvm.buildtools.native)
}

application {
    mainClass = "io.statemodeler.cli.Main"
}

// GraalVM native-image configuration (minimal for CLI native build)
graalvmNative {
    binaries {
        named("main") {
            imageName.set("sdd-modeler")
            sharedLibrary.set(false)
            mainClass.set("io.statemodeler.cli.Main")
            // Use no-fallback by default for a pure native image. Remove if fallback is needed.
            buildArgs.addAll("--no-fallback", "-H:+ReportExceptionStackTraces")
            // Resource and agent configuration intentionally left out to
            // prevent Kotlin DSL misconfiguration. Resources will be pulled
            // from the classpath by default; for detailed control, add
            // resource & agent config following the plugin docs.
        }
    }

    // Additional configuration for resources and agent can be added here. The
    // plugin version has different DSL features across versions; adjust if
    // specific features are needed (resources/proxy/agent configuration).
}

dependencies {
    //Depends on core module
    implementation("io.statemodeler:state-modeler-core")

    // Lombok for reducing boilerplate
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    // Vavr for functional programming (needed for Try<T> from core)
    implementation(libs.vavr)
    
    // Picocli for CLI
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)
    
    // H2 Database (embedded) for SDR repository
    implementation(libs.h2)
    
    // JSON diff library for schema comparison
    implementation(libs.zjsonpatch)

    // Snakeyaml for YAML parsing
    implementation(libs.snakeyaml)
    
    // Diff algorithm for DDL comparison
    implementation(libs.java.diff.utils)
    
    // LangChain4j for LLM-based migration script generation (Ollama and OpenAI)
    implementation(libs.langchain4j)
    implementation(libs.langchain4j.ollama)
    implementation(libs.langchain4j.open.ai)
    
    // SLF4J for logging (runtime provider is centralized by root `subprojects` configuration)
    implementation(libs.slf4j.api)

    // Test dependencies specific to CLI
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}