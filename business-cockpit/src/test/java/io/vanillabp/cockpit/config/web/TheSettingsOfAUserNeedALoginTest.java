package io.vanillabp.cockpit.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Every security chain opens the path of the current user to everybody, because the browser asks
 * it before anybody logs in. The settings of the user lie below that path and need a login. So the
 * matcher a chain uses must not take them, and nobody may widen it to {@code current-user/**}.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheSettingsOfAUserNeedALoginTest {

  @Test
  void theMatcherOfTheCurrentUserTakesThatPathAlone() {

    assertThat(WebSecurityConfiguration.currentUserRequestMatcher
        .matches(new MockHttpServletRequest("GET", "/gui/api/v1/app/current-user")))
        .isTrue();

    for (final var method : new String[]{
        "GET", "PUT", "DELETE"
    }) {
      assertThat(WebSecurityConfiguration.currentUserRequestMatcher
          .matches(new MockHttpServletRequest(method, "/gui/api/v1/app/current-user/settings/language")))
          .as("%s of a setting is open to everybody", method)
          .isFalse();
    }

  }

}
