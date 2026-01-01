# Change: Maven plugin + split sample modules

## Why
The project only ships a Gradle plugin and a single `sample` module built with Gradle. Teams using Maven cannot consume the generator, and the current sample mixes Gradle wiring with the generated app tests, making cross-build coverage hard to maintain.

## What Changes
- Add a Maven plugin mirroring the Gradle plugin configuration and tasks (generate sources, apply DDL, verify) so Maven projects can run the generator.
- Split the existing `sample` into `common-sample` (shared model + tests), `gradle-sample` using the Gradle plugin, and `maven-sample` using the new Maven plugin, both reusing the shared tests.
- Keep parity of integration tests: Maven sample runs the same end-to-end checks currently in `sample` (DDL apply + generated Spring Boot app + MockMvc tests).
- Update build/CI wiring so both sample variants are exercised without local publishing steps.

## Impact
- Affected specs: sample-module, new maven-plugin capability (and build-system integration).
- Affected code: build scripts for new modules, plugin implementations (Gradle + new Maven), sample app/test layout, CI workflows for running Gradle and Maven sample checks.
