package io.statemodeler.cli.commands;

import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.dsl.YamlModelLoader;
import io.statemodeler.sdr.DefaultSdrFactory;
import io.statemodeler.sdr.SdrFactory;
import io.statemodeler.validation.DefaultModelValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

/**
 * CLI command to register an SDD model as an SDR (State-Driven Record) in the
 * repository.
 *
 * <p>
 * Usage:
 *
 * <pre>{@code
 * sdd-modeler register orders-model.yaml
 * sdd-modeler register --repository /custom/path/repo.h2 orders-model.yaml
 * }</pre>
 *
 * <p>
 * The command:
 *
 * <ol>
 * <li>Loads and parses the YAML/JSON model file
 * <li>Validates the model structure
 * <li>Generates DDL SQL from the validated model
 * <li>Creates an SDR with schema hash and DDL hash
 * <li>Saves the SDR to the repository
 * </ol>
 */
@Command(name = "register", description = "Register an SDD model in the repository", mixinStandardHelpOptions = true)
public class RegisterCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the SDD model file (YAML or JSON)", paramLabel = "<model-file>")
    Path modelFile;

    @Option(
            names = {"--name", "-n"},
            description = "Model name (default: derived from filename)",
            paramLabel = "<name>")
    String modelName;

    @Option(
            names = {"--version", "-v"},
            description = "Model version (default: derived from model or '1.0')",
            paramLabel = "<version>")
    String modelVersion;

    @Mixin
    RepositoryMixin repositoryMixin;

    @Spec
    CommandSpec spec;

    private final YamlModelLoader loader = new YamlModelLoader();
    private final DefaultModelValidator validator = new DefaultModelValidator();
    private final SdrFactory sdrFactory = new DefaultSdrFactory();

    @Override
    public Integer call() {
        // Attempt to resolve provided path; CLI runs may be launched from different
        // working directories (e.g., Gradle project dir). When a relative path is
        // provided and not found, try to locate it relative to the repository root
        // (project workspace), or by searching typical example locations.
        modelFile = resolveModelFile(modelFile);
        spec.commandLine().getOut().println("Registering SDD model: " + modelFile);

        return io.vavr.control.Try.of(() -> {
                    String modelSource = Files.readString(modelFile);
                    String contentType =
                            modelFile.toString().endsWith(".json") ? "application/json" : "application/yaml";
                    return new java.util.AbstractMap.SimpleEntry<>(modelSource, contentType);
                })
                .flatMap(pair -> loader.loadFromFile(modelFile)
                        .map(model -> new java.util.AbstractMap.SimpleEntry<>(model, pair)))
                .map(entry -> {
                    var model = entry.getKey();
                    var pair = entry.getValue();
                    spec.commandLine().getOut().println("  Loaded model: " + model.name());

                    // Validate
                    validator.validateOrThrow(model);
                    spec.commandLine().getOut().println("  Validation: PASSED");

                    return new java.util.AbstractMap.SimpleEntry<>(model, pair);
                })
                .map(entry -> {
                    var model = entry.getKey();
                    var pair = entry.getValue();
                    String modelSource = pair.getKey();
                    String contentType = pair.getValue();

                    String sqlDialect = model.database().dialect();
                    var sdr = sdrFactory.create(modelSource, contentType, sqlDialect);

                    spec.commandLine().getOut().println("  Schema hash: " + sdr.schemaHash());
                    spec.commandLine().getOut().println("  DDL hash: " + sdr.ddlHash());

                    return new java.util.AbstractMap.SimpleEntry<>(sdr, model);
                })
                .flatMap(entry -> {
                    var sdr = entry.getKey();
                    var model = entry.getValue();
                    String finalName = resolveName(model.name());
                    String finalVersion = resolveVersion(model.version());

                    return io.vavr.control.Try.withResources(() -> repositoryMixin.createRepository())
                            .of(repo -> repo.save(sdr, finalName, finalVersion))
                            .flatMap(x -> x)
                            .map(ignored -> {
                                spec.commandLine().getOut().println("\n✓ Successfully registered SDR");
                                spec.commandLine().getOut().println("  Name: " + finalName);
                                spec.commandLine().getOut().println("  Version: " + finalVersion);
                                spec.commandLine().getOut().println("  Hash: " + sdr.schemaHash());
                                return 0;
                            });
                })
                .recoverWith(throwable -> {
                    Throwable cause = throwable instanceof IllegalArgumentException ? throwable : throwable.getCause();
                    if (cause != null
                            && cause.getMessage() != null
                            && cause.getMessage().contains("already exists")) {
                        spec.commandLine().getErr().println("ERROR: SDR already registered");
                        spec.commandLine().getErr().println("  " + cause.getMessage());
                        spec.commandLine().getErr().println("  Use 'sdd-modeler list' to view registered SDRs");
                        return io.vavr.control.Try.success(2);
                    }
                    return io.vavr.control.Try.failure(throwable);
                })
                .getOrElseGet(throwable -> {
                    spec.commandLine().getErr().println("ERROR: Failed to register SDR");
                    // Print the full stack trace to the CLI error writer to aid debugging
                    try {
                        var pw = spec.commandLine().getErr();
                        throwable.printStackTrace(pw);
                        pw.flush();
                    } catch (Exception e) {
                        // Best-effort: fallback to message
                        spec.commandLine().getErr().println("  " + throwable.getMessage());
                    }
                    return 1;
                });
    }

    private Path resolveModelFile(Path requested) {
        if (requested == null) {
            return requested;
        }
        if (Files.exists(requested)) {
            return requested;
        }

        var cwd = Path.of(System.getProperty("user.dir"));
        // Search upwards for project root (settings.gradle.kts or .git)
        Path repoRoot = cwd;
        while (repoRoot != null) {
            if (Files.exists(repoRoot.resolve("settings.gradle.kts")) || Files.exists(repoRoot.resolve(".git"))) {
                break;
            }
            repoRoot = repoRoot.getParent();
        }

        if (repoRoot != null) {
            var candidate = repoRoot.resolve(requested);
            if (Files.exists(candidate)) {
                spec.commandLine().getOut().println("  Resolved model path to: " + candidate);
                return candidate;
            }

            // If a path like 'scripts/examples/foo.yaml' was provided relative to workspace
            // try to find the file name under the scripts folder
            Path scriptsFolder = repoRoot.resolve("scripts");
            if (Files.exists(scriptsFolder)) {
                try (var stream = java.nio.file.Files.walk(scriptsFolder)) {
                    var fileName = requested.getFileName().toString();
                    var found = stream.filter(p -> p.getFileName().toString().equals(fileName))
                            .findFirst();
                    if (found.isPresent()) {
                        var f = found.get();
                        spec.commandLine().getOut().println("  Resolved model path to: " + f);
                        return f;
                    }
                } catch (Exception ignored) {
                    // Best-effort resolution, ignore exceptions and fallback to original path
                }
            }
        }

        // Fallback: unchanged requested path (original behavior)
        return requested;
    }

    /**
     * Resolves the model name from CLI option or model file.
     */
    private String resolveName(String modelName) {
        if (this.modelName != null && !this.modelName.isBlank()) {
            return this.modelName;
        }
        if (modelName != null && !modelName.isBlank()) {
            return modelName;
        }
        // Fallback: derive from filename
        String filename = modelFile.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    /**
     * Resolves the model version from CLI option or model version.
     */
    private String resolveVersion(String modelVersion) {
        if (this.modelVersion != null && !this.modelVersion.isBlank()) {
            return this.modelVersion;
        }
        if (modelVersion != null && !modelVersion.isBlank()) {
            return modelVersion;
        }
        return "1.0";
    }
}
