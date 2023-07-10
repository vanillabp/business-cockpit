package io.vanillabp.cockpit.simulator.common;

import java.util.Locale;

import com.devskiller.jfairy.Fairy;

public class FairyHelper {
    public static Fairy buildFairy(
            final String language) {

        return Fairy.builder()
                    .withLocale(Locale.forLanguageTag(language))
                    .withRandomSeed((int) System.currentTimeMillis())
                    .build();

    }
}