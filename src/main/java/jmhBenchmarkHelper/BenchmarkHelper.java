package jmhBenchmarkHelper;

import de.rwth.i2.attestor.main.Attestor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class BenchmarkHelper {

    private static boolean CHECK_EXPECTED_SIZE = System
            .getProperty("attestor.checkExpectedSize")
            .equalsIgnoreCase("true");

    private final static Path file = Paths.get("benchmark-results.csv");

    private boolean checkLTLResults;
    private boolean expectedLTLResults;

    private boolean checkTotalStates;
    private long expectedTotalStates;

    private boolean checkMainProcedureStates;
    private long expectedMainProcedureStates;

    private boolean checkFinalStates;
    private long expectedFinalStates;

    private boolean checkCounterexampleNumber;
    private int expectedCounterexampleNumber;

    private boolean expectFatal =false;

    private boolean autosetRootPath = true;
    private String rootPath;

    private StringBuilder errorBuilder = new StringBuilder();

    BenchmarkHelper() {
    }

    public static BenchmarkHelperBuilder builder() {
        return new BenchmarkHelperBuilder();
    }

    public static class BenchmarkHelperBuilder {

        private BenchmarkHelper benchmarkHelper = new BenchmarkHelper();

        public BenchmarkHelper build() {
            BenchmarkHelper result = benchmarkHelper;
            benchmarkHelper = null;
            return result;
        }

        public BenchmarkHelperBuilder expectLTLResults(boolean allSatisfied) {
            benchmarkHelper.checkLTLResults = true;
            benchmarkHelper.expectedLTLResults = allSatisfied;
            return this;
        }

        public BenchmarkHelperBuilder expectTotalStates(long totalStates) {
            benchmarkHelper.checkTotalStates = true;
            benchmarkHelper.expectedTotalStates = totalStates;
            return this;
        }

        public BenchmarkHelperBuilder expectMainProcedureStates(long mainProcedureStates) {
            benchmarkHelper.checkMainProcedureStates = true;
            benchmarkHelper.expectedMainProcedureStates = mainProcedureStates;
            return this;
        }

        public BenchmarkHelperBuilder expectFinalStates(long finalStates) {
            benchmarkHelper.checkFinalStates = true;
            benchmarkHelper.expectedFinalStates = finalStates;
            return this;
        }

        public BenchmarkHelperBuilder expectNoCounterexamples(int noCounterexamples) {
            benchmarkHelper.checkCounterexampleNumber = true;
            benchmarkHelper.expectedCounterexampleNumber = noCounterexamples;
            return this;
        }

        public BenchmarkHelperBuilder setRootPath(String rootPath) {
            benchmarkHelper.autosetRootPath = false;
            benchmarkHelper.rootPath = rootPath;
            return this;
        }

        public BenchmarkHelperBuilder expectFatal() {
            benchmarkHelper.expectFatal = true;
            return this;
        }
    }

    public void run() {

        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
        String methodName = stackTraceElement.getMethodName();

        if(autosetRootPath) {
            String className = stackTraceElement.getClassName();
            className = className.substring(className.lastIndexOf(".")+1);
            rootPath = "./" + className;
        }

        Attestor attestor = new Attestor();
        attestor.run(new String[]{
                "-rp",
                rootPath,
                "-l",
                "configuration/settings/" + methodName + ".attestor"
        });

        if(!CHECK_EXPECTED_SIZE) {
            return;
        }

        if(checkTotalStates) {
            failOnMismatch(
                    attestor.getTotalNumberOfStates(),
                    expectedTotalStates,
                    "Total number of states does not match."
            );
        }

        if(checkMainProcedureStates) {
            failOnMismatch(
                    attestor.getNumberOfStatesWithoutProcedureCalls(),
                    expectedMainProcedureStates,
                    "Number of states without procedure calls does not match."
            );
        }

        if(checkFinalStates) {
            failOnMismatch(
                    attestor.getNumberOfFinalStates(),
                    expectedFinalStates,
                    "Number of final states does not match."
            );
        }

        if(checkLTLResults) {
            failOnMismatch(
                    attestor.hasAllLTLSatisfied(),
                    expectedLTLResults,
                    "Model-checking results do not match (SAT=true, UNSAT=false)."
            );
        }

        if(checkCounterexampleNumber) {
            failOnMismatch(
                    attestor.getCounterexamples().size(),
                    expectedCounterexampleNumber,
                    "Number of generated counterexamples does not match."
            );
        }

        if(expectFatal) {
            failOnMismatch(
                    attestor.hasFatalError(),
                    expectFatal,
                    "Expected fatal error, e.g. a detected null pointer dereference."
            );
        }

        String errorMessage = errorBuilder.toString();
        if(!errorMessage.isEmpty()) {
            throw new IllegalStateException("\n" + errorMessage);
        }

        logResults(attestor);


    }

    private void failOnMismatch(long actual, long expected, String message) {
        if(actual != expected) {
            storeFailure(actual, expected, message);
        }
    }

    private void failOnMismatch(int actual, int expected, String message) {
        if(actual != expected) {
            storeFailure(actual, expected, message);
        }
    }

    private void failOnMismatch(boolean actual, boolean expected, String message) {
        if(actual != expected) {
            storeFailure(actual, expected, message);
        }
    }

    private void logResults(Attestor attestor) {

        try {
            StringBuilder lineBuilder = new StringBuilder();

            lineBuilder.append(attestor.getDescription());

            Map<String, Double> executionTimes = attestor.getExecutionTimes();

            lineBuilder.append(String.format(Locale.ROOT, "%.3f", executionTimes.get("Interprocedural Analysis")))
                    .append(",")
                    .append(String.format(Locale.ROOT, "%.3f", executionTimes.get("Model checking")))
                    .append(",")
                    .append(String.format(Locale.ROOT, "%.3f", executionTimes.get("Verification")))
                    .append(",")
                    .append(String.format(Locale.ROOT, "%.3f", executionTimes.get("Total")));

            Files.write(file, Collections.singletonList(lineBuilder.toString()),
                    Charset.forName("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private <T> void storeFailure(T actual, T expected, String message) {
        errorBuilder.append(message);
        errorBuilder.append("\n");
        errorBuilder.append("Expected: ");
        errorBuilder.append(expected);
        errorBuilder.append("\n");
        errorBuilder.append("Got: ");
        errorBuilder.append(actual);
        errorBuilder.append("\n------------------------------------------------\n");
    }
}
