# SDD Modeler Gradle Plugin

Plugin Gradle pour intégrer la génération SDD (code + DDL) dans un build Java.

## Installation

```kotlin
plugins {
    id("io.statemodeler.sdd-codegen") version "0.1.0-SNAPSHOT" // version du repo
}
```
Dans ce mono-repo, le plugin est inclus en composite build (`settings.gradle.kts`), donc pas besoin de publier pour l'utiliser en local.

## Configuration (DSL Kotlin)

```kotlin
sddCodegen {
    modelFile.set(file("src/main/resources/sdd.yaml"))
    outputDir.set(layout.buildDirectory.dir("generated/sdd"))
    ddlOutputDir.set(layout.buildDirectory.dir("generated/sdd/ddl"))
    language.set("java")
    generateController.set(true)
    generateRepository.set(true)
    generateMcp.set(true)
    liquibase.set(true) // changelog.yaml au lieu de schema.sql
    addToSourceSet.set(true) // ajoute le code généré à main
}
```

Variables (valeurs par défaut) :
- `modelFile` : `src/main/resources/sdd.yaml`
- `outputDir` : `build/generated/sdd`
- `ddlOutputDir` : `build/generated/sdd/ddl`
- `language` : `java`
- Toggles : `generateController`, `generateRepository`, `generateMcp` (tous `true`)
- `liquibase` : `false`
- `addToSourceSet` : `true`

## Tâches ajoutées
- `generateSddCode` : génère le code (filée dans `compileJava` si `addToSourceSet=true`).
- `generateSddDdl` : génère DDL (`schema.sql` ou `changelog.yaml` si `liquibase=true`).

## Exemple minimal

```kotlin
plugins {
    java
    id("io.statemodeler.sdd-codegen")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
}

sddCodegen {
    modelFile.set(file("model/orders.yaml"))
    outputDir.set(layout.buildDirectory.dir("generated/sdd"))
}
```

## Postgres pour tests (si vous réutilisez common-sample)
Définissez `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` (par défaut : host=localhost, db=sdd_test, user=test, pass=test).

## Développement du plugin
- Build local : `./gradlew :state-modeler-gradle-plugin:build`
- Publier le core localement si besoin : `./gradlew :state-modeler-core:publishToMavenLocal`

## Ressources
- Sample Gradle : [`gradle-sample`](../gradle-sample) (utilise ce plugin + `common-sample`).
- CI Maven/Gradle : voir badges dans le README racine et `.github/workflows/ci.yml`, `maven-ci.yml`.
