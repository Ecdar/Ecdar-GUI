package ecdar.utility;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner {
    public static void main(String[] args) {
        // ------------ Test 1: symbol to unicode ------------------
        Result result1 = JUnitCore.runClasses(StringHelperTestSymbolToUnicode.class);
        for (Failure failure : result1.getFailures()) {
            System.out.println(failure.toString());
        }
        System.out.println("Result of test StringHelperTestSymbolToUnicode: " + result1.wasSuccessful());

        // ------------ Test 2: unicode to symbol ------------------
        Result result2 = JUnitCore.runClasses(StringHelperTestUnicodeToSymbol.class);
        for (Failure failure : result2.getFailures()) {
            System.out.println(failure.toString());
        }
        System.out.println("Result of test StringHelperTestUnicodeToSymbol: " + result2.wasSuccessful());
    }
}
