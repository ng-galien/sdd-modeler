# Change: Add Maven Plugin Module

## Why
Teams using Maven need first-class support to run SDD code and DDL generation without switching to
Gradle. Providing a Maven plugin keeps build tooling parity and broadens adoption.

## What Changes
- Introduce a Maven plugin module that mirrors the existing Gradle plugin goals and configuration
  (model path, output directories, toggles for controllers/repositories/MCP, Liquibase switch)
- Wire the Maven plugin build to depend on `state-modeler-core` via the reactor instead of
  requiring published artifacts
- Provide documentation and a runnable Maven sample showing typical usage and defaults
- Ensure CI can build and exercise the Maven plugin alongside the Gradle plugin

## Impact
- Affected specs: build-system
- Affected code: new Maven plugin module, Maven sample, build/CI wiring, shared plugin docs
