package io.statemodeler.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import picocli.CommandLine;

/**
 * Utility helpers to capture picocli command output in tests.
 *
 * <p>Captures both CommandLine.print/err via CommandLine.setOut/setErr and System.err
 * (for SLF4J logging) to allow assertions on both kinds of outputs. It also tries to
 * invoke setOutput(PrintWriter) / setErr(PrintWriter) methods on the command object if
 * present (some commands provide such setters for custom output).
 *
 * <p>Usage (try-with-resources supported):
 * <pre>
 *   var cmd = new CommandLine(new ShowCommand());
 *   try (var capture = PicocliTestHelper.capture(cmd)) {
 *     int exitCode = cmd.execute("my-model", "--format=all");
 *     assertEquals(0, exitCode);
 *     assertTrue(capture.getOut().contains("=== SDR Metadata ==="));
 *   }
 * </pre>
 */
public final class PicocliTestHelper {

    private PicocliTestHelper() {}

    public static OutputCapture capture(CommandLine cmd) {
        StringWriter outWriter = new StringWriter();
        PrintWriter outPw = new PrintWriter(outWriter, true);
        cmd.setOut(outPw);

        StringWriter errWriter = new StringWriter();
        PrintWriter errPw = new PrintWriter(errWriter, true);
        cmd.setErr(errPw);

        // If command exposes setOutput(PrintWriter) / setErr(PrintWriter), set them
        Object command = cmd.getCommand();
        try {
            Method setOutMethod = command.getClass().getMethod("setOutput", PrintWriter.class);
            setOutMethod.invoke(command, outPw);
        } catch (NoSuchMethodException e) {
            // ignore
        } catch (Exception e) {
            // ignore
        }
        try {
            Method setErrMethod = command.getClass().getMethod("setErr", PrintWriter.class);
            setErrMethod.invoke(command, errPw);
        } catch (NoSuchMethodException e) {
            // ignore
        } catch (Exception e) {
            // ignore
        }

        // Capture System.out and System.err (command output and logger output). Save originals and replace.
        var originalOut = System.out;
        var outBaos = new ByteArrayOutputStream();
        var outPs = new PrintStream(outBaos, true);
        System.setOut(outPs);

        var originalErr = System.err;
        var errBaos = new ByteArrayOutputStream();
        var errPs = new PrintStream(errBaos, true);
        System.setErr(errPs);

        return new OutputCapture(outWriter, errWriter, outBaos, errBaos, originalOut, originalErr);
    }

    public static final class OutputCapture implements AutoCloseable {
        private final StringWriter outWriter;
        private final StringWriter errWriter;
        private final ByteArrayOutputStream outBaos;
        private final ByteArrayOutputStream errBaos;
        private final PrintStream originalOut;
        private final PrintStream originalErr;

        OutputCapture(
                StringWriter outWriter,
                StringWriter errWriter,
                ByteArrayOutputStream outBaos,
                ByteArrayOutputStream errBaos,
                PrintStream originalOut,
                PrintStream originalErr) {
            this.outWriter = outWriter;
            this.errWriter = errWriter;
            this.outBaos = outBaos;
            this.errBaos = errBaos;
            this.originalOut = originalOut;
            this.originalErr = originalErr;
        }

        public String getOut() {
            // prefer the cmd.out text if present; otherwise fallback to system out bytes
            var outText = outWriter.toString();
            if (outText != null && !outText.isBlank()) {
                return outText;
            }
            return outBaos.toString();
        }

        public String getErr() {
            // prefer the cmd.err text if present; otherwise fallback to system err bytes
            var errText = errWriter.toString();
            if (errText != null && !errText.isBlank()) {
                return errText;
            }
            return errBaos.toString();
        }

        public void restore() {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        @Override
        public void close() {
            restore();
        }
    }
}
