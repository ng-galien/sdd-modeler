group = "io.statemodeler"
version = "0.1.0-SNAPSHOT"

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(project(":state-modeler-core"))
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
