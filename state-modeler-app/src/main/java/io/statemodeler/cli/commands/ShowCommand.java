package io.statemodeler.cli.commands;

import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

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

    @Parameters(index = "0", description = "SDR hash or name[:version] to display")
    String identifier;

    @Option(
            names = {"--format", "-f"},
            description = "Output format: all (default), metadata, schema, ddl",
            defaultValue = "all")
    String format;

    @Mixin
    RepositoryMixin repositoryMixin;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        // Validate format
        if (!isValidFormat(format)) {
            spec.commandLine().getErr().println("ERROR: Invalid format '" + format + "'");
            spec.commandLine().getErr().println("  Supported formats: all, metadata, schema, ddl");
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
                                spec.commandLine().getErr().println("ERROR: SDR not found");
                                spec.commandLine().getErr().println("  Identifier: " + identifier);
                                spec.commandLine().getErr().println("  Use 'sdd-modeler list' to view registered SDRs");
                                return 1;
                            }
                            spec.commandLine().getErr().println("ERROR: Repository error");
                            spec.commandLine().getErr().println("  " + throwable.getMessage());
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
        spec.commandLine().getOut().println("=== SDR Metadata ===");
        spec.commandLine().getOut().println("Schema Hash:       " + sdr.schemaHash());
        spec.commandLine().getOut().println("Content Type:      " + sdr.contentType());
        spec.commandLine().getOut().println("DDL Hash:          " + sdr.ddlHash());
        spec.commandLine().getOut().println("SDR Version:       " + sdr.version());
        spec.commandLine().getOut().println("Build Fingerprint: " + sdr.buildFingerprint());
    }

    private void printSchema(SdrRecord sdr) {
        spec.commandLine().getOut().println("=== Schema (JSON) ===");
        spec.commandLine().getOut().println(sdr.schema());
    }

    private void printDdl(SdrRecord sdr) {
        spec.commandLine().getOut().println("=== DDL (SQL) ===");
        spec.commandLine().getOut().println(sdr.ddl());
    }

    private void printAll(SdrRecord sdr) {
        printMetadata(sdr);
        spec.commandLine().getOut().println();
        printSchema(sdr);
        spec.commandLine().getOut().println();
        printDdl(sdr);
    }
}
