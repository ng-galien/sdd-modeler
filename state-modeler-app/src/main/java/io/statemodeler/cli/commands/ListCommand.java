package io.statemodeler.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.cli.dto.SdrListResponse;
import io.statemodeler.repository.SdrMetadata;
import java.io.PrintWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * CLI command to list registered SDRs in the repository.
 *
 * <p>
 * Usage: sdd-modeler list [--format table|json|yaml] [--limit N]
 */
@Command(name = "list", description = "List registered SDRs in the repository", mixinStandardHelpOptions = true)
public class ListCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ListCommand.class);

    @Option(names = { "--format",
            "-f" }, description = "Output format: table (default), json, yaml", defaultValue = "table")
    String format;

    @Option(names = { "--limit",
            "-l" }, description = "Maximum number of SDRs to display (0 = all)", defaultValue = "0")
    int limit;

    @Mixin
    RepositoryMixin repositoryMixin;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
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
            logger.error("  Supported formats: table, json, yaml");
            return 1;
        }

        try (var repository = repositoryMixin.createRepository()) {
            // Fetch SDRs
            var result = limit > 0 ? repository.findRecent(limit) : repository.listAll();

            if (result.isFailure()) {
                logger.error("ERROR: Failed to list SDRs");
                logger.error("  {}", result.getCause().getMessage());
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
            logger.error("ERROR: Repository error");
            logger.error("  {}", e.getMessage());
            return 1;
        }
    }

    private boolean isValidFormat(String fmt) {
        return fmt != null
                && (fmt.equalsIgnoreCase("table") || fmt.equalsIgnoreCase("json") || fmt.equalsIgnoreCase("yaml"));
    }

    private void printTable(List<SdrMetadata> sdrs) {
        if (sdrs.isEmpty()) {
            output.println("No SDRs registered in repository");
            return;
        }

        // Print header
        output.println(
                String.format("%-40s %-20s %-12s %-12s %s", "NAME", "VERSION", "HASH", "SDR VERSION", "CREATED AT"));
        output.println("-".repeat(110));

        // Print rows
        for (SdrMetadata sdr : sdrs) {
            output.println(String.format(
                    "%-40s %-20s %-12s %-12s %s",
                    truncate(sdr.modelName(), 40),
                    truncate(sdr.modelVersion(), 20),
                    sdr.shortHash(),
                    sdr.sdrVersion(),
                    DATE_FORMATTER.format(sdr.createdAt())));
        }

        output.println("\nTotal: " + sdrs.size() + " SDR(s)");
    }

    private void printJson(List<SdrMetadata> sdrs) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            SdrListResponse response = SdrListResponse.from(sdrs);
            // Write directly to the output writer
            // We use writeValueAsString to avoid closing the writer if we passed it
            // directly
            output.println(mapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.error("ERROR: Failed to generate JSON output", e);
        }
    }

    private void printYaml(List<SdrMetadata> sdrs) {
        try {
            ObjectMapper mapper = new ObjectMapper(
                    new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
            mapper.findAndRegisterModules();
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            SdrListResponse response = SdrListResponse.from(sdrs);
            output.println(mapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.error("ERROR: Failed to generate YAML output", e);
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
