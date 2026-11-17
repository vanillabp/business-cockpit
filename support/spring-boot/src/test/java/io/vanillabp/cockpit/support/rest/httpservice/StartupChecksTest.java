package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What stops the start, and what the developer reads then. Every problem of every group is in one
 * list, and each one names the key to set or to change.
 */
@ExtendWith(SuppressOutputExtension.class)
public class StartupChecksTest {

  interface OrdersInventory extends InventoryApi {
  }

  interface BillingInventory extends InventoryApi {
  }

  /** A group with keys below a prefix of its own. */
  @Configuration
  @ImportHttpServices(group = "orders-inventory", types = OrdersInventory.class)
  static class OrdersModule {

    @Bean
    HttpServiceGroupPrefix ordersInventoryKeys() {
      return HttpServiceGroupPrefix.of("orders-inventory", "orders.inventory");
    }

  }

  /** A group with its keys in Spring Boot's place. */
  @Configuration
  @ImportHttpServices(group = "billing-inventory", types = BillingInventory.class)
  static class BillingModule {
  }

  @Configuration
  static class PrefixOfAGroupWhichDoesNotExist {

    @Bean
    HttpServiceGroupPrefix misspelledKeys() {
      return HttpServiceGroupPrefix.of("order-inventory", "orders.inventory");
    }

  }

  @Configuration
  static class SecondPrefixOfTheOrdersGroup {

    @Bean
    HttpServiceGroupPrefix secondOrdersInventoryKeys() {
      return HttpServiceGroupPrefix.of("orders-inventory", "orders.stock");
    }

  }

  private static final Class<?>[] BOTH_MODULES = {
      TestApplication.class, OrdersModule.class, BillingModule.class
  };

  private static final Class<?>[] ORDERS_MODULE = {
      TestApplication.class, OrdersModule.class
  };

  private static final String ORDERS_URL = "orders.inventory.base-url=http://localhost:1/api";

  @Test
  public void a_missing_address_of_any_group_is_named_with_its_key() {

    final var problems = Starts.problemsOf(BOTH_MODULES);

    assertThat(problems)
        .anyMatch(problem -> problem.contains("'orders-inventory' has no address") && problem
            .contains("Set 'orders.inventory.base-url'"))
        .anyMatch(problem -> problem.contains("'billing-inventory' has no address") && problem
            .contains("Set 'spring.http.serviceclient.billing-inventory.base-url'"));

  }

  @Test
  public void an_address_with_a_placeholder_nothing_resolves_names_the_placeholder() {

    final var problems = Starts.problemsOf(ORDERS_MODULE,
        "orders.inventory.base-url=${INVENTORY_URL}/api");

    assertThat(problems).singleElement().asString()
        .contains("holds a placeholder nothing resolves")
        .contains("'INVENTORY_URL'")
        .contains("'orders.inventory.base-url'");

  }

  @Test
  public void an_address_without_scheme_or_host_is_refused() {

    final var problems = Starts.problemsOf(ORDERS_MODULE,
        "orders.inventory.base-url=inventory.example.com/api");

    assertThat(problems).singleElement().asString()
        .contains("'inventory.example.com/api' in 'orders.inventory.base-url' is no complete http or https address");

  }

  @Test
  public void a_key_nobody_reads_stops_the_start() {

    final var problems = Starts.problemsOf(ORDERS_MODULE, ORDERS_URL,
        "orders.inventory.read-timout=2s",
        "orders.inventory.retry.max-attempt=3",
        // known keys of every kind, which must not count
        "orders.inventory.default-header.X-Caller=stock-service",
        "orders.inventory.oauth.scope[0]=read",
        "orders.inventory.oauth.token-uri=http://localhost:1/token",
        "orders.inventory.oauth.client-id=stock-service",
        "orders.inventory.oauth.client-secret=s3cret",
        "orders.inventory.apiversion.insert.header=X-Version");

    assertThat(problems).containsExactly(
        "The key 'orders.inventory.read-timout' of the HTTP service group 'orders-inventory' is read by nobody. Check its spelling. The support README lists every key.",
        "The key 'orders.inventory.retry.max-attempt' of the HTTP service group 'orders-inventory' is read by nobody. Check its spelling. The support README lists every key.");

  }

  @Test
  public void keys_in_spring_boots_place_of_a_group_with_a_prefix_are_named() {

    final var problems = Starts.problemsOf(ORDERS_MODULE, ORDERS_URL,
        "spring.http.serviceclient.orders-inventory.read-timeout=2s");

    assertThat(problems).singleElement().asString()
        .contains("reads its keys below 'orders.inventory'")
        .contains("keys below 'spring.http.serviceclient.orders-inventory' as well");

  }

