package io.statemodeler.cli.commands;

import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.repository.SdrMetadata;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * CLI command to list registered SDRs in the repository.
 *
 * <p>Usage: sdd-modeler list [--format table|json|yaml] [--limit N]
 */
@Command(name = "list", description = "List registered SDRs in the repository", mixinStandardHelpOptions = true)
public class ListCommand implements Callable<Integer> {

    @Option(
            names = {"--format", "-f"},
            description = "Output format: table (default), json, yaml",
            defaultValue = "table")
    String format;

    @Option(
            names = {"--limit", "-l"},
            description = "Maximum number of SDRs to display (0 = all)",
            defaultValue = "0")
    int limit;

    @Mixin
    RepositoryMixin repositoryMixin;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public Integer call() {
        // Validate format
        if (!isValidFormat(format)) {
            System.err.println("ERROR: Invalid format '" + format + "'");
            System.err.println("  Supported formats: table, json, yaml");
            return 1;
        }

        try (var repository = repositoryMixin.createRepository()) {
            // Fetch SDRs
            var result = limit > 0 ? repository.findRecent(limit) : repository.listAll();

            if (result.isFailure()) {
                System.err.println("ERROR: Failed to list SDRs");
                System.err.println("  " + result.getCause().getMessage());
                return 1;
            }

            List<SdrMetadata> sdrs = result.get();

            // Display based on format
            switch (format.toLowerCase()) {
                case "json":
                    printJson(sdrs);
                    break;
                case "yaml":
                    printYaml(sdrs);
                    break;
                case "table":
                default:
                    printTable(sdrs);
                    break;
            }

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: Repository error");
            System.err.println("  " + e.getMessage());
            return 1;
        }
    }

    private boolean isValidFormat(String fmt) {
        return fmt != null
                && (fmt.equalsIgnoreCase("table") || fmt.equalsIgnoreCase("json") || fmt.equalsIgnoreCase("yaml"));
    }

    private void printTable(List<SdrMetadata> sdrs) {
        if (sdrs.isEmpty()) {
            System.out.println("No SDRs registered in repository");
            return;
        }

        // Print header
        System.out.println(
                String.format("%-40s %-20s %-12s %-12s %s", "NAME", "VERSION", "HASH", "SDR VERSION", "CREATED AT"));
        System.out.println("-".repeat(110));

        // Print rows
        for (SdrMetadata sdr : sdrs) {
            System.out.println(String.format(
                    "%-40s %-20s %-12s %-12s %s",
                    truncate(sdr.modelName(), 40),
                    truncate(sdr.modelVersion(), 20),
                    sdr.shortHash(),
                    sdr.sdrVersion(),
                    DATE_FORMATTER.format(sdr.createdAt())));
        }

        System.out.println("\nTotal: " + sdrs.size() + " SDR(s)");
    }

    private void printJson(List<SdrMetadata> sdrs) {
        System.out.println("{");
        System.out.println("  \"sdrs\": [");

        for (int i = 0; i < sdrs.size(); i++) {
            SdrMetadata sdr = sdrs.get(i);
            System.out.println("    {");
            System.out.println("      \"name\": \"" + escapeJson(sdr.modelName()) + "\",");
            System.out.println("      \"version\": \"" + escapeJson(sdr.modelVersion()) + "\",");
            System.out.println("      \"hash\": \"" + sdr.schemaHash() + "\",");
            System.out.println("      \"sdrVersion\": \"" + sdr.sdrVersion() + "\",");
            System.out.println("      \"buildFingerprint\": \"" + sdr.buildFingerprint() + "\",");
            System.out.println("      \"createdAt\": \"" + sdr.createdAt() + "\"");
            System.out.print("    }");
            if (i < sdrs.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }

        System.out.println("  ],");
        System.out.println("  \"total\": " + sdrs.size());
        System.out.println("}");
    }

    private void printYaml(List<SdrMetadata> sdrs) {
        System.out.println("sdrs:");

        if (sdrs.isEmpty()) {
            System.out.println("  []");
        } else {
            for (SdrMetadata sdr : sdrs) {
                System.out.println("  - name: \"" + escapeYaml(sdr.modelName()) + "\"");
                System.out.println("    version: \"" + escapeYaml(sdr.modelVersion()) + "\"");
                System.out.println("    hash: \"" + sdr.schemaHash() + "\"");
                System.out.println("    sdrVersion: \"" + sdr.sdrVersion() + "\"");
                System.out.println("    buildFingerprint: \"" + sdr.buildFingerprint() + "\"");
                System.out.println("    createdAt: \"" + sdr.createdAt() + "\"");
            }
        }

        System.out.println("total: " + sdrs.size());
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String escapeYaml(String str) {
        if (str == null) {
            return "";
        }
        // Basic YAML escaping for quoted strings
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
