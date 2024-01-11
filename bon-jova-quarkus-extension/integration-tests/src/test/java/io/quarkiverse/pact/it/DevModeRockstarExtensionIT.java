package io.quarkiverse.pact.it;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.maven.it.RunAndCheckMojoTestBase;
import io.quarkus.maven.it.continuoustesting.ContinuousTestingMavenTestUtils;
import io.quarkus.test.devmode.util.DevModeClient;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@DisabledIfSystemProperty(named = "quarkus.test.native", matches = "true")
public class DevModeRockstarExtensionIT extends RunAndCheckMojoTestBase {

    protected void runAndCheck(boolean performCompile, String... options)
            throws MavenInvocationException, FileNotFoundException {
        run(performCompile, options);

        String resp = new DevModeClient().getHttpResponse();

        assertThat(resp).containsIgnoringCase("ready")
                        .containsIgnoringCase("application");

        // There's no json endpoints, so nothing else to check
    }

    @Disabled("The tests currently run twice on startup, which makes the continuous testing utils very upset, so it throws an exception " +
              "when we ask for test status")
    @Test
    public void testThatTheTestsPassed() throws MavenInvocationException, IOException {
        //we also check continuous testing
        String executionDir = "projects/simple-app-processed";
        testDir = initProject("projects/simple-app", executionDir);

        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils();
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();

        Assertions.assertEquals(1, results.getTestsPassed());
        Assertions.assertEquals(0, results.getTestsFailed());
    }

    @Test
    public void testThatTheTestsPassedOnSomeRun() throws MavenInvocationException, IOException {
        //we also check continuous testing
        String executionDir = "projects/simple-app-processed";
        testDir = initProject("projects/simple-app", executionDir);

        runAndCheck();

        ContinuousTestingMavenTestUtils.TestStatus results = waitForNextCompletion();

        Assertions.assertEquals(1, results.getTotalTestsPassed());
        Assertions.assertEquals(0, results.getTotalTestsFailed());
    }

    @Test
    public void testThatCodeChangeTriggersHotReloadAndTestRerun() throws MavenInvocationException, IOException {
        // Baseline check; wait for the tests to run
        String executionDir = "projects/simple-app-processed";
        testDir = initProject("projects/simple-app", executionDir);
        runAndCheck();
        ContinuousTestingMavenTestUtils.TestStatus results = waitForNextCompletion();
        Assertions.assertEquals(1, results.getTotalTestsPassed());
        Assertions.assertEquals(0, results.getTotalTestsFailed());

        // Now change the contents of the rock file
        File rockFile = new File(testDir, "src/main/rockstar/simple_hello_world.rock");
        String str = "Tommy is a dancer";
        BufferedWriter writer = new BufferedWriter(new FileWriter(rockFile));
        writer.write(str);
        writer.close();

        results = waitForNextCompletion();

        // Note we're now checking the tests on this run, not the total over all runs (tests may be run incrementally with only some
        // tests run for some changes)
        // The test should fail since we changed the code
        Assertions.assertEquals(1, results.getTestsFailed());
        Assertions.assertEquals(0, results.getTestsPassed());

    }


    /* Home rolled copy of the method in the test utils, to allow us to skip the check for the last run number */
    public ContinuousTestingMavenTestUtils.TestStatus waitForNextCompletion() {
        try {
            Awaitility.waitAtMost(2, TimeUnit.MINUTES)
                      .pollInterval(200, TimeUnit.MILLISECONDS)
                      .until(() -> {
                          ContinuousTestingMavenTestUtils.TestStatus ts = getTestStatus();
                          // the real implementation is much more sophisticated about waiting sure we wait for the next run, and
                          //  subtle race conditions
                          // Check if it's running, and also check if it's never run (ie we got in before the first run)
                          if (ts.getRunning() > 0 || (ts.getTotalTestsPassed() < 0 && ts.getTotalTestsFailed() < 0 && ts.getTotalTestsSkipped() < 0)) {
                              return false;
                          } else {
                              return true;
                          }
                      });
            return getTestStatus();
        } catch (Exception e) {
            ContinuousTestingMavenTestUtils.TestStatus ts;
            try {
                ts = getTestStatus();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        throw new RuntimeException("Could not get a test status within the timeout");
    }

    /* Home rolled copy of the method in the test utils, to allow us to skip the check for the last run number */
    private ContinuousTestingMavenTestUtils.TestStatus getTestStatus() {
        DevUIJsonRPCTest devUIJsonRPCTest = new DevUIJsonRPCTest("devui-continuous-testing", "http://localhost:8080");
        try {

            TypeReference<Map<String, Long>> typeRef = new TypeReference<Map<String, Long>>() {
            };
            Map<String, Long> testStatus = devUIJsonRPCTest.executeJsonRPCMethod(typeRef, "getStatus");

            long lastRun = testStatus.getOrDefault("lastRun", -1L);
            long running = testStatus.getOrDefault("running", -1L);
            long testsRun = testStatus.getOrDefault("testsRun", -1L);
            long testsPassed = testStatus.getOrDefault("testsPassed", -1L);
            long testsFailed = testStatus.getOrDefault("testsFailed", -1L);
            long testsSkipped = testStatus.getOrDefault("testsSkipped", -1L);
            long totalTestsPassed = testStatus.getOrDefault("totalTestsPassed", -1L);
            long totalTestsFailed = testStatus.getOrDefault("totalTestsFailed", -1L);
            long totalTestsSkipped = testStatus.getOrDefault("totalTestsSkipped", -1L);

            return new ContinuousTestingMavenTestUtils.TestStatus(lastRun, running, testsRun, testsPassed, testsFailed, testsSkipped,
                    totalTestsPassed,
                    totalTestsFailed, totalTestsSkipped);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