  @Test
  public void a_prefix_of_a_group_which_does_not_exist_is_named() {

    final var problems = Starts.problemsOf(new Class<?>[]{
        TestApplication.class, BillingModule.class, PrefixOfAGroupWhichDoesNotExist.class
    }, "spring.http.serviceclient.billing-inventory.base-url=http://localhost:1/api");

    assertThat(problems).singleElement().asString()
        .contains("names the HTTP service group 'order-inventory', but there is no such group")
        .contains("[billing-inventory]");

  }

  @Test
  public void two_prefixes_for_one_group_are_refused() {

    final var problems = Starts.problemsOf(new Class<?>[]{
        TestApplication.class, OrdersModule.class, SecondPrefixOfTheOrdersGroup.class
    }, ORDERS_URL);

    assertThat(problems).singleElement().asString()
        .contains("'orders-inventory' has two prefixes")
        .contains("import the API into one group per module");

  }

  @Test
  public void an_incomplete_proxy_is_named() {

    assertThat(Starts.problemsOf(ORDERS_MODULE, ORDERS_URL, "orders.inventory.proxy.host=proxy.example.com"))
        .singleElement().asString()
        .contains("has no port. Set 'orders.inventory.proxy.port'");
    assertThat(Starts.problemsOf(ORDERS_MODULE, ORDERS_URL, "orders.inventory.proxy.port=3128"))
        .singleElement().asString()
        .contains("no 'orders.inventory.proxy.host'");

  }

  @Test
  public void a_retry_without_attempts_is_refused() {

    final var problems = Starts.problemsOf(ORDERS_MODULE, ORDERS_URL,
        "orders.inventory.retry.enabled=true",
        "orders.inventory.retry.max-attempts=0");

    assertThat(problems).singleElement().asString()
        .contains("'orders.inventory.retry.max-attempts: 0'. Set at least 1");

  }

  @Test
  public void an_unknown_ssl_bundle_is_named() {

    final var problems = Starts.problemsOf(ORDERS_MODULE, ORDERS_URL,
        "orders.inventory.ssl.bundle=inventory");

    assertThat(problems).singleElement().asString()
        .contains("names the SSL bundle 'inventory' in 'orders.inventory.ssl.bundle', but there is no such bundle")
        .contains("'spring.ssl.bundle.jks.inventory'");

  }

  @Test
  public void a_value_which_cannot_be_read_is_named() {

    final var problems = Starts.problemsOf(ORDERS_MODULE, ORDERS_URL,
        "orders.inventory.read-timeout=a while");

    assertThat(problems).singleElement().asString()
        .contains("below 'orders.inventory' cannot be read")
        .contains("orders.inventory.read-timeout");

  }

  @Test
  public void an_incomplete_oauth_client_is_named_with_every_missing_key() {

    final var problems = Starts.problemsOf(ORDERS_MODULE, ORDERS_URL,
        "orders.inventory.oauth.token-uri=http://localhost:1/token",
        "orders.inventory.oauth.client-authentication-method=private_key_jwt",
        "orders.inventory.oauth.renew-before=-1s");

    assertThat(problems).hasSize(4)
        .anyMatch(problem -> problem.contains("no client id. Set 'orders.inventory.oauth.client-id'"))
        .anyMatch(problem -> problem.contains("no client secret. Set 'orders.inventory.oauth.client-secret'"))
        .anyMatch(problem -> problem.contains("'orders.inventory.oauth.client-authentication-method: private_key_jwt'"))
        .anyMatch(problem -> problem.contains("negative 'orders.inventory.oauth.renew-before'"));

  }

  @Test
  public void a_prefix_needs_a_group_and_a_valid_key() {

    assertThatThrownBy(() -> HttpServiceGroupPrefix.of(" ", "orders.inventory"))
        .hasMessageContaining("needs the name of its group");
    assertThatThrownBy(() -> HttpServiceGroupPrefix.of("orders-inventory", "Orders Inventory"))
        .hasMessageContaining("prefix 'Orders Inventory', which is no valid property key");

  }

  @Test
  public void the_failure_analysis_lists_the_problems_and_says_where_keys_live() {

    final var analysis = new HttpServiceClientsMisconfiguredFailureAnalyzer().analyze(
        new IllegalStateException("wrapped", new HttpServiceClientsMisconfigured(List.of("first", "second"))));

    assertThat(analysis.getDescription()).isEqualTo("""
        The keys of the HTTP service clients need a change:
        - first
        - second""");
    assertThat(analysis.getAction()).contains("'spring.http.serviceclient.<group>'");

  }

}
