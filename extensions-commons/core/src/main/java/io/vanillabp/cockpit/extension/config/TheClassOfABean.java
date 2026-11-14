package io.vanillabp.cockpit.extension.config;

/**
 * Names the class a developer wrote for a bean, for a message at startup.
 * <p>
 * The class of a bean is not always that class. Quarkus hands an {@code @ApplicationScoped} bean
 * out as a client proxy, a class named <code>&lt;bean class&gt;_ClientProxy</code>, and a bean
 * with an interceptor as a subclass named <code>&lt;bean class&gt;_Subclass</code>. Spring hands
 * out a CGLIB subclass whose name carries <code>$$</code>. Each of them extends the class the
 * developer wrote. A message which names the generated class sends the developer looking for a
 * file which does not exist. The core knows neither platform, so it goes by these names and not
 * by the platform's own way of unwrapping a proxy.
 */
public final class TheClassOfABean {

  private TheClassOfABean() {
  }

  /**
   * Names the class of a bean, and the class behind it where the bean is a proxy.
   *
   * @param bean A bean as a platform handed it out
   * @return The name of the class the developer wrote for it
   */
  public static String nameOf(
      final Object bean) {

    Class<?> type = bean.getClass();
    while (isGenerated(type) && (type.getSuperclass() != null) && (type.getSuperclass() != Object.class)) {
      type = type.getSuperclass();
    }
    return type.getName();

  }

  private static boolean isGenerated(
      final Class<?> type) {

    final var name = type.getName();
    return name.endsWith("_ClientProxy") || name.endsWith("_Subclass") || name.contains("$$");

  }

}
