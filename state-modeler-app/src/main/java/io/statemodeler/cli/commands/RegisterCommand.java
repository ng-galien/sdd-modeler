package io.statemodeler.cli.commands;

import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.dsl.YamlModelLoader;
import io.statemodeler.sdr.DefaultSdrFactory;
import io.statemodeler.sdr.SdrFactory;
import io.statemodeler.validation.DefaultModelValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.*;

/**
 * CLI command to register an SDD model as an SDR (State-Driven Record) in the repository.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * sdd-modeler register orders-model.yaml
 * sdd-modeler register --repository /custom/path/repo.h2 orders-model.yaml
 * }</pre>
 *
 * <p>The command:
 *
 * <ol>
 *   <li>Loads and parses the YAML/JSON model file
 *   <li>Validates the model structure
 *   <li>Generates DDL SQL from the validated model
 *   <li>Creates an SDR with schema hash and DDL hash
 *   <li>Saves the SDR to the repository
 * </ol>
 */
@Command(name = "register", description = "Register an SDD model in the repository", mixinStandardHelpOptions = true)
public class RegisterCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(RegisterCommand.class);

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

    private final YamlModelLoader loader = new YamlModelLoader();
    private final DefaultModelValidator validator = new DefaultModelValidator();
    private final SdrFactory sdrFactory = new DefaultSdrFactory();

    @Override
    public Integer call() {
        logger.info("Registering SDD model: {}", modelFile);

        // Read -> load -> validate -> create -> save using Vavr Try flows
        return io.vavr.control.Try.of(() -> {
                    String modelSource = Files.readString(modelFile);
                    String contentType =
                            modelFile.toString().endsWith(".json") ? "application/json" : "application/yaml";
                    var model = loader.loadFromFile(modelFile).get();
                    return new java.util.AbstractMap.SimpleEntry<>(
                            model, new java.util.AbstractMap.SimpleEntry<>(modelSource, contentType));
                })
                .fold(
                        throwable -> {
                            logger.error("ERROR: Failed to parse model file");
                            logger.error("  {}", throwable.getMessage());
                            return 1;
                        },
                        entry -> {
                            var model = entry.getKey();
                            var pair = entry.getValue();
                            String modelSource = pair.getKey();
                            String contentType = pair.getValue();
                            logger.info("  Loaded model: {}", model.name());
                            var validationResult = validator.validate(model);
                            if (validationResult.isInvalid()) {
                                logger.error("ERROR: Model validation failed");
                                validationResult.getError().forEach(err -> logger.error("  - {}", err.message()));
                                return 1;
                            }

                            logger.info("  Validation: PASSED");

                            String sqlDialect = model.database().dialect();
                            var sdr = sdrFactory.create(modelSource, contentType, sqlDialect);

                            logger.info("  Schema hash: {}", sdr.schemaHash());
                            logger.info("  DDL hash: {}", sdr.ddlHash());

                            String finalName = resolveName(model.name());
                            String finalVersion = resolveVersion(model.version());

                            // Save to repository; use Try to flatten nested Try from repository.save
                            return io.vavr.control.Try.of(() -> {
                                        try (var repo = repositoryMixin.createRepository()) {
                                            return repo.save(sdr, finalName, finalVersion);
                                        }
                                    })
                                    .flatMap(x -> x)
                                    .fold(
                                            saveErr -> {
                                                Throwable cause = saveErr instanceof IllegalArgumentException
                                                        ? saveErr
                                                        : saveErr.getCause();
                                                if (cause instanceof IllegalArgumentException
                                                        && cause.getMessage().contains("already exists")) {
                                                    logger.error("ERROR: SDR already registered");
                                                    logger.error(
                                                            "  An SDR with hash {} already exists in the repository",
                                                            sdr.schemaHash());
                                                    logger.error("  Use 'sdd-modeler list' to view registered SDRs");
                                                    return 2;
                                                }
                                                logger.error("ERROR: Failed to save SDR to repository");
                                                logger.error("  {}", cause.getMessage());
                                                return 1;
                                            },
                                            ignored -> {
                                                logger.info("\n✓ Successfully registered SDR");
                                                logger.info("  Name: {}", finalName);
                                                logger.info("  Version: {}", finalVersion);
                                                logger.info("  Hash: {}", sdr.schemaHash());
                                                return 0;
                                            });
                        });
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
