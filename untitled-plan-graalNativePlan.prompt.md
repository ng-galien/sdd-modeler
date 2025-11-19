Résumé

Proposition détaillée pour ajouter la possibilité de produire un exécutable natif (GraalVM native-image) pour le module CLI `state-modeler-app`. Le plan inclut : configuration Gradle (plugin GraalVM), fichiers de configuration natifs (réflexion / ressources / proxy), instrumentation via `native-image-agent`, procedure de build local et CI (GitHub Actions) mult-OS.

Détails

- Entrée CLI confirmée : `io.statemodeler.cli.Main` (fichier : `state-modeler-app/src/main/java/io/statemodeler/cli/Main.java`).
- Dépendances runtime à surveiller :
  - Jackson (jackson-databind, jackson-dataformat-yaml, jackson-datatype-jsr310) : introspection / reflection ; nécessite config pour les DTOs.
  - Picocli (cli) : sous-commands et annotations ; l’AOT fonctionne mieux si les commandes sont connues mais la reflection est souvent nécessaire.
  - Pebble templates : accès aux propriétés via reflection ; templates ressources doivent être embarquées.
  - Vavr : Try/Validation (généralement pas d’attache reflection spécifique).
  - LangChain4j / clients LLM : clients HTTP, SPI et code dynamique ; potentiel overhead de configuration (recommandation : exclure `migrate` dans la première passe native ou utiliser un binaire JVM séparé).
  - H2 / JDBC / drivers : (DriverManager / ServiceLoader) ; les drivers nécessitent la configuration ServiceLoader si on veut les starter via native-image.
- Aucune configuration Graal existante dans le repo. Le plugin et la configuration sont à ajouter.

Plan d'actions (étapes)

1) Ajouter le plugin GraalVM (Native Build Tools) au module CLI
   - Fichier : `state-modeler-app/build.gradle.kts`.
   - Ajouter :
     - plugins block : `id("org.graalvm.buildtools.native") version "0.9.23"` (ou la version compatible la plus récente).
     - application.mainClass : `io.statemodeler.cli.Main`.
     - config `graalvmNative` : `imageName = "sdd-modeler"`, `buildArgs` (ex. `--no-fallback`, `-H:+ReportExceptionStackTraces`).

2) Créer la structure de configuration native initiale
   - Chemin : `state-modeler-app/src/main/resources/META-INF/native-image/`.
   - Fichiers : `reflect-config.json`, `resource-config.json`, `proxy-config.json`, (éventuellement `jni-config.json`).
   - Inclure : DTOs sérieux (ex. `YamlModelDto` et membres), commandes Picocli, classes utilisées par Pebble (models), `logback.xml`, et `templates/**`.

3) Instrumenter l’application pour récupérer la config réelle (native-image-agent)
   - Exécution recommandée sur un exemple :
     - `./gradlew :state-modeler-app:jar` puis :
     - `java -agentlib:native-image-agent=config-output-dir=tmp/native-config -jar state-modeler-app/build/libs/state-modeler-app.jar validate scripts/examples/orders-sdd-mini-model.yaml` et exécuter plusieurs commandes représentatives (`--help`, `validate`, `list`, `sql`, `show`).
   - Récupérer : `tmp/native-config/reflection-config.json` et autres, puis copier dans `src/main/resources/META-INF/native-image/` après revue manuelle.

4) Ajuster build Gradle pour embarquer ces fichiers
   - Dans `state-modeler-app/build.gradle.kts` : configurer `graalvmNative`/`nativeImage` pour inclure les ressources et args.
   - Inclure : `templates/**`, `logback.xml`, `sdd-model-schema.json`, et autres ressources.
   - Ajouter `buildArgs` et éventuellement `--initialize-at-build-time`/`--initialize-at-run-time` pour libs problématiques.

5) Tests et vérifications locales
   - Exécuter : `./gradlew :state-modeler-app:nativeCompile`.
   - Lancer l’exécutable : `./state-modeler-app/build/native/nativeCompile/sdd-modeler --help`.
   - Vérifier : `validate`, `list`, `show`, `sql` et `--outdir` behavior.
   - Ajuster `reflect-config.json` / `resource-config.json` selon erreurs (classes manquantes, exceptions reflection, templates non trouvés).

6) CI GitHub Actions (matrix multi-OS)
   - Workflow : `.github/workflows/native-image.yml`.
   - Étapes : checkout, setup GraalVM + native-image (via `graalvm/setup-java` action), instrumenter runs (agent), copier configs générés, build avec `./gradlew :state-modeler-app:nativeCompile`, puis smoke tests (help, validate sample YAML).
   - Matrix : `ubuntu-latest`, `macos-latest`, `windows-latest` (vérifier disponibilité GraalVM natif sur macOS/Windows). Cacher gradle dependencies.

CI / Script d’exercice de l’agent

Local quickstart pour générer la config d’instrumentation:

