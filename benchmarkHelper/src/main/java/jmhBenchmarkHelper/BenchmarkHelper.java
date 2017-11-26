package jmhBenchmarkHelper;

import de.rwth.i2.attestor.main.Attestor;

public class BenchmarkHelper {

    private static boolean CHECK_EXPECTED_SIZE = System.getProperty("attestor.checkExpectedSize").equalsIgnoreCase("true");

    public static void run() {
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        Attestor attestor = new Attestor();
        attestor.run(new String[]{
                "-sf",
                "configuration/settings/" + methodName + ".json",
                "-rp",
                "."
        });
    }


    public static void run(long expectedTotalStates, long expectedProcedureStates, long expectedFinalStates) {

        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        Attestor attestor = new Attestor();
        attestor.run(new String[]{
                "-sf",
                "configuration/settings/" + methodName + ".json",
                "-rp",
                "."
        });

        failOnMismatch(
                attestor.getTotalNumberOfStates(),
                expectedTotalStates,
                "Total number of states does not match."
                );

        failOnMismatch(
                attestor.getNumberOfStatesWithoutProcedureCalls(),
                expectedProcedureStates,
                "Number of states without procedure calls does not match."
        );

        failOnMismatch(
                attestor.getNumberOfFinalStates(),
                expectedFinalStates,
                "Number of final states does not match."
        );
    }

    private static void failOnMismatch(long actual, long expected, String message) {

        if(CHECK_EXPECTED_SIZE && actual != expected) {
            throw new IllegalStateException(message + "\nexpected: " + expected + "\ngot: " + actual);
        }
    }
}
