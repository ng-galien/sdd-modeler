pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
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
include("state-modeler-gradle-plugin")

