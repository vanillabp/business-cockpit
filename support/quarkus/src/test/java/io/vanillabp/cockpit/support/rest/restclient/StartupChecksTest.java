package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Keys which cannot work stop the building of a client, and every problem of a prefix is named
 * with its key and what to do: a missing or broken address, an address which ends with the path
 * of the API the client adds itself, an incomplete proxy, a pool without connections, retries without
 * attempts, an OAuth client without credentials, a value which cannot be read, and a key nobody
 * reads, with the key that was meant.
 */
@ExtendWith(SuppressOutputExtension.class)
public class StartupChecksTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(
          root -> TheRestClientsOnQuarkus.withTheRestClients(root).addClasses(InventoryWithItsPath.class,
              InventoryApiPath.class))
      .overrideConfigKey("no-address.read-timeout", "2s")
      .overrideConfigKey("placeholder.base-url", "${INVENTORY_URL_NOBODY_SET}/api")
      .overrideConfigKey("relative.base-url", "inventory/api")
      .overrideConfigKey("with-the-path.base-url", "https://shop.example.com/inventory/v2/")
      .overrideConfigKey("shop.base-url", "https://shop.example.com/inventory")
      .overrideConfigKey("typos.base-url", "https://inventory.example.com/api")
      .overrideConfigKey("typos.read-timout", "2s")
      .overrideConfigKey("%test.typos.proxy.hots", "proxy.example.com")
      .overrideConfigKey("typos.ssl.bundle", "inventory")
      .overrideConfigKey("typos.apiversion.default-version", "2")
      .overrideConfigKey("typos.something-else-entirely", "x")
      .overrideConfigKey("typos.default-header.X-Tenant", "north")
      .overrideConfigKey("typos.oauth.scope[0]", "stock.read")
      .overrideConfigKey("typos.oauth.token-uri", "https://login.example.com/oauth/token")
      .overrideConfigKey("typos.oauth.client-id", "stock-service")
      .overrideConfigKey("typos.oauth.client-secret", "s3cret")
      .overrideConfigKey("proxy-without-port.base-url", "https://inventory.example.com/api")
      .overrideConfigKey("proxy-without-port.proxy.host", "proxy.example.com")
      .overrideConfigKey("proxy-without-host.base-url", "https://inventory.example.com/api")
      .overrideConfigKey("proxy-without-host.proxy.port", "3128")
      .overrideConfigKey("no-attempts.base-url", "https://inventory.example.com/api")
      .overrideConfigKey("no-attempts.retry.enabled", "true")
      .overrideConfigKey("no-attempts.retry.max-attempts", "0")
      .overrideConfigKey("empty-pool.base-url", "https://inventory.example.com/api")
      .overrideConfigKey("empty-pool.connection-pool-size", "0")
      .overrideConfigKey("wrong-value.base-url", "https://inventory.example.com/api")
      .overrideConfigKey("wrong-value.read-timeout", "a while")
      .overrideConfigKey("wrong-value.redirects", "sometimes")
      .overrideConfigKey("incomplete-oauth.base-url", "https://inventory.example.com/api")
      .overrideConfigKey("incomplete-oauth.oauth.token-uri", "login/oauth/token")
      .overrideConfigKey("incomplete-oauth.oauth.client-authentication-method", "private_key_jwt")
      .overrideConfigKey("incomplete-oauth.oauth.renew-before", "-1s");

  private static String problemsOf(
      final String prefix) {

    final var failure = org.assertj.core.api.Assertions.catchThrowableOfType(RestClientsMisconfigured.class,
        () -> TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, prefix));
    assertThat(failure).as("the keys at '%s' are refused", prefix).isNotNull();
    assertThat(failure.getMessage())
        .startsWith("The REST clients at '%s' cannot be built, so the application does not start:".formatted(prefix))
        .contains("The keys of REST clients and their defaults are listed in the support README.");
    return failure.getMessage();

  }

  @Test
  public void a_missing_address_is_named_with_its_key() {

    assertThat(problemsOf("no-address"))
        .contains("The REST clients at 'no-address' have no address. Set 'no-address.base-url'");

  }

  @Test
  public void an_address_with_a_placeholder_nothing_resolves_is_named() {

    assertThat(problemsOf("placeholder"))
        .contains("The address of the REST clients at 'placeholder' holds a placeholder nothing resolves. "
            + "Set the key or environment variable 'INVENTORY_URL_NOBODY_SET'")
        .contains("INVENTORY_URL_NOBODY_SET")
        .doesNotContain("have no address");

  }

  @Test
  public void an_address_without_scheme_or_host_is_refused() {

    assertThat(problemsOf("relative"))
        .contains("The address 'inventory/api' in 'relative.base-url' is no complete http or https address");

  }

  @Test
  public void an_address_which_ends_with_the_path_of_the_interface_is_refused() {

    final var failure = org.assertj.core.api.Assertions.catchThrowableOfType(RestClientsMisconfigured.class,
        () -> TheRestClientsOnQuarkus.fresh().build(InventoryWithItsPath.class, "with-the-path"));

    assertThat(failure).isNotNull();
    assertThat(failure.getProblems()).containsExactly(
        "The address 'https://shop.example.com/inventory/v2/' in 'with-the-path.base-url' ends with '/inventory/v2', the path of the API which the client InventoryWithItsPath adds to the address itself. Set 'with-the-path.base-url' to the address without it: 'https://shop.example.com'.");

  }

  @Test
  public void an_address_without_the_path_of_the_interface_builds_the_client() {

    assertThat(TheRestClientsOnQuarkus.fresh().build(InventoryWithItsPath.class, "shop")).isNotNull();

  }

  @Test
  public void the_path_of_an_api_starts_with_a_slash() {

    assertThatThrownBy(() -> new RestClientApiPath("inventory/v2", java.util.List.of(InventoryWithItsPath.class)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("starts with '/'");

  }

  @Test
  public void keys_nobody_reads_are_named_with_what_was_meant() {

    assertThat(problemsOf("typos"))
        .contains("The key 'typos.read-timout' of the REST clients at 'typos' is read by nobody. "
            + "Did you mean 'typos.read-timeout'?")
        // the key holds for the profile 'test' only, and Quarkus lists it without the profile
        .contains("The key 'typos.proxy.hots' of the REST clients at 'typos' is read by nobody. "
            + "Did you mean 'typos.proxy.host'?")
        .contains("'typos.ssl.bundle'", "name it in 'typos.tls-configuration-name'")
        .contains("'typos.apiversion.default-version'", "'typos.default-header.<name>'")
        .contains("'typos.something-else-entirely' of the REST clients at 'typos' is read by nobody. "
            + "The support README lists every key.")
        .doesNotContain("X-Tenant")
        .doesNotContain("scope");

  }

  @Test
  public void an_incomplete_proxy_is_named() {

    assertThat(problemsOf("proxy-without-port"))
        .contains("The proxy 'proxy.example.com' of the REST clients at 'proxy-without-port' has no port. "
            + "Set 'proxy-without-port.proxy.port'.");
    assertThat(problemsOf("proxy-without-host"))
        .contains("have proxy keys, but no 'proxy-without-host.proxy.host'");

  }

  @Test
  public void a_pool_without_connections_is_refused() {

    assertThat(problemsOf("empty-pool")).contains("'empty-pool.connection-pool-size: 0'. Set at least 1");

  }

  @Test
  public void a_retry_without_attempts_is_refused() {

    assertThat(problemsOf("no-attempts")).contains("'no-attempts.retry.max-attempts: 0'. Set at least 1");

  }

  @Test
  public void a_value_which_cannot_be_read_is_named() {

    assertThat(problemsOf("wrong-value"))
        .contains("The value of 'wrong-value.read-timeout' cannot be read")
        .contains("'wrong-value.redirects' is 'sometimes'. Set one of follow-when-possible, follow, dont-follow.");

  }

  @Test
  public void an_incomplete_oauth_client_is_named_with_every_missing_key() {

    assertThat(problemsOf("incomplete-oauth"))
        .contains("The address 'login/oauth/token' in 'incomplete-oauth.oauth.token-uri' is no complete")
        .contains("no client id. Set 'incomplete-oauth.oauth.client-id'.")
        .contains("no client secret. Set 'incomplete-oauth.oauth.client-secret'.")
        .contains("'incomplete-oauth.oauth.client-authentication-method: private_key_jwt'")
        .contains("a negative 'incomplete-oauth.oauth.renew-before'");

  }

  @Test
  public void a_prefix_needs_a_valid_key_of_the_module() {

    assertThatThrownBy(() -> TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, "loan approval"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("The prefix 'loan approval' of REST clients is no valid key");
    for (final var taken : new String[]{
        "quarkus.rest-client.inventory", "business-cockpit.inventory", "vanillabp.inventory"
    }) {
      assertThatThrownBy(() -> TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, taken))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("The keys of a REST client belong to the module which calls the service");
    }

  }

}
