group = "io.statemodeler"
version = libs.versions.sdd.version.get()

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    //Depends on core module
    implementation(libs.state.modeler.core)
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
