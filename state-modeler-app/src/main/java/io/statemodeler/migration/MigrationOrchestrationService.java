package io.statemodeler.migration;

import io.statemodeler.comparison.DdlComparison;
import io.statemodeler.comparison.DdlComparisonService;
import io.statemodeler.repository.SdrMigration;
import io.statemodeler.repository.SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.time.Instant;

/**
 * Service that orchestrates migration script generation and persistence.
 *
 * <p>Combines {@link MigrationGenerationService} with {@link SdrRepository} to generate and store
 * migration scripts between two SDR versions.
 */
public class MigrationOrchestrationService {

    private final MigrationGenerationService migrationGenerator;
    private final DdlComparisonService comparisonService;
    private final SdrRepository repository;

    /**
     * Creates a new migration orchestration service.
     *
     * @param migrationGenerator LLM-based migration script generator
     * @param comparisonService service for comparing DDL schemas
     * @param repository repository for persisting migrations
     * @throws IllegalArgumentException if any parameter is null
     */
    public MigrationOrchestrationService(
            MigrationGenerationService migrationGenerator,
            DdlComparisonService comparisonService,
            SdrRepository repository) {
        if (migrationGenerator == null) {
            throw new IllegalArgumentException("migrationGenerator cannot be null");
        }
        if (comparisonService == null) {
            throw new IllegalArgumentException("comparisonService cannot be null");
        }
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        this.migrationGenerator = migrationGenerator;
        this.comparisonService = comparisonService;
        this.repository = repository;
    }

    /**
     * Generates and saves a migration script between two SDR versions.
     *
     * <p>Steps:
     * <ol>
     *   <li>Computes diff between old and new DDL using {@link DdlComparisonService}
     *   <li>Generates migration script using {@link MigrationGenerationService}
     *   <li>Persists migration in {@link SdrRepository}
     * </ol>
     *
     * @param fromSdr source SDR version
     * @param toSdr target SDR version
     * @param dialect database dialect (e.g., "postgres")
     * @return Success with migration if generated and saved, Failure if error occurs
     */
    public Try<SdrMigration> generateAndSaveMigration(SdrRecord fromSdr, SdrRecord toSdr, String dialect) {
        if (fromSdr == null) {
            return Try.failure(new IllegalArgumentException("fromSdr cannot be null"));
        }
        if (toSdr == null) {
            return Try.failure(new IllegalArgumentException("toSdr cannot be null"));
        }
        if (dialect == null || dialect.isBlank()) {
            return Try.failure(new IllegalArgumentException("dialect cannot be null or blank"));
        }

        // Step 1: Compare DDLs
        DdlComparison comparison = comparisonService.compare(fromSdr.ddl(), toSdr.ddl());
        String textDiff = formatDiff(comparison);

        // Step 2: Generate migration script via LLM
        var scriptResult = migrationGenerator.generateMigrationScript(fromSdr.ddl(), toSdr.ddl(), textDiff, dialect);
        if (scriptResult.isFailure()) {
            return Try.failure(scriptResult.getCause());
        }

        String migrationScript = scriptResult.get();

        // Step 3: Create and persist migration
        var migration =
                new SdrMigration(fromSdr.schemaHash(), toSdr.schemaHash(), migrationScript, dialect, Instant.now());

        return repository.saveMigration(migration).map(v -> migration);
    }

    /**
     * Retrieves an existing migration between two SDR versions.
     *
     * @param fromHash source SDR hash
     * @param toHash target SDR hash
     * @return Success with Optional migration, Failure if database error
     */
    public Try<java.util.Optional<SdrMigration>> findMigration(String fromHash, String toHash) {
        return repository.findMigration(fromHash, toHash);
    }

    /**
     * Formats a {@link DdlComparison} as a text diff summary for LLM prompt.
     */
    private String formatDiff(DdlComparison comparison) {
        if (comparison.diff().isEmpty()) {
            return "No differences detected";
        }

        var sb = new StringBuilder();
        sb.append("DDL Differences:\n");
        for (var diffLine : comparison.diff()) {
            sb.append(diffLine).append("\n");
        }
        return sb.toString();
    }
}
