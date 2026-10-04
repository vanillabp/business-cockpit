package io.vanillabp.cockpit.bpms;

import io.vanillabp.integration.test.utils.CapturedOutput;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The lines a test logged as an error. A failure which is not stored is logged once per attempt,
 * and a test counts the lines to show that.
 */
public final class LoggedErrors {

    /** The pattern of the test logging: time, thread in brackets, level, logger. */
    private static final Pattern ERROR_LINE = Pattern.compile("^\\S+ \\[[^\\]]+\\] ERROR .*");

    private LoggedErrors() {
    }

    /**
     * @param output What the test wrote
     * @return Each line which starts an error, without the stack traces below it
     */
    public static List<String> of(
            final CapturedOutput output) {

        return output
                .getAllOfThisTest()
                .lines()
                .filter(line -> ERROR_LINE.matcher(line).matches())
                .toList();

    }

}
