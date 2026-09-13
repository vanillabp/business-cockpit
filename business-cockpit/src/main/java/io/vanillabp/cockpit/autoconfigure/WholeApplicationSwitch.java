package io.vanillabp.cockpit.autoconfigure;

import org.slf4j.Logger;

/**
 * A Spring feature the Business Cockpit switches on for the whole application, and the line which
 * says so on every start.
 * <p>
 * The cockpit used to get these features from a base class an application wrote down itself, so a
 * developer could see in their own source that they had asked for them. They now arrive with the
 * dependency, which is why every one of them reports itself: what it does to the application, and
 * what to write to decide it instead of the cockpit.
 *
 * @param annotation the annotation as it would be written in source
 * @param whatItDoes what changes for the application, in one sentence
 * @param howToDecideItYourself what to write to take the decision over
 */
record WholeApplicationSwitch(
        String annotation,
        String whatItDoes,
        String howToDecideItYourself) {

    void reportTo(
            final Logger logger) {

        logger.info(
                "The Business Cockpit switched {} on for the whole application, not only for itself. {} "
                        + "To decide it yourself: {}",
                annotation,
                whatItDoes,
                howToDecideItYourself);

    }

}
