# Change: Refactor Gradle Configuration

## Why
The Gradle build should be simple, lean, and aligned with best practices so it stays easy to
maintain as modules evolve.

## What Changes
- Review root and module Gradle configuration for duplication, unnecessary custom logic, and
  outdated patterns
- Align plugin usage, dependency management, and task configuration with Gradle best practices
- Simplify build scripts by centralizing shared settings and preferring standard Gradle DSL
- Keep changes minimal and focused on clarity, maintainability, and build performance
- Ensure plugin management is CI-friendly: the `sample` module can build using the Gradle plugin
  module in `.github/workflows/ci.yml` without special local setup
- Update build documentation if any conventions change

## Non-Goals
- No module restructuring or feature work outside Gradle configuration
- No dependency upgrades unless required by configuration cleanup
- No behavior changes that are not tied to best-practice alignment

## Impact
- Affected specs: build-system
- Affected code: `build.gradle.kts`, `settings.gradle.kts`, `gradle/` files, version catalog
