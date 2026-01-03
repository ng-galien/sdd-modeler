# Maven Sample for SDD Modeler Plugin

Demonstrates running the Maven plugin built by Gradle. Use after building/publishing the plugin
locally with Gradle:

```bash
./gradlew :state-modeler-maven-plugin:publishToMavenLocal
cd maven-sample
mvn clean verify
```

The build will generate sources under `target/generated-sources/sdd` and DDL under
`target/generated-sources/sdd/ddl/schema.sql` using `src/main/resources/sdd.yaml`.
