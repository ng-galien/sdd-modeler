package io.statemodeler.cli.commands;

import io.statemodeler.cli.RepositoryMixin;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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

    @Parameters(index = "0", description = "SDR hash to delete")
    String hash;

    @Option(
            names = {"--yes", "-y"},
            description = "Skip confirmation prompt")
    boolean skipConfirmation;

    @Mixin
    RepositoryMixin repositoryMixin;

    @Override
    public Integer call() {
        if (hash == null || hash.isBlank()) {
            System.err.println("ERROR: Hash cannot be empty");
            return 1;
        }

        try (var repository = repositoryMixin.createRepository()) {
            // First, check if SDR exists and get metadata for confirmation
            var findResult = repository.findByHash(hash);

            if (findResult.isFailure()) {
                System.err.println("ERROR: Failed to retrieve SDR");
                System.err.println("  " + findResult.getCause().getMessage());
                return 1;
            }

            var sdrOpt = findResult.get();
            if (sdrOpt.isEmpty()) {
                System.err.println("ERROR: SDR not found");
                System.err.println("  Hash: " + hash);
                System.err.println("  Use 'sdd-modeler list' to view registered SDRs");
                return 1;
            }

            var sdr = sdrOpt.get();

            // Get metadata for display
            var metadataResult = repository.findByHash(hash);
            if (metadataResult.isFailure() || metadataResult.get().isEmpty()) {
                // Shouldn't happen since we just found it, but handle anyway
                System.err.println("ERROR: Failed to retrieve SDR metadata");
                return 1;
            }

            // Display what will be deleted
            System.out.println("About to delete SDR:");
            System.out.println("  Hash: " + sdr.schemaHash());
            System.out.println("  Version: " + sdr.version());
            System.out.println("  Build Fingerprint: " + sdr.buildFingerprint());

            // Prompt for confirmation unless --yes flag is set
            if (!skipConfirmation) {
                if (!confirmDeletion()) {
                    System.out.println("\nDeletion cancelled");
                    return 0;
                }
            }

            // Perform deletion
            var deleteResult = repository.delete(hash);

            if (deleteResult.isFailure()) {
                System.err.println("ERROR: Failed to delete SDR");
                System.err.println("  " + deleteResult.getCause().getMessage());
                return 1;
            }

            boolean deleted = deleteResult.get();
            if (!deleted) {
                System.err.println("ERROR: SDR not found during deletion");
                System.err.println("  Hash: " + hash);
                return 1;
            }

            System.out.println("\n✓ Successfully deleted SDR");
            System.out.println("  Hash: " + hash);

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: Repository error");
            System.err.println("  " + e.getMessage());
            return 1;
        }
    }

    private boolean confirmDeletion() {
        System.out.print("\nAre you sure you want to delete this SDR? (yes/no): ");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String response = reader.readLine();
            return response != null
                    && (response.trim().equalsIgnoreCase("yes")
                            || response.trim().equalsIgnoreCase("y"));
        } catch (Exception e) {
            System.err.println("\nERROR: Failed to read confirmation");
            return false;
        }
    }
}
