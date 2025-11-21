group = "io.statemodeler"
version = "0.1.0-SNAPSHOT"

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(gradleApi())
    //Depends on core module
    // When the plugin is included as a composite build (pluginManagement.includeBuild),
    // the plugin build is executed in a build that is separate from the main multi-project
    // build. In that case 'project(":state-modeler-core")' is not available and the
    // dependency must be resolved via an external binary artifact (mavenLocal or repo).
    // When developing locally, publish the core module to mavenLocal using
    // `./gradlew :state-modeler-core:publishToMavenLocal` so it can be consumed here.
    implementation("io.statemodeler:state-modeler-core:0.1.0-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("sddCodegen") {
            id = "io.statemodeler.sdd-codegen"
            displayName = "SDD Modeler code generation plugin"
            description = "Generates source code from SDD model files using the SDD Modeler core library."
            implementationClass = "io.statemodeler.gradle.SddCodegenPlugin"
        }
    }
}