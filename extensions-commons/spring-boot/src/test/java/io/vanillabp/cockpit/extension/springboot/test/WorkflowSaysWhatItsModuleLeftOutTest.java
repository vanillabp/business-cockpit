package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.CockpitSettings;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module which says neither which language its BPMN names are in nor which languages
 * it reports in starts, because its one workflow says both.
 * <p>
 * That is what version 1 did, and it asked at the first event of a workflow. This asks while the
 * application starts, once VanillaBP has said which processes the module holds, so the boot of
 * this application is half of the assertion. The Quarkus twin asserts the same tree.
 */
@SpringBootTest(classes = TestApplication.class,
    properties = {
        "spring.config.additional-location=classpath:/left-to-the-workflows/"
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class WorkflowSaysWhatItsModuleLeftOutTest {

  @Autowired
  private CockpitSettings settings;

  @Autowired
  private MigrationAdapterProperties properties;

  @Test
  @DisplayName("What the workflow says is what its module reports with")
  public void theWorkflowSaysItForItsModule() {

    final var module = BusinessCockpitConfiguration
        .readAndValidate(properties, settings, false)
        .workflowModule("test-module");

    assertEquals(List.of("fr"), module.i18nLanguages("TestProcess"));
    assertEquals("fr", module.bpmnDescriptionLanguage("TestProcess"));

  }

}
