# sdd-modeler

[![CI](https://github.com/ng-galien/sdd-modeler/actions/workflows/ci.yml/badge.svg)](https://github.com/ng-galien/sdd-modeler/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/ng-galien/sdd-modeler/graph/badge.svg)](https://codecov.io/gh/ng-galien/sdd-modeler)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.3-blue.svg)](https://gradle.org/)

A Java library and CLI tool for implementing **State-Driven Design (SDD)** with automatic SQL schema generation.

**SDD-Modeler** enables you to define your domain model as a declarative YAML/JSON schema describing entities, states, transitions, extensions, and projections. From this single source of truth, it generates production-ready PostgreSQL DDL with optimized state tracking patterns.

See related [blog post](https://ng-galien.github.io/tags/sdd/) for more context.

## 🧩 Modules

This project is organized into several modules, each with its own documentation:

### 📚 [Core Library](state-modeler-core/README.md)

The heart of the project. Contains the modeling concepts, validation logic, and SQL generation engine.

- **Key Features**: Declarative modeling, immutable state facts, robust validation.
- **Go here if**: You want to understand the SDD concepts or use the library programmatically.

### 🛠️ [CLI Tool](state-modeler-app/README.md)

A command-line interface for managing SDD models.

- **Key Features**: Validation, SQL generation, local repository management, AI-powered migrations.
- **Go here if**: You want to use the `state-modeler` command to manage your models.

### 🔌 [Gradle Plugin](state-modeler-gradle-plugin/README.md)

Integrate SDD generation into your Gradle build.

- **Key Features**: Automatic code generation during build, seamless integration with Java projects.
- **Go here if**: You want to use SDD Modeler in your Gradle project.

### 🧪 [Sample Project](sample/README.md)

A working example demonstrating the full stack.

- **Go here if**: You want to see a running application using SDD Modeler.

## 🤝 Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure `./gradlew spotlessApply` passes
5. Submit a pull request

Note about scripts: The manual testing scripts under `scripts/` now default to using the `--examples-test` models (test resources in `state-modeler-app/src/test/resources/examples`) for richer validation. Additionally, the functional tests (`test-ddl-functional.sh`) include an early cleanup trap that removes any leftover Docker containers if the script fails early to avoid blocking subsequent test runs.

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

Built with:

- [Jackson](https://github.com/FasterXML/jackson) for YAML/JSON parsing
- [Vavr](https://www.vavr.io/) for functional validation
- [Picocli](https://picocli.info/) for CLI framework
- [LangChain4j](https://github.com/langchain4j/langchain4j) for LLM integration
- [JUnit 5](https://junit.org/junit5/) for unit testing
