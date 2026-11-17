package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

/**
 * The conventions take everything they need in their constructor, so a test of a client module
 * creates them with {@code new}, without an application context, and gets the same client.
 */
@ExtendWith(SuppressOutputExtension.class)
public class WithoutSpringTest {

  private static final String GROUP = "labels";

  static final MockWebServer server = new MockWebServer();

  @AfterAll
  static void stop() {
    server.close();
  }

  /** The one group of a test, handed to the conventions as Spring would hand it. */
  private record OneGroup(
                          RestClient.Builder builder) implements HttpServiceGroupConfigurer.Groups<RestClient.Builder> {

    private static final HttpServiceGroup LABELS = new HttpServiceGroup() {

      @Override
      public String name() {
        return GROUP;
      }

      @Override
      public Set<Class<?>> httpServiceTypes() {
        return Set.of(InventoryApi.class);
      }

      @Override
      public ClientType clientType() {
        return ClientType.REST_CLIENT;
      }

    };

    @Override
    public void forEachClient(
        final HttpServiceGroupConfigurer.ClientCallback<RestClient.Builder> callback) {
      callback.withClient(LABELS, builder);
    }

    @Override
    public HttpServiceGroupConfigurer.Groups<RestClient.Builder> filterByName(
        final String... groupNames) {
      throw new UnsupportedOperationException("the conventions configure every group");
    }

    @Override
    public HttpServiceGroupConfigurer.Groups<RestClient.Builder> filter(
        final Predicate<HttpServiceGroup> predicate) {
      throw new UnsupportedOperationException("the conventions configure every group");
    }

    @Override
    public void forEachClient(
        final HttpServiceGroupConfigurer.InitializingClientCallback<RestClient.Builder> callback) {
      throw new UnsupportedOperationException("the conventions do not create builders");
    }

    @Override
    public void forEachProxyFactory(
        final HttpServiceGroupConfigurer.ProxyFactoryCallback callback) {
      throw new UnsupportedOperationException("the conventions do not change the proxy factory");
    }

    @Override
    public void forEachGroup(
        final HttpServiceGroupConfigurer.GroupCallback<RestClient.Builder> callback) {
      throw new UnsupportedOperationException("the conventions configure the builders only");
    }

  }

  @Test
  public void conventions_created_with_new_build_a_working_client() {

    final var environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("keys", Map.of(
        "labels.client.base-url", Stubs.apiUrl(server),
        "labels.client.default-header.X-Tenant", "north")));
    final var conventions = new HttpServiceClientConventions(environment, null, null, null, List.of(
        HttpServiceGroupPrefix.of(GROUP, "labels.client")));
    final var builder = RestClient.builder();

    conventions.configureGroups(new OneGroup(builder));

    final var labels = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(builder.build()))
        .build()
        .createClient(InventoryApi.class);
    server.enqueue(new MockResponse.Builder()
        .code(HttpStatus.OK.value())
        .addHeader("Content-Type", "text/plain")
        .body("Shelf 3, left")
        .build());
    assertThat(labels.getLabel("1").getBody()).isEqualTo("Shelf 3, left");
    assertThat(Stubs.next(server).getHeaders().get("X-Tenant")).isEqualTo("north");

  }

}
