package io.statemodeler.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.statemodeler.repository.SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine.Model.CommandSpec;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;

/**
 * Shared helpers for CLI commands.
 */
public final class CliCommandHelpers {
    private CliCommandHelpers() {}

    public static Try<String> checkHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return Try.failure(new IllegalArgumentException("Hash cannot be empty"));
        }
        return Try.success(hash);
    }

    public static Try<SdrRecord> findByHash(CommandSpec spec, SdrRepository repository, String hash) {
        return checkHash(hash)
                .onFailure(ex -> spec.commandLine().getErr().println("ERROR: " + ex.getMessage()))
                .flatMap(repository::findByHash)
                .onFailure(ex -> spec.commandLine().getErr().println("ERROR: Failed to retrieve SDR"))
                .map(io.vavr.control.Option::ofOptional)
                .flatMap(opt -> opt.toTry(() -> new IllegalArgumentException("SDR not found")))
                .onFailure(ex -> spec.commandLine().getErr().println("ERROR: " + ex.getMessage()));
    }

    public static Try<Void> deleteSdr(CommandSpec spec, SdrRepository repository, SdrRecord sdr) {
        return repository
                .delete(sdr.schemaHash())
                .onFailure(ex -> spec.commandLine().getErr().println("ERROR: Failed to delete SDR"))
                .flatMap(deleted -> {
                    if (Boolean.FALSE.equals(deleted)) {
                        return Try.failure(new IllegalArgumentException("SDR not found during deletion"));
                    }
                    return Try.success((Void) null);
                })
                .onFailure(ex -> spec.commandLine().getErr().println("ERROR: " + ex.getMessage()));
    }

    public static <T extends Record>  void printTable(CommandSpec spec, Collection<T> items, Class<T> clazz, String format) {
        var recordsComponent = clazz.getRecordComponents();
        StringBuilder stringBuilder = new StringBuilder();
        String heading = String.format(format, Arrays.stream(recordsComponent).map(f -> f.getName().toUpperCase()).toArray());
        stringBuilder.append(heading).append("\n");
        stringBuilder.append("-".repeat(heading.length())).append("\n");
        for (T item : items) {
            Object[] values = Arrays.stream(recordsComponent)
                    .map(RecordComponent::getAccessor)
                    .map(accessor -> fieldValueToString(accessor, item))
                    .toArray();
            stringBuilder.append(String.format(format, values)).append("\n");
        }
        stringBuilder.append("\nTotal: ").append(items.size()).append(" item(s)");
        spec.commandLine().getOut().println(stringBuilder.toString());
    }

    private static String fieldValueToString(Method accessor, Object record) {
        try {
            Object value = accessor.invoke(record);
            return value != null ? value.toString() : "null";
        } catch (Exception e) {
            return "ERROR";
        }
    }
    public static void printYaml(CommandSpec spec, Yaml mapper, Object obj) {
        try {
            String yamlOutput = mapper.dump(obj);
            spec.commandLine().getOut().println(yamlOutput);
        } catch (Exception e) {
            spec.commandLine().getErr().println("ERROR: Failed to produce YAML output: " + e.getMessage());
        }
    }

    public static void printJson(CommandSpec spec, ObjectMapper mapper, Object obj) {
        try {
            String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            spec.commandLine().getOut().println(jsonOutput);
        } catch (JsonProcessingException e) {
            spec.commandLine().getErr().println("ERROR: Failed to produce JSON output: " + e.getMessage());
        }
    }
}