```bash
# 1. Build jar
./gradlew :state-modeler-app:clean :state-modeler-app:jar

# 2. Run jar with native-image-agent to collect config
java -agentlib:native-image-agent=config-output-dir=tmp/native-config -jar state-modeler-app/build/libs/state-modeler-app.jar validate scripts/examples/orders-sdd-mini-model.yaml

# 3. Inspect tmp/native-config and merge into src/main/resources/META-INF/native-image/
# 4. Build native image
./gradlew :state-modeler-app:nativeCompile

# 5. Run smoke tests
./state-modeler-app/build/native/nativeCompile/sdd-modeler --help
./state-modeler-app/build/native/nativeCompile/sdd-modeler validate scripts/examples/orders-sdd-mini-model.yaml
```

Fichiers `configs` à ajouter (exemples)

- `state-modeler-app/build.gradle.kts` (ajout du plugin et config):

```kotlin
plugins {
    application
    id("org.graalvm.buildtools.native") version "0.9.23"
}

application {
    mainClass.set("io.statemodeler.cli.Main")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("sdd-modeler")
            buildArgs.addAll("--no-fallback", "-H:+ReportExceptionStackTraces")
        }
    }
    resources {
        includes.add("templates/**")
        includes.add("logback.xml")
        includes.add("sdd-model-schema.json")
    }
    agent {
        enabled.set(true)
        configOutputDir.set(file("tmp/native-config"))
    }
}
```

- `state-modeler-app/src/main/resources/META-INF/native-image/reflect-config.json` (squelette initial):

```json
[
  {
    "name": "io.statemodeler.dsl.yaml.YamlModelDto",
    "allDeclaredConstructors": true,
    "allDeclaredFields": true
  },
  {
    "name": "io.statemodeler.dsl.yaml.YamlModelDto$YamlDatabaseDto",
    "allDeclaredConstructors": true,
    "allDeclaredFields": true
  },
  {
    "name": "io.statemodeler.core.DatabaseConfig",
    "allDeclaredConstructors": true,
    "allDeclaredFields": true
  }
]
```

- `state-modeler-app/src/main/resources/META-INF/native-image/resource-config.json` (squelette initial):

```json
{
  "resources": [
    { "pattern": "templates/.*" },
    { "pattern": "logback.xml" },
    { "pattern": "sdd-model-schema.json" }
  ]
}
```

- `state-modeler-app/src/main/resources/META-INF/native-image/proxy-config.json` (si proxies sont utilisés) :

```json
[
  ["java.lang.reflect.Proxy"]
]
```

Options & recommandations

Option A — GraalVM native-image (recommandé pour CLI):
- Pros : exécutable natif, startup rapide, distribution unique.
- Cons : exige configuration de réflexion/ressources; complexité pour LangChain & clients dynamiques; cross-compile spécifique par plateforme.
- Recommandation : produire d’abord un binaire natif pour les commandes stables (`validate`, `sql`, `show`, `list`), exclure ou laisser la commande `migrate` en JVM ou version séparée.

Option B — jlink / jpackage (fallback / alternative):
- Pros : configuration plus simple; supporte dynamiques; packaging cross-OS (installers), peu de maintenance de config reflection.
- Cons : binaire plus gros; démarrage plus lent; nécessite packaging per OS.
- Recommandation : utile si la complexité de config Graal s’avère plus coûteuse que les bénéfices.

Checklist détaillée et tâches à réaliser ensuite

- [ ] Ajouter plugin et configuration Gradle pour GraalVM dans `state-modeler-app/build.gradle.kts`.
- [ ] Créer `META-INF/native-image/reflect-config.json`/`resource-config.json`/`proxy-config.json` initiaux.
- [ ] Instrumenter runs via `native-image-agent` pour enrichir config; intégrer et nettoyer la config générée.
- [ ] Exécuter `./gradlew :state-modeler-app:nativeCompile` et corriger les erreurs de réflection/ressources.
- [ ] Ajouter tests smoke qui s’exécutent sur le binaire natif au sortir du build.
- [ ] Mettre en place GitHub Actions pour créer des binaires natifs sur Linux/macOS/Windows, instrumenter, builder et tester.
- [ ] Examiner l’intégration de LangChain4j (LLM) : soit l’instrumenter (complexe) soit la laisser out-of-image (JVM-only). Documenter la décision.

Notes finales

- La réalisation d’un binaire natif stable prend plusieurs itérations : l’utilisation du `native-image-agent` pour collecter des infos durant des temps d’exécution représentatifs accélère grandement le travail.
- Pour démarrer rapidement, je propose : (A) produire un binaire natif limité aux commandes statiques et (B) documenter comment exécuter la commande `migrate` via JVM si nécessaire.
- Si vous voulez, je peux mettre en place la configuration Gradle et les fichiers d’exemple dans le repo et ouvrir un PR (patch) qui installe le plugin, crée les configs initiales et ajoute le workflow CI de build natif.

Souhaitez-vous que je crée tout cela maintenant (patch Gradle + config + workflow CI initial), ou commencez-vous par une version kit de config initiale pour itération manuelle ?
