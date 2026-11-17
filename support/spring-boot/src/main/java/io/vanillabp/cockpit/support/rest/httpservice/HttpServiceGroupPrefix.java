package io.vanillabp.cockpit.support.rest.httpservice;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/**
 * Says where the keys of one HTTP service group live. A client module declares it as a bean next
 * to its {@code @ImportHttpServices}, and so can an application or a workflow module for a group of
 * its own:
 *
 * <pre>
 * &#64;Bean
 * HttpServiceGroupPrefix inventoryKeys() {
 *   return HttpServiceGroupPrefix.of("inventory", "inventory.client");
 * }
 * </pre>
 *
 * Then all keys of the group are read below {@code inventory.client}: Spring Boot's
 * ({@code base-url}, timeouts, SSL bundle, default headers) and the ones
 * {@link HttpServiceClientConventions} adds (proxy, logging, retry, {@code verify-ssl}, OAuth). A
 * group without such a bean has its keys where Spring Boot looks for them,
 * {@code spring.http.serviceclient.<group>}.
 *
 * @param group  the name of the group, as in {@code @ImportHttpServices(group = ...)}
 * @param prefix the key all keys of the group start with, for example {@code inventory.client}
 */
public record HttpServiceGroupPrefix(
                                     String group,
                                     String prefix) {

  /** Where Spring Boot reads the keys of a group, followed by the group's name. */
  public static final String SPRING_BOOT_PREFIX = "spring.http.serviceclient";

  /**
   * Checks the name of the group and the prefix.
   *
   * @param group  the name of the group
   * @param prefix the key all keys of the group start with
   * @throws IllegalArgumentException if the group has no name or the prefix is no valid key
   */
  public HttpServiceGroupPrefix {

    if ((group == null) || group.isBlank()) {
      throw new IllegalArgumentException("""
          An HttpServiceGroupPrefix needs the name of its group, as in \
          '@ImportHttpServices(group = ...)'.""");
    }
    if ((prefix == null) || !ConfigurationPropertyName.isValid(prefix) || prefix.isBlank()) {
      throw new IllegalArgumentException("""
          The HttpServiceGroupPrefix of the HTTP service group '%s' has the prefix '%s', which is \
          no valid property key. Use lower case words joined by dots and dashes, for example \
          'inventory.client'."""
          .formatted(group, prefix));
    }

  }

  /**
   * Says that the keys of the group {@code group} live below {@code prefix}.
   *
   * @param group  the name of the group, as in {@code @ImportHttpServices(group = ...)}
   * @param prefix the key all keys of the group start with, for example {@code inventory.client}
   * @return the prefix of the group
   */
  public static HttpServiceGroupPrefix of(
      final String group,
      final String prefix) {

    return new HttpServiceGroupPrefix(group, prefix);

  }

  /**
   * The prefix of a group which declares none: Spring Boot's own place for its keys.
   *
   * @param group the name of the group
   * @return {@code spring.http.serviceclient.<group>}
   */
  public static String springBootPrefixOf(
      final String group) {

    return SPRING_BOOT_PREFIX
        + "."
        + group;

  }

}
