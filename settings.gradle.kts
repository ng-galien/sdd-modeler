pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("state-modeler-gradle-plugin")
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "sdd-modeler"

include("state-modeler-app")
include("sample")

include("state-modeler-core")

