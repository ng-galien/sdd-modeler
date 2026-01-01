# SDD Modeler Maven Plugin

Plugin Maven pour générer code et DDL à partir d'un modèle SDD (YAML/JSON).

## Installation rapide

```xml
<plugin>
  <groupId>io.statemodeler</groupId>
  <artifactId>sdd-modeler-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</plugin>
```

Le plugin dépend du jar `state-modeler-core`. Dans ce mono-repo, publiez-le d'abord dans le repo isolé `build/m2` :

```bash
GRADLE_USER_HOME=./build/gradle-home ./gradlew :state-modeler-core:publishMavenJavaPublicationToBuildM2Repository --no-daemon
```

Puis construisez/installez le plugin avec ce repo local :

```bash
cd maven
mvn -B -Dmaven.repo.local=../build/m2 -DskipTests -pl maven-plugin -am clean install
```

## Exemple de configuration (pom.xml)

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.statemodeler</groupId>
      <artifactId>sdd-modeler-maven-plugin</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <inherited>false</inherited>
      <configuration>
        <modelFile>${project.basedir}/src/main/resources/sdd.yaml</modelFile>
        <outputDir>${project.build.directory}/generated/sdd</outputDir>
        <ddlOutputDir>${project.build.directory}/generated/sdd/ddl</ddlOutputDir>
        <language>java</language>
        <generateController>true</generateController>
        <generateRepository>true</generateRepository>
        <generateMcp>true</generateMcp>
        <liquibase>true</liquibase>
      </configuration>
      <executions>
        <execution>
          <id>generate-model-sources</id>
          <phase>generate-sources</phase>
          <goals>
            <goal>generate</goal>
          </goals>
        </execution>
        <execution>
          <id>generate-ddl-resources</id>
          <phase>generate-resources</phase>
          <goals>
            <goal>generate-ddl</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

## Paramètres disponibles

| Paramètre             | Description                                                     | Défaut                                |
|-----------------------|-----------------------------------------------------------------|----------------------------------------|
| `modelFile`           | Chemin du modèle SDD (YAML/JSON)                                | `src/main/resources/sdd.yaml`          |
| `outputDir`           | Dossier de code généré                                          | `target/generated/sdd`                 |
| `ddlOutputDir`        | Dossier DDL généré (schema.sql ou changelog Liquibase)          | `target/generated/sdd/ddl`             |
| `language`            | Langage cible (`java`)                                          | `java`                                 |
| `generateController`  | Générer les contrôleurs REST                                    | `true`                                 |
| `generateRepository`  | Générer les repositories Spring Data                            | `true`                                 |
| `generateMcp`         | Générer serveur MCP (Spring AI)                                 | `true`                                 |
| `liquibase`           | Générer un changelog Liquibase plutôt qu'un SQL brut            | `false`                                |

## Repo local isolé (recommandé CI/dev)

Utilisez `-Dmaven.repo.local=../build/m2` (chemin relatif à `maven/`) pour éviter de polluer `~/.m2`. Le workflow GitHub `maven-ci.yml` suit ce schéma.

## Tests / Postgres

Les tests d'intégration attendent Postgres :
- host `localhost`, port `5432`
- db `sdd_test`
- user `postgres`, pass `postgrs`

Ces valeurs peuvent être surchargées via `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.

## Commandes utiles

- Générer sans tests : `mvn -B -Dmaven.repo.local=../build/m2 -DskipTests -pl maven-plugin -am clean install`
- Vérifier le sample Maven : `mvn -B -Dmaven.repo.local=../build/m2 -pl maven-sample -am verify`

## Dépannage

- **Impossible de résoudre `state-modeler-core`** : assurez-vous d'avoir publié le core dans `build/m2` (commande Gradle ci-dessus).
- **Changelog/DDL manquant** : vérifiez `ddlOutputDir` et que la phase `generate-resources` s'exécute (voir executions ci-dessus).
- **Tests Postgres skip** : fournir les creds/instance ou ignorer avec `-DskipTests`.
