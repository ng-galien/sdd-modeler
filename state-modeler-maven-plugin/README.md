# SDD Modeler Maven Plugin

A Maven plugin that mirrors the SDD Gradle plugin: validate an SDD model, generate code, and emit
DDL or Liquibase changelog with identical defaults and toggles.

## Usage

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.statemodeler</groupId>
      <artifactId>sdd-maven-plugin</artifactId>
      <version>${sdd.version}</version>
      <executions>
        <execution>
          <goals>
            <goal>generate-sdd-code</goal>
            <goal>generate-sdd-ddl</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <!-- all parameters are optional; defaults mirror the Gradle plugin -->
        <modelFile>${project.basedir}/src/main/resources/sdd.yaml</modelFile>
        <outputDir>${project.build.directory}/generated-sources/sdd</outputDir>
        <ddlOutputDir>${project.build.directory}/generated-sources/sdd/ddl</ddlOutputDir>
        <language>java</language>
        <generateController>true</generateController>
        <generateRepository>true</generateRepository>
        <generateMcp>true</generateMcp>
        <addToSource>true</addToSource>
        <liquibase>false</liquibase>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### Goals

- `generate-sdd-code`: validate the model and generate code; optionally registers output as a
  compile source root (`addToSource=true` by default).
- `generate-sdd-ddl`: validate the model and emit `schema.sql` or `changelog.yaml` when
  `liquibase=true`.

### Defaults and toggles (parity with Gradle)

- `modelFile`: `src/main/resources/sdd.yaml`
- `outputDir`: `target/generated-sources/sdd`
- `ddlOutputDir`: `target/generated-sources/sdd/ddl`
- `language`: `java`
- `generateController`, `generateRepository`, `generateMcp`: all `true`
- `addToSource`: `true`
- `liquibase`: `false`

### Running

```bash
./mvnw -Dsdd.modelFile=src/main/resources/sdd.yaml \
       io.statemodeler:sdd-maven-plugin:${sdd.version}:generate-sdd-code
```
