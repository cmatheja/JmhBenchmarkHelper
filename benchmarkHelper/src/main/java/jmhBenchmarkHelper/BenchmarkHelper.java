package jmhBenchmarkHelper;

import de.rwth.i2.attestor.main.Attestor;

public class BenchmarkHelper {

    private static boolean CHECK_EXPECTED_SIZE = System
            .getProperty("attestor.checkExpectedSize")
            .equalsIgnoreCase("true");

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
    }

    public void run() {

        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        Attestor attestor = new Attestor();
        attestor.run(new String[]{
                "-sf",
                "configuration/settings/" + methodName + ".json",
                "-rp",
                "."
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

        String errorMessage = errorBuilder.toString();
        if(!errorMessage.isEmpty()) {
            throw new IllegalStateException("\n" + errorMessage);
        }
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
