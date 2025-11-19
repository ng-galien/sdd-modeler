package io.statemodeler.cli;

import io.statemodeler.repository.SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import picocli.CommandLine.Model.CommandSpec;

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
}
