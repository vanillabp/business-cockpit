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
import io.vanillabp.cockpit.extension.springboot.bridges.BridgesOfASecondBpms;
import io.vanillabp.cockpit.extension.springboot.broken.BrokenApplication;
import io.vanillabp.cockpit.extension.springboot.brokenparts.ProxiedVersionedProviderService;
import io.vanillabp.cockpit.extension.springboot.brokenparts.TwiceServingProviderService;
import io.vanillabp.cockpit.extension.springboot.brokenparts.VersionedProviderService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the application is told while it starts.
 * <p>
 * Two of these boots are meant to fail: a defect the extension can see at startup is refused
 * there and not when the first event arrives, and the message names the method to change. The
 * third is meant to succeed although the cockpit server is unreachable - a workflow module which
 * cannot start because a reporting server is down would be the worse failure of the two.
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
                "vanillabp.extensions.business-cockpit.rest.base-url=http://localhost:1")
            .properties(properties)
            .run()
            .close());
    Throwable cause = failure;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return String.valueOf(cause.getMessage());

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
            // the everyday way into a JDK proxy: an interface plus @Transactional. What the
            // bean's TYPE is then says nothing about the class the application wrote
            "spring.aop.proxy-target-class=false"
        },
        BrokenApplication.class, ProxiedVersionedProviderService.class);

    assertTrue(message.contains(ProxiedVersionedProviderService.class.getName()), message);
    assertTrue(message.contains("approve"), message);
    assertTrue(message.contains("version"), message);

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
  @DisplayName("A BPMS half registered as one list of bridges serves its adapter")
  public void bridgesRegisteredAsAListAreCollected() {

    try (var application = new SpringApplicationBuilder(
        TestApplication.class, BridgesOfASecondBpms.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-bridge-list;DB_CLOSE_DELAY=-1",
            "vanillabp.extensions.business-cockpit.rest.base-url=http://localhost:1")
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
  @DisplayName("A cockpit server which is down does not keep the application from starting")
  public void anUnreachableCockpitServerStillLetsTheApplicationStart() {

    try (var application = new SpringApplicationBuilder(TestApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-without-a-server;DB_CLOSE_DELAY=-1",
            "vanillabp.extensions.business-cockpit.rest.base-url=http://localhost:1")
        .run()) {

      assertNotNull(
          application.getBean(BusinessCockpitExtension.class));

    }

  }

}
