/**
 * How an application gets the Business Cockpit: it adds the dependency, and Spring Boot reads the
 * auto-configurations listed in this jar's
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}. There is
 * no base class to extend and no component scan to inherit.
 * <p>
 * Every bean of the library is named in an {@code @Import} or in an {@code @Bean} method of one of
 * the classes here. That is more writing than a scan, and it is worth it. A scan collects whatever
 * happens to lie in the package at the moment it runs, so the question which of these classes are
 * part of what the library offers never has to be answered. An import can be made conditional and a
 * scan cannot. And Spring Boot advises against scanning from an auto-configuration at all, because
 * the result then depends on the order the auto-configuration ran in.
 * <p>
 * The consequence for anybody adding a bean to {@code io.vanillabp.cockpit}: the stereotype
 * annotation alone does not register it any more. Name the class in the auto-configuration of its
 * area. {@code EveryCockpitBeanIsRegisteredTest} fails when a class is forgotten, so this is not
 * something a review has to catch.
 * <p>
 * One configuration per area rather than one for everything, so an area's conditions sit where the
 * area is and the report Spring Boot prints with {@code --debug} says which of them applied.
 * <p>
 * The four {@code @Enable} annotations the cockpit needs act on the whole application and not only
 * on the cockpit. Each one has an auto-configuration of its own which logs on every start what it
 * does to the application and how to decide it yourself.
 */
package io.vanillabp.cockpit.autoconfigure;
