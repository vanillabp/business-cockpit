package io.vanillabp.cockpit.extension.springboot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.ConfigurationKeyCheck;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Spring Boot binds a key written in camel case or with underscores as well as the dashed one. So
 * the check of the keys must not refuse a key in such a spelling, which the application has
 * always been allowed to write.
 */
@ExtendWith(SuppressOutputExtension.class)
class EverySpellingOfAKeyIsCheckedTest {

  @Test
  @DisplayName("A key in camel case or with underscores is checked in its dashed form")
  void aKeyIsCheckedInItsDashedForm() {

    assertEquals(
        "vanillabp.cockpit.rest.base-url",
        KeysOfTheConfigurationFiles.inTheDashedForm("vanillabp.cockpit.rest.baseUrl"));
    assertEquals(
        "vanillabp.workflow-modules.loanApproval.cockpit.ui-uri-path",
        KeysOfTheConfigurationFiles
            .inTheDashedForm("vanillabp.workflow-modules.loanApproval.cockpit.ui_uri_path"));
    assertDoesNotThrow(() -> ConfigurationKeyCheck
        .refuseKeysNobodyReads(List
            .of(
                KeysOfTheConfigurationFiles.inTheDashedForm("vanillabp.cockpit.rest.baseUrl"),
                KeysOfTheConfigurationFiles
                    .inTheDashedForm("vanillabp.workflow-modules.loanApproval.cockpit.uiUriPath"))));

  }

  @Test
  @DisplayName("A key outside the cockpit's sections stays as it is")
  void aKeyOutsideTheCockpitStaysAsItIs() {

    assertEquals(
        "vanillabp.adapters.camunda8.taskTimeout",
        KeysOfTheConfigurationFiles.inTheDashedForm("vanillabp.adapters.camunda8.taskTimeout"));

  }

}
