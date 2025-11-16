package io.statemodeler.cli;

import io.statemodeler.comparison.DdlComparisonService;
import io.statemodeler.dsl.ModelLoader;
import io.statemodeler.sql.DdlGenerators;
import io.statemodeler.validation.ModelValidators;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command to compare DDL between two SDD model files and display the diff.
 */
@Command(name = "diff", description = "Compare DDL between two SDD model files")
public class DiffCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(DiffCommand.class);

    @Parameters(index = "0", description = "Path to the current SDD model file (YAML or JSON)")
    private Path currentModelFile;

    @Parameters(index = "1", description = "Path to the future SDD model file (YAML or JSON)")
    private Path futureModelFile;

    @Option(
            names = {"--dialect"},
            description = "SQL dialect (supported: postgres)",
            defaultValue = "postgres")
    private String dialect;

    @Override
    public Integer call() throws Exception {
        logger.info("Comparing DDL between:");
        logger.info("  Current: {}", currentModelFile);
        logger.info("  Future:  {}", futureModelFile);
        logger.info("  Dialect: {}", dialect);

        try {
            // Check if dialect is supported
            if (!DdlGenerators.isSupported(dialect)) {
                logger.error("Error: Unsupported SQL dialect '{}'", dialect);
                logger.error("Supported dialects: {}", String.join(", ", DdlGenerators.getSupportedDialects()));
                return 1;
            }

            // Check if model files exist
            if (!Files.exists(currentModelFile)) {
                logger.error("Error: Current model file does not exist: {}", currentModelFile);
                return 1;
            }
            if (!Files.exists(futureModelFile)) {
                logger.error("Error: Future model file does not exist: {}", futureModelFile);
                return 1;
            }

            // Load and validate current model
            var currentLoader = ModelLoader.forFile(currentModelFile);
            var currentLoadResult = currentLoader.loadFromFile(currentModelFile);

            if (currentLoadResult.isFailure()) {
                logger.error("✗ Failed to parse current model file:");
                logger.error("  {}", currentLoadResult.getCause().getMessage());
                return 1;
            }

            var currentModel = currentLoadResult.get();
            logger.info("✓ Current model parsed: {}", currentModel.name());

            var validator = ModelValidators.getInstance();
            var currentValidation = validator.validate(currentModel);

            if (currentValidation.isInvalid()) {
                logger.error("✗ Current model validation failed:");
                for (var error : currentValidation.getError()) {
                    logger.error("  • {}", error.message());
                }
                return 1;
            }

            // Load and validate future model
            var futureLoader = ModelLoader.forFile(futureModelFile);
            var futureLoadResult = futureLoader.loadFromFile(futureModelFile);

            if (futureLoadResult.isFailure()) {
                logger.error("✗ Failed to parse future model file:");
                logger.error("  {}", futureLoadResult.getCause().getMessage());
                return 1;
            }

            var futureModel = futureLoadResult.get();
            logger.info("✓ Future model parsed: {}", futureModel.name());

            var futureValidation = validator.validate(futureModel);

            if (futureValidation.isInvalid()) {
                logger.error("✗ Future model validation failed:");
                for (var error : futureValidation.getError()) {
                    logger.error("  • {}", error.message());
                }
                return 1;
            }

            // Generate DDL for both models
            var generator = DdlGenerators.forDialect(dialect);
            var currentStatements = generator.generateDdl(currentModel);
            var currentDdl = String.join(";\n", currentStatements) + ";\n";
            logger.info("✓ Current DDL generated ({} chars)", currentDdl.length());

            var futureStatements = generator.generateDdl(futureModel);
            var futureDdl = String.join(";\n", futureStatements) + ";\n";
            logger.info("✓ Future DDL generated ({} chars)", futureDdl.length());

            // Compute and display diff
            var comparisonService = new DdlComparisonService();
            var comparison = comparisonService.compare(currentDdl, futureDdl);

            if (comparison.diff().isEmpty()) {
                logger.info("");
                logger.info("✓ No differences found - DDL schemas are identical");
                return 0;
            }

            // Display diff
            logger.info("");
            logger.info("DDL Diff:");
            logger.info("─".repeat(80));
            for (var line : comparison.diff()) {
                System.out.println(line);
            }
            logger.info("─".repeat(80));
            logger.info("✓ Diff generated ({} lines)", comparison.diff().size());

            return 0;

        } catch (Exception e) {
            logger.error("Error during DDL comparison: {}", e.getMessage(), e);
            return 1;
        }
    }
}
