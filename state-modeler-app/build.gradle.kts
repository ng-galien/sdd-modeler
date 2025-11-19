plugins {
    application
    id("org.graalvm.buildtools.native") version "0.11.3"
}

application {
    mainClass = "io.statemodeler.cli.Main"
}

// GraalVM native-image configuration (minimal for CLI native build)
graalvmNative {
    binaries {
        named("main") {
            imageName.set("sdd-modeler")
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
    // Lombok for reducing boilerplate
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    
    // Depend on core module
    implementation(project(":state-modeler-core"))
    
    // Vavr for functional programming (needed for Try<T> from core)
    implementation("io.vavr:vavr:0.10.7")
    
    // Picocli for CLI
    implementation("info.picocli:picocli:${rootProject.ext["picocliVersion"]}")
    annotationProcessor("info.picocli:picocli-codegen:${rootProject.ext["picocliVersion"]}")
    
    // H2 Database (embedded) for SDR repository
    implementation("com.h2database:h2:2.2.224")
    
    // JSON diff library for schema comparison
    implementation("com.flipkart.zjsonpatch:zjsonpatch:0.4.16")

    // Snakeyaml for YAML parsing
    implementation("org.yaml:snakeyaml:2.2")
    
    // Diff algorithm for DDL comparison
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    
    // LangChain4j for LLM-based migration script generation (Ollama and OpenAI)
    implementation("dev.langchain4j:langchain4j:1.8.0")
    implementation("dev.langchain4j:langchain4j-ollama:1.8.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.8.0")
    
    // SLF4J for logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.12")

    // Test dependencies specific to CLI
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}