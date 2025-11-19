package io.statemodeler.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.cli.dto.SdrListResponse;
import io.statemodeler.repository.SdrMetadata;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * CLI command to list registered SDRs in the repository.
 *
 * <p>
 * Usage: sdd-modeler list [--format table|json|yaml] [--limit N]
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

    @Spec
    CommandSpec spec;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public Integer call() {
        // Validate format
        if (!isValidFormat(format)) {
            spec.commandLine().getErr().println("ERROR: Invalid format '" + format + "'");
            spec.commandLine().getErr().println("  Supported formats: table, json, yaml");
            return 1;
        }

        return io.vavr.control.Try.withResources(() -> repositoryMixin.createRepository())
                .of(repository -> (limit > 0 ? repository.findRecent(limit) : repository.listAll())
                        .map(sdrs -> {
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
                        })
                        .getOrElseThrow(e -> e))
                .fold(
                        throwable -> {
                            spec.commandLine().getErr().println("ERROR: Failed to list SDRs");
                            spec.commandLine().getErr().println("  " + throwable.getMessage());
                            return 1;
                        },
                        result -> result);
    }

    private boolean isValidFormat(String fmt) {
        return fmt != null
                && (fmt.equalsIgnoreCase("table") || fmt.equalsIgnoreCase("json") || fmt.equalsIgnoreCase("yaml"));
    }

    private void printTable(List<SdrMetadata> sdrs) {
        if (sdrs.isEmpty()) {
            spec.commandLine().getOut().println("No SDRs registered in repository");
            return;
        }

        // Print header
        spec.commandLine()
                .getOut()
                .println(String.format(
                        "%-40s %-20s %-12s %-12s %s", "NAME", "VERSION", "HASH", "SDR VERSION", "CREATED AT"));
        spec.commandLine().getOut().println("-".repeat(110));

        // Print rows
        for (SdrMetadata sdr : sdrs) {
            spec.commandLine()
                    .getOut()
                    .println(String.format(
                            "%-40s %-20s %-12s %-12s %s",
                            truncate(sdr.modelName(), 40),
                            truncate(sdr.modelVersion(), 20),
                            sdr.shortHash(),
                            sdr.sdrVersion(),
                            DATE_FORMATTER.format(sdr.createdAt())));
        }

        spec.commandLine().getOut().println("\nTotal: " + sdrs.size() + " SDR(s)");
    }

    private void printJson(List<SdrMetadata> sdrs) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            SdrListResponse response = SdrListResponse.from(sdrs);
            spec.commandLine().getOut().println(mapper.writeValueAsString(response));
        } catch (Exception e) {
            spec.commandLine().getErr().println("ERROR: Failed to generate JSON output");
            e.printStackTrace(spec.commandLine().getErr());
        }
    }

    private void printYaml(List<SdrMetadata> sdrs) {
        try {
            ObjectMapper mapper =
                    new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
            mapper.findAndRegisterModules();
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            SdrListResponse response = SdrListResponse.from(sdrs);
            spec.commandLine().getOut().println(mapper.writeValueAsString(response));
        } catch (Exception e) {
            spec.commandLine().getErr().println("ERROR: Failed to generate YAML output");
            e.printStackTrace(spec.commandLine().getErr());
        }
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
}
