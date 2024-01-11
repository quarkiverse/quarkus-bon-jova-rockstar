package io.quarkiverse.pact.it;

import org.apache.maven.cli.MavenCli;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NormalModeRockstarExtensionIT {

    @Test
    public void testMavenBuild() {
        final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
        final ByteArrayOutputStream testErr = new ByteArrayOutputStream();

        // Just launch the build via maven, using the maven embedder
        MavenCli cli = new MavenCli();
        String projectDir = "./target/test-classes/projects/simple-app-processed";
        System.setProperty("maven.multiModuleProjectDirectory", new File(projectDir).getAbsolutePath());
        int status = cli.doMain(
                new String[]{"clean", "verify"},
                projectDir,
                new PrintStream(testOut),
                new PrintStream(testErr));

        // The output and error streams seem to include application output, but not the output from maven itself, so we don't get the
        // test count
        // We could dig into the surefire reports, but just check the status code; in the failure case, at least we get a failure that
        // can be investigated by hand
        assertEquals(0, status, testOut.toString());

        // There should not be any error messages
        assertEquals("", testErr.toString());
    }


}
