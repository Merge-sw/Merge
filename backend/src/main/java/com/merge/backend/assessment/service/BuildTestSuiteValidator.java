package com.merge.backend.assessment.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static validation of a student's submitted test suite before it reaches Judge0.
 * Catches trivially-passing suites and enforces per-stage minimums.
 */
@Component
public class BuildTestSuiteValidator {

    /**
     * Any assertion statement: assert, expect(), it(), test() or equivalent.
     * Used to count how many assertions are present vs how many are trivial.
     */
    private static final Pattern ASSERTION = Pattern.compile(
            "\\bassert\\b|\\bexpect\\s*\\(|\\bit\\s*\\(|\\btest\\s*\\(",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Trivially-passing assertion patterns: assert(true), assert.ok(true),
     * assert.strictEqual(true, true), expect(true).toBe(true), etc.
     * A suite whose EVERY assertion matches this is considered empty of meaning.
     */
    private static final Pattern TRIVIAL_ASSERTION = Pattern.compile(
            "\\bassert\\s*\\(\\s*true\\s*\\)"
                    + "|\\bassert\\.ok\\s*\\(\\s*true\\s*\\)"
                    + "|\\bassert\\.strictEqual\\s*\\(\\s*true\\s*,\\s*true\\s*\\)"
                    + "|\\bexpect\\s*\\(\\s*true\\s*\\)\\.to(?:Be|Equal)\\s*\\(\\s*true\\s*\\)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Signals that at least one edge-case scenario is tested.
     * Matches null/undefined/NaN, common boundary words, negative numbers, zero,
     * and empty collection / empty string literals passed as arguments.
     */
    private static final Pattern EDGE_CASE_SIGNAL = Pattern.compile(
            "\\bnull\\b|\\bundefined\\b|\\bNaN\\b|\\bInfinity\\b"
                    + "|\\bempty\\b|\\bedge\\b|\\bboundary\\b|\\binvalid\\b|\\bnegative\\b|\\bzero\\b"
                    + "|[,(]\\s*-\\d"          // negative number argument
                    + "|[,(]\\s*0[^.\\d]"      // zero argument (not a decimal)
                    + "|[,(]\\s*[\"'][\"']"     // empty string argument
                    + "|[,(]\\s*\\[\\s*\\]"    // empty array argument
                    + "|[,(]\\s*\\{\\s*\\}",   // empty object argument
            Pattern.CASE_INSENSITIVE
    );

    /** Cadet stage requires at least 2 assertions (happy path + edge case). */
    private static final int CADET_MINIMUM_ASSERTIONS = 2;

    public record ValidationResult(boolean valid, String errorMessage) {
        static ValidationResult pass() {
            return new ValidationResult(true, null);
        }

        static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Validates the test suite against static quality rules.
     * Call before executing via Judge0 to avoid wasting execution quota on trivial suites.
     *
     * @param testSuite the student's submitted test suite
     * @param stageName the student's current stage (e.g. "CADET"), used for per-stage minimums
     */
    public ValidationResult validate(String testSuite, String stageName) {
        if (testSuite == null || testSuite.isBlank()) {
            return ValidationResult.fail("Test suite must not be empty");
        }
        if (isTrivial(testSuite)) {
            return ValidationResult.fail(
                    "Test suite must contain meaningful assertions — assert(true) does not count");
        }
        if ("CADET".equalsIgnoreCase(stageName)) {
            return validateCadetMinimum(testSuite);
        }
        return ValidationResult.pass();
    }

    /**
     * Returns true when every assertion in the suite is a trivially-passing no-op.
     * A suite with zero assertions is also trivial.
     */
    private boolean isTrivial(String testSuite) {
        int total = countMatches(ASSERTION, testSuite);
        if (total == 0) return true;
        int trivial = countMatches(TRIVIAL_ASSERTION, testSuite);
        return total == trivial;
    }

    /**
     * Cadet minimum: at least 2 assertions and at least one edge-case signal.
     */
    private ValidationResult validateCadetMinimum(String testSuite) {
        int assertionCount = countMatches(ASSERTION, testSuite);
        if (assertionCount < CADET_MINIMUM_ASSERTIONS) {
            return ValidationResult.fail(
                    "Cadet submissions require at least a happy-path test and one edge-case test");
        }
        if (!EDGE_CASE_SIGNAL.matcher(testSuite).find()) {
            return ValidationResult.fail(
                    "Cadet submissions require at least one edge case "
                            + "(e.g. null, undefined, zero, negative number, or empty input)");
        }
        return ValidationResult.pass();
    }

    private int countMatches(Pattern pattern, String input) {
        Matcher m = pattern.matcher(input);
        int count = 0;
        while (m.find()) count++;
        return count;
    }
}
