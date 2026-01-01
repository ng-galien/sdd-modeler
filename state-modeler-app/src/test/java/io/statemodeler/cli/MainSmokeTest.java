package io.statemodeler.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class MainSmokeTest {

    @Test
    void mainShouldExitZeroOnHelp() {
        int exit = new CommandLine(new Main()).execute("--help");
        org.junit.jupiter.api.Assertions.assertEquals(0, exit);
    }
}
