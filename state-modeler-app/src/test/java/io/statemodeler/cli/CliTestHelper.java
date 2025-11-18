package io.statemodeler.cli;

import java.util.function.Consumer;
import picocli.CommandLine;

/**
 * Small convenience helpers to make assertions on picocli CommandLine executions easier in tests.
 *
 * <p>Provides a compact API to execute a command and assert on its exit code and outputs with a
 * single try-with-resources call under the hood.
 */
public final class CliTestHelper {

    private CliTestHelper() {}

    public static record ExecutionResult(int exitCode, String out, String err) {}

    public static void runWithCapture(CommandLine cmd, Consumer<ExecutionResult> assertions, String... args) {
        try (var capture = PicocliTestHelper.capture(cmd)) {
            int exitCode = cmd.execute(args);
            var result = new ExecutionResult(exitCode, capture.getOut(), capture.getErr());
            assertions.accept(result);
        }
    }

    public static void runWithCapture(CommandLine cmd, Consumer<ExecutionResult> assertions) {
        runWithCapture(cmd, assertions, new String[0]);
    }
}
