package io.statemodeler.cli;

import io.statemodeler.sdr.SdrRecord;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI command to show details of a registered SDR.
 *
 * <p>Usage: sdd-modeler show <hash|name[:version]> [--format metadata|schema|ddl|all]
 *
 * <p>Examples:
 * <ul>
 *   <li>sdd-modeler show 222fa0d3 (show by short hash)
 *   <li>sdd-modeler show orders-sdd-example (show latest version by name)
 *   <li>sdd-modeler show orders-sdd-example:0.1 (show specific version)
 *   <li>sdd-modeler show 222fa0d3 --format schema (show schema JSON only)
 * </ul>
 */
@Command(name = "show", description = "Show details of a registered SDR", mixinStandardHelpOptions = true)
public class ShowCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "SDR hash or name[:version] to display")
    String identifier;

    @Option(
            names = {"--format", "-f"},
            description = "Output format: all (default), metadata, schema, ddl",
            defaultValue = "all")
    String format;

    @Mixin
    RepositoryMixin repositoryMixin;

    @Override
    public Integer call() {
        // Validate format
        if (!isValidFormat(format)) {
            System.err.println("ERROR: Invalid format '" + format + "'");
            System.err.println("  Supported formats: all, metadata, schema, ddl");
            return 1;
        }

        try (var repository = repositoryMixin.createRepository()) {
            // Try to find SDR
            var result = findSdr(repository, identifier);

            if (result.isFailure()) {
                System.err.println("ERROR: Failed to retrieve SDR");
                System.err.println("  " + result.getCause().getMessage());
                return 1;
            }

            var sdrOpt = result.get();
            if (sdrOpt.isEmpty()) {
                System.err.println("ERROR: SDR not found");
                System.err.println("  Identifier: " + identifier);
                System.err.println("  Use 'sdd-modeler list' to view registered SDRs");
                return 1;
            }

            SdrRecord sdr = sdrOpt.get();

            // Display based on format
            switch (format.toLowerCase()) {
                case "metadata":
                    printMetadata(sdr);
                    break;
                case "schema":
                    printSchema(sdr);
                    break;
                case "ddl":
                    printDdl(sdr);
                    break;
                case "all":
                default:
                    printAll(sdr);
                    break;
            }

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: Repository error");
            System.err.println("  " + e.getMessage());
            return 1;
        }
    }

    private io.vavr.control.Try<java.util.Optional<SdrRecord>> findSdr(
            io.statemodeler.repository.SdrRepository repository, String id) {
        // Check if it's a name[:version] pattern
        if (id.contains(":")) {
            String[] parts = id.split(":", 2);
            String name = parts[0];
            String version = parts[1];
            return repository.findByNameAndVersion(name, version);
        }

        // Check if it's a hash (hexadecimal characters only)
        if (id.matches("^[a-fA-F0-9]+$")) {
            return repository.findByHash(id);
        }

        // Otherwise, treat as name (find latest version by name, then fetch full record)
        return repository.findByName(id).flatMap(metadataList -> {
            if (metadataList.isEmpty()) {
                return io.vavr.control.Try.success(java.util.Optional.<SdrRecord>empty());
            }
            // Get the most recent (first in descending order)
            String hash = metadataList.get(0).schemaHash();
            return repository.findByHash(hash);
        });
    }

    private boolean isValidFormat(String fmt) {
        return fmt != null
                && (fmt.equalsIgnoreCase("all")
                        || fmt.equalsIgnoreCase("metadata")
                        || fmt.equalsIgnoreCase("schema")
                        || fmt.equalsIgnoreCase("ddl"));
    }

    private void printMetadata(SdrRecord sdr) {
        System.out.println("=== SDR Metadata ===");
        System.out.println("Schema Hash:       " + sdr.schemaHash());
        System.out.println("Content Type:      " + sdr.contentType());
        System.out.println("DDL Hash:          " + sdr.ddlHash());
        System.out.println("SDR Version:       " + sdr.version());
        System.out.println("Build Fingerprint: " + sdr.buildFingerprint());
    }

    private void printSchema(SdrRecord sdr) {
        System.out.println("=== Schema (JSON) ===");
        System.out.println(sdr.schema());
    }

    private void printDdl(SdrRecord sdr) {
        System.out.println("=== DDL (SQL) ===");
        System.out.println(sdr.ddl());
    }

    private void printAll(SdrRecord sdr) {
        printMetadata(sdr);
        System.out.println();
        printSchema(sdr);
        System.out.println();
        printDdl(sdr);
    }
}
