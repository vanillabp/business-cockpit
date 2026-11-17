package io.vanillabp.cockpit.support.rest.httpservice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Imports an HTTP service group only if the application sets its address, the key
 * {@code <prefix>.base-url}. A module which calls a service only if the application wants it puts
 * it on the class which imports the group:
 *
 * <pre>
 * &#64;Configuration
 * &#64;ConditionalOnHttpServiceGroupAddress(prefix = "orders.inventory")
 * &#64;ImportHttpServices(group = "orders-inventory", types = OrdersInventory.class)
 * class OrdersInventoryClient {
 * }
 * </pre>
 *
 * Without the key there is no group and no client, and the application starts. With the key the
 * group is checked like every other group by {@link HttpServiceClientConventions}. Spring Boot's
 * {@code @ConditionalOnProperty} would read the value and stop the start at a placeholder nothing
 * resolves, like {@code ${INVENTORY_URL}}, before the conventions can name it. This condition only
 * asks whether the key is set, so the conventions still say what is wrong with the address.
 * <p>
 * The prefix is named here and not taken from the {@link HttpServiceGroupPrefix} bean, because the
 * condition is decided before any bean exists.
 */
@Target({
    ElementType.TYPE, ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnHttpServiceGroupAddressCondition.class)
public @interface ConditionalOnHttpServiceGroupAddress {

  /**
   * The prefix of the group's keys, for example {@code orders.inventory}, or
   * {@code spring.http.serviceclient.<group>} for a group in Spring Boot's place.
   *
   * @return the key the address is set below
   */
  String prefix();

}
