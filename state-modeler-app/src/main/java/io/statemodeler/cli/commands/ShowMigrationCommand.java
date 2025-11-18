package io.statemodeler.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.cli.dto.MigrationDto;
import io.statemodeler.cli.dto.SdrSummaryDto;
import io.statemodeler.cli.dto.ShowMigrationDto;
import io.statemodeler.comparison.DdlComparison;
import io.statemodeler.comparison.DdlComparisonService;
import io.statemodeler.repository.SdrMigration;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.io.File;
// Files import not needed with Jackson ObjectMapper JSON serialization
import java.util.Optional;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI command to retrieve migration information between two SDR versions.
 *
 * Usage: sdd-modeler show-migration <from> <to> [--output-json output.json]
 */
@Command(
        name = "show-migration",
        description = "Retrieve migration info (orig/new ddl, diff, migration JSON)",
        mixinStandardHelpOptions = true)
public class ShowMigrationCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShowMigrationCommand.class);

    @Parameters(index = "0", description = "Source SDR hash or name:version")
    String fromIdentifier;

    @Parameters(index = "1", description = "Target SDR hash or name:version")
    String toIdentifier;

    @Option(
            names = {"--output-json", "-j"},
            description = "Write JSON output to file (default: stdout)")
    File outputJson;

    @Mixin
    RepositoryMixin repositoryMixin;

    @Override
    public Integer call() {
        try (var repository = repositoryMixin.createRepository()) {
            // Resolve from and to SDRs
            var fromResult = findSdr(repository, fromIdentifier);
            if (fromResult.isFailure()) {
                logger.error("ERROR: Failed to retrieve source SDR");
                logger.error("  {}", fromResult.getCause().getMessage());
                return 1;
            }
            var fromOpt = fromResult.get();
            if (fromOpt.isEmpty()) {
                logger.error("ERROR: Source SDR not found: {}", fromIdentifier);
                return 1;
            }
            SdrRecord fromSdr = fromOpt.get();

            var toResult = findSdr(repository, toIdentifier);
            if (toResult.isFailure()) {
                logger.error("ERROR: Failed to retrieve target SDR");
                logger.error("  {}", toResult.getCause().getMessage());
                return 1;
            }
            var toOpt = toResult.get();
            if (toOpt.isEmpty()) {
                logger.error("ERROR: Target SDR not found: {}", toIdentifier);
                return 1;
            }
            SdrRecord toSdr = toOpt.get();

            // Use services
            var comparisonService = new DdlComparisonService();

            // Compute diff
            DdlComparison comparison = comparisonService.compare(fromSdr.ddl(), toSdr.ddl());
            String textDiff = comparison.diff().isEmpty() ? "" : String.join("\n", comparison.diff());

            // Find persisted migration via repository
            var migrationResult = repository.findMigration(fromSdr.schemaHash(), toSdr.schemaHash());
            if (migrationResult.isFailure()) {
                logger.error("ERROR: Failed to fetch migration from repository");
                logger.error("  {}", migrationResult.getCause().getMessage());
                return 1;
            }
            Optional<SdrMigration> maybeMigration = migrationResult.get();

            var original = new SdrSummaryDto(fromSdr.schemaHash(), fromSdr.ddl());
            var newSdrSummary = new SdrSummaryDto(toSdr.schemaHash(), toSdr.ddl());
            MigrationDto migrationOutput = null;
            if (maybeMigration.isPresent()) {
                var m = maybeMigration.get();
                migrationOutput = new MigrationDto(m.confidence(), m.comments(), m.migrationScript());
            }

            var output = new ShowMigrationDto(original, newSdrSummary, textDiff, migrationOutput);

            if (outputJson != null) {
                var mapper = new ObjectMapper();
                mapper.writeValue(outputJson, output);
                logger.info("Wrote JSON to {}", outputJson.getAbsolutePath());
            } else {
                var mapper = new ObjectMapper();
                System.out.println(mapper.writeValueAsString(output));
            }

            return 0;
        } catch (Exception e) {
            logger.error("ERROR: Unexpected error");
            logger.error("  {}", e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private Try<Optional<SdrRecord>> findSdr(io.statemodeler.repository.SdrRepository repository, String identifier) {
        if (!identifier.contains(":")) {
            return repository.findByHash(identifier);
        }
        String[] parts = identifier.split(":", 2);
        String name = parts[0];
        String version = parts.length > 1 ? parts[1] : null;
        if (version != null) {
            return repository.findByNameAndVersion(name, version);
        }
        // No version provided: find the most recent metadata and then load the full record
        return repository.findByName(name).flatMap(list -> {
            if (list == null || list.isEmpty()) {
                return Try.success(Optional.empty());
            }
            String hash = list.get(0).schemaHash();
            return repository.findByHash(hash);
        });
    }

    // jsonEscape removed; Jackson ObjectMapper handles JSON serialization safely
}
