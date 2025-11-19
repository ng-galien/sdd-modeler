package io.statemodeler.cli;

import java.io.PrintWriter;
import java.io.StringWriter;
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

    public record ExecutionResult(int exitCode, String out, String err) {}

    public static void runWithCapture(CommandLine cmd, Consumer<ExecutionResult> assertions, String... args) {
        var out = new StringWriter();
        var err = new StringWriter();
        cmd.setOut(new PrintWriter(out, true));
        cmd.setErr(new PrintWriter(err, true));
        int exitCode = cmd.execute(args);
        var result = new ExecutionResult(exitCode, out.toString(), err.toString());
        assertions.accept(result);
    }

    public static void runWithCapture(CommandLine cmd, Consumer<ExecutionResult> assertions) {
        runWithCapture(cmd, assertions, new String[0]);
    }
}
