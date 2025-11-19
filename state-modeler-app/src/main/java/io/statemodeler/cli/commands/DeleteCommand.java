package io.statemodeler.cli.commands;

import io.statemodeler.cli.CliCommandHelpers;
import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.repository.SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * CLI command to delete a registered SDR from the repository.
 *
 * <p>Usage: sdd-modeler delete <hash> [--yes]
 *
 * <p>By default, prompts for confirmation before deletion. Use --yes to skip confirmation.
 *
 * <p>Examples:
 * <ul>
 *   <li>sdd-modeler delete 222fa0d3... (interactive confirmation)
 *   <li>sdd-modeler delete 222fa0d3... --yes (skip confirmation)
 * </ul>
 */
@Command(name = "delete", description = "Delete a registered SDR", mixinStandardHelpOptions = true)
public class DeleteCommand implements Callable<Integer> {
    // Logging moved to picocli's output (spec.commandLine())

    @Parameters(index = "0", description = "SDR hash to delete")
    String hash;

    @Option(
            names = {"--yes", "-y"},
            description = "Skip confirmation prompt")
    boolean skipConfirmation;

    @Mixin
    RepositoryMixin repositoryMixin;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        if (hash == null || hash.isBlank()) {
            spec.commandLine().getErr().println("ERROR: Hash cannot be empty");
            return 1;
        }

        try (var repository = repositoryMixin.createRepository()) {
            return findAndDeleteRegardingConfirmation(
                            spec, repository, hash, confirmed -> skipConfirmation || confirmDeletion(spec, confirmed))
                    .map(unused -> 0)
                    .getOrElseGet(ex -> 1);
        }
    }

    private static boolean confirmDeletion(CommandSpec spec, SdrRecord sdr) {
        spec.commandLine().getOut().println("About to delete SDR:");
        spec.commandLine().getOut().println("  Hash: " + sdr.schemaHash());
        spec.commandLine().getOut().println("  Version: " + sdr.version());
        spec.commandLine().getOut().println("  Build Fingerprint: " + sdr.buildFingerprint());
        spec.commandLine().getOut().print("\nAre you sure you want to delete this SDR? (yes/no): ");
        return Try.withResources(() -> new BufferedReader(new InputStreamReader(System.in)))
                .of(reader -> {
                    String response = reader.readLine();
                    return response != null
                            && (response.trim().equalsIgnoreCase("yes")
                                    || response.trim().equalsIgnoreCase("y"));
                })
                .getOrElseGet(ex -> {
                    spec.commandLine().getErr().println("\nERROR: Failed to read confirmation");
                    return false;
                });
    }

    private static Try<Void> findAndDeleteRegardingConfirmation(
            CommandSpec spec, SdrRepository repository, String hash, Predicate<SdrRecord> confirmationPredicate) {
        return CliCommandHelpers.findByHash(spec, repository, hash).flatMap(sdr -> {
            if (!confirmationPredicate.test(sdr)) {
                spec.commandLine().getOut().println("\nDeletion cancelled");
                return Try.success(null);
            } else {
                return CliCommandHelpers.deleteSdr(spec, repository, sdr).onSuccess(unused -> {
                    spec.commandLine().getOut().println("\n✓ Successfully deleted SDR");
                    spec.commandLine().getOut().println("  Hash: " + hash);
                });
            }
        });
    }
}
