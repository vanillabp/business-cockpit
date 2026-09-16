package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.springboot.bridges.BridgesOfASecondBpms;
import io.vanillabp.cockpit.extension.springboot.broken.BrokenApplication;
import io.vanillabp.cockpit.extension.springboot.brokenparts.ProxiedVersionedProviderService;
import io.vanillabp.cockpit.extension.springboot.brokenparts.TwiceServingProviderService;
import io.vanillabp.cockpit.extension.springboot.brokenparts.TwoMethodsForEveryUserTaskService;
import io.vanillabp.cockpit.extension.springboot.brokenparts.VersionedProviderService;
import io.vanillabp.cockpit.extension.springboot.writinghandler.ExtensionWithAWritingHandler;
import io.vanillabp.cockpit.extension.transport.BusinessCockpitTransport;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the application is told while it starts.
 * <p>
 * Most of these boots are meant to fail. A defect the extension can see at startup is refused
 * there and not when the first event arrives, and the message names the method to change. The
 * last boot is meant to succeed although the cockpit server is unreachable. A workflow module
 * which cannot start because a reporting server is down would be the worse failure of the two.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class BusinessCockpitBootTest {

  private static String failureOfBooting(
      final String databaseName,
      final Class<?>... sources) {

    return failureOfBooting(databaseName, new String[0], sources);

  }

  private static String failureOfBooting(
      final String databaseName,
      final String[] properties,
      final Class<?>... sources) {

    final var failure = assertThrows(
        Exception.class,
        () -> new SpringApplicationBuilder(sources)
            .web(WebApplicationType.NONE)
            .properties(
                "spring.datasource.url=jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1".formatted(databaseName),
                "vanillabp.cockpit.rest.base-url=http://localhost:1")
            .properties(properties)
            .run()
            .close());
    // every message of the chain. What an extension refuses is refused by VanillaBP's own scan
    // now, which names the annotation, the class and the method in front of what the extension
    // said. So the two halves of the answer stand in two exceptions
    final var messages = new StringBuilder();
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      messages.append(cause.getMessage()).append('\n');
    }
    return messages.toString();

  }

  @Test
  @DisplayName("A details provider naming a version is refused, naming its method")
  public void aVersionAttributeIsRefusedNamingTheMethod() {

    final var message = failureOfBooting(
        "cockpit-broken-version", BrokenApplication.class, VersionedProviderService.class);

    assertTrue(message.contains(VersionedProviderService.class.getName()), message);
    assertTrue(message.contains("approve"), message);
    assertTrue(message.contains("version"), message);
    assertTrue(message.contains("taskDefinition"), message);

  }

  @Test
  @DisplayName("A details provider is found behind a JDK proxy, too")
  public void aProxiedServiceIsScannedAsWell() {

    final var message = failureOfBooting(
        "cockpit-broken-proxied",
        new String[]{
            // the everyday way into a JDK proxy is an interface plus @Transactional. What the
            // bean's TYPE is then says nothing about the class the application wrote
            "spring.aop.proxy-target-class=false"
        },
        BrokenApplication.class, ProxiedVersionedProviderService.class);

    assertTrue(message.contains(ProxiedVersionedProviderService.class.getName()), message);
    assertTrue(message.contains("approve"), message);
    assertTrue(message.contains("version"), message);

  }

  @Test
  @DisplayName("A details provider which is not public is named while booting")
  public void aDetailsProviderNobodySeesIsReported(
      final CapturedOutput output) {

    try (var ignored = new SpringApplicationBuilder(TestApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-nobody-sees;DB_CLOSE_DELAY=-1",
            "vanillabp.cockpit.rest.base-url=http://localhost:1")
        .run()) {

      final var reported = output.getAll();

      assertTrue(reported.contains("which VanillaBP does not see"), reported);
      assertTrue(reported.contains(TestWorkflowService.class.getName()), reported);
      assertTrue(reported.contains("@UserTaskDetailsProvider"), reported);
      assertTrue(reported.contains("unseenByTheScan"), reported);
      assertTrue(reported.contains("Make the method public"), reported);

    }

  }

  @Test
  @DisplayName("A boot with details providers says nothing about a second writer")
  public void theDetailsProvidersBringNoSecondWriter(
      final CapturedOutput output) {

    try (var ignored = new SpringApplicationBuilder(TestApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-reading-providers;DB_CLOSE_DELAY=-1",
            "vanillabp.cockpit.rest.base-url=http://localhost:1")
        .run()) {

      final var aboutTheCase = output
          .getAll()
          .lines()
          .filter(line -> line.contains(ConfigurationKeys.EXTENSION_ID))
          .filter(line -> line.contains(TestAggregate.class.getName()))
          .toList();

      assertTrue(
          aboutTheCase.isEmpty(),
          "the boot warned about the aggregate of a details provider: "
              + aboutTheCase);

    }

  }

  @Test
  @DisplayName("A handler which may write is still named while booting")
  public void aHandlerWhichMayWriteIsStillReported(
      final CapturedOutput output) {

    try (var ignored = new SpringApplicationBuilder(
        TestApplication.class, ExtensionWithAWritingHandler.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-second-writer;DB_CLOSE_DELAY=-1",
            "vanillabp.cockpit.rest.base-url=http://localhost:1")
        .run()) {

      final var reported = output
          .getAll()
          .lines()
          .filter(line -> line.contains(ExtensionWithAWritingHandler.EXTENSION_ID))
          .filter(line -> line.contains(TestAggregate.class.getName()))
          .findFirst()
          .orElseThrow(
              () -> new AssertionError(
                  "nothing was said about the second writer that extension brings: "
                      + output.getAll()));

      // the way out is what a developer needs from the line, and it is a version attribute on
      // the case. See decision 20 in the repository's DECISIONS.md for why this warning is
      // VanillaBP's and not one of ours

      assertTrue(reported.contains("version attribute"), reported);

    }

  }

  @Test
  @DisplayName("Two details providers claiming one user task are refused")
  public void twoProvidersOfOneUserTaskAreRefused() {

    final var message = failureOfBooting(
        "cockpit-broken-keys", BrokenApplication.class, TwiceServingProviderService.class);

    assertTrue(message.contains("approve"), message);
    assertTrue(message.contains(TwiceServingProviderService.class.getSimpleName()), message);

  }

  @Test
  @DisplayName("Two details providers claiming every user task are refused, naming both")
  public void twoProvidersOfEveryUserTaskAreRefused() {

    final var message = failureOfBooting(
        "cockpit-broken-every-task", BrokenApplication.class,
        TwoMethodsForEveryUserTaskService.class);

    assertTrue(message.contains("everyTaskByItsTaskDefinition"), message);
    assertTrue(message.contains("everyTaskByItsElementId"), message);
    assertTrue(message.contains(TwoMethodsForEveryUserTaskService.class.getSimpleName()), message);

  }

  @Test
  @DisplayName("A BPMS half registered as one list of bridges serves its adapter")
  public void bridgesRegisteredAsAListAreCollected() {

    try (var application = new SpringApplicationBuilder(
        TestApplication.class, BridgesOfASecondBpms.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-bridge-list;DB_CLOSE_DELAY=-1",
            "vanillabp.cockpit.rest.base-url=http://localhost:1")
        .run()) {

      final var extension = application.getBean(BusinessCockpitExtension.class);

      assertEquals(
          BridgesOfASecondBpms.ADAPTER_ID,
          extension.bridgeOf(BridgesOfASecondBpms.ADAPTER_ID).adapterId(),
          "the bridge of the list serves its adapter");
      assertEquals(
          RecordingBpmsBridge.ADAPTER_ID,
          extension.bridgeOf(RecordingBpmsBridge.ADAPTER_ID).adapterId(),
          "and the bridge registered as a bean of its own still does");

    }

  }

  @Test
  @DisplayName("Without a transport the boot names both keys and the way of bringing one")
  public void withoutATransportBothKeysAndTheOwnWayAreNamed() {

    final var message = failureOfBooting(
        "cockpit-without-a-transport",
        new String[]{
            // the properties of the builder are default properties, which the module's own
            // application.yaml outranks, so the key it sets is emptied by a file of its own
            "spring.config.additional-location=classpath:/without-a-transport/"
        },
        TestApplication.class);

    assertTrue(message.contains("vanillabp.cockpit.rest.base-url"), message);
    assertTrue(message.contains("vanillabp.cockpit.kafka.bootstrap-servers"), message);
    assertTrue(message.contains(BusinessCockpitTransport.class.getName()), message);

  }

  @Test
  @DisplayName("With both transports and no own one the boot still says to remove one")
  public void withBothTransportsOneIsToBeRemoved() {

    final var message = failureOfBooting(
        "cockpit-with-both-transports",
        new String[]{
            "vanillabp.cockpit.kafka.bootstrap-servers=broker:9092", "vanillabp.cockpit.kafka.topics.user-task=user-task", "vanillabp.cockpit.kafka.topics.workflow=workflow", "vanillabp.cockpit.kafka.topics.workflow-module=workflow-module"
        },
        TestApplication.class);

    assertTrue(message.contains("reported twice"), message);
    assertTrue(message.contains("vanillabp.cockpit.rest.base-url"), message);

  }

  @Test
  @DisplayName("A cockpit server which is down does not keep the application from starting")
  public void anUnreachableCockpitServerStillLetsTheApplicationStart() {

    try (var application = new SpringApplicationBuilder(TestApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-without-a-server;DB_CLOSE_DELAY=-1",
            "vanillabp.cockpit.rest.base-url=http://localhost:1")
        .run()) {

      assertNotNull(
          application.getBean(BusinessCockpitExtension.class));

    }

  }

}
