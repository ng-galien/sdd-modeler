package io.statemodeler.cli.commands;

import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI command to show details of a registered SDR.
 *
 * <p>
 * Usage: sdd-modeler show <hash|name[:version]> [--format
 * metadata|schema|ddl|all]
 *
 * <p>
 * Examples:
 * <ul>
 * <li>sdd-modeler show 222fa0d3 (show by short hash)
 * <li>sdd-modeler show orders-sdd-example (show latest version by name)
 * <li>sdd-modeler show orders-sdd-example:0.1 (show specific version)
 * <li>sdd-modeler show 222fa0d3 --format schema (show schema JSON only)
 * </ul>
 */
@Command(name = "show", description = "Show details of a registered SDR", mixinStandardHelpOptions = true)
public class ShowCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShowCommand.class);

    @Parameters(index = "0", description = "SDR hash or name[:version] to display")
    String identifier;

    @Option(names = { "--format",
            "-f" }, description = "Output format: all (default), metadata, schema, ddl", defaultValue = "all")
    String format;

    @Mixin
    RepositoryMixin repositoryMixin;

    // Output writer for CLI content printing. Default to System.out; tests can
    // override.
    PrintWriter output = new PrintWriter(System.out, true);

    public void setOutput(PrintWriter output) {
        this.output = output;
    }

    @Override
    public Integer call() {
        // Validate format
        if (!isValidFormat(format)) {
            logger.error("ERROR: Invalid format '{}'", format);
            logger.error("  Supported formats: all, metadata, schema, ddl");
            return 1;
        }

        return Try.withResources(() -> repositoryMixin.createRepository())
                .of(repository -> findSdr(repository, identifier)
                        .flatMap(sdrOpt -> sdrOpt.map(Try::success)
                                .orElseGet(() -> Try.failure(new IllegalArgumentException("SDR not found"))))
                        .map(sdr -> {
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
                        })
                        .getOrElseThrow(e -> e))
                .fold(
                        throwable -> {
                            if (throwable instanceof IllegalArgumentException
                                    && "SDR not found".equals(throwable.getMessage())) {
                                logger.error("ERROR: SDR not found");
                                logger.error("  Identifier: {}", identifier);
                                logger.error("  Use 'sdd-modeler list' to view registered SDRs");
                                return 1;
                            }
                            logger.error("ERROR: Repository error");
                            logger.error("  {}", throwable.getMessage());
                            return 1;
                        },
                        result -> result);
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

        // Otherwise, treat as name (find latest version by name, then fetch full
        // record)
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
        output.println("=== SDR Metadata ===");
        output.println("Schema Hash:       " + sdr.schemaHash());
        output.println("Content Type:      " + sdr.contentType());
        output.println("DDL Hash:          " + sdr.ddlHash());
        output.println("SDR Version:       " + sdr.version());
        output.println("Build Fingerprint: " + sdr.buildFingerprint());
    }

    private void printSchema(SdrRecord sdr) {
        output.println("=== Schema (JSON) ===");
        output.println(sdr.schema());
    }

    private void printDdl(SdrRecord sdr) {
        output.println("=== DDL (SQL) ===");
        output.println(sdr.ddl());
    }

    private void printAll(SdrRecord sdr) {
        printMetadata(sdr);
        output.println();
        printSchema(sdr);
        output.println();
        printDdl(sdr);
    }
}
