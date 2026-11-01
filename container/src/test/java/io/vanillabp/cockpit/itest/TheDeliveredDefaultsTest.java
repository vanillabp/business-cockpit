package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Two values the library has none for, and which the delivered application sets in its own
 * {@code application.yaml}. A custom cockpit writes them as well, see the wiki page "Building a
 * custom Business Cockpit".
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheDeliveredDefaultsTest extends ItestBase {

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private Environment environment;

  @Test
  void aUserWithoutALanguageGetsEnglish() {

    assertThat(properties.getDefaultLocale()).isEqualTo(Locale.ENGLISH);

  }

  @Test
  void aDeepLinkIsAnsweredWithTheShellOnTheClassPath() {

    assertThat(environment.getProperty("application.spa-default-file"))
        .isEqualTo("classpath:/static/index.html");

  }

}
