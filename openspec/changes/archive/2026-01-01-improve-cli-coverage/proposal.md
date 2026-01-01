# Change: Improve CLI coverage

## Why
Codecov shows ~70% coverage in `state-modeler-app/src/main/java/io/statemodeler/cli`, with gaps in util, Main entrypoint, and RepositoryMixin. Higher coverage will harden CLI behaviors and prevent regressions.

## What Changes
- Add tests to raise CLI package coverage (commands/util/main/repository mixin) to at least 85%.
- Cover error paths and option validation in RepositoryMixin and CliCommandHelpers.
- Add smoke test for CLI entrypoint (Main) with representative subcommand.

## Impact
- Affected specs: build-system
- Affected code: state-modeler-app CLI (commands/util/Main/RepositoryMixin), test suites
