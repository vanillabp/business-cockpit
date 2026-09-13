package io.vanillabp.cockpit;

/**
 * Nothing, and on purpose.
 * <p>
 * This class used to carry the annotations a cockpit application needed, and extending it was how an
 * application got them. That is gone: the annotations live in the auto-configurations of
 * {@code io.vanillabp.cockpit.autoconfigure}, which Spring Boot reads out of this jar. An
 * application writes its own class and nothing else:
 * <pre>
 * &#64;SpringBootApplication
 * public class MyCockpit {
 *     public static void main(String... args) {
 *         SpringApplication.run(MyCockpit.class, args);
 *     }
 * }
 * </pre>
 * The class stays for one release so that an application still naming it keeps compiling. Extending
 * it adds no annotation, no bean and no component scan, so it changes nothing either way, and
 * {@code BaseClassCarriesNothingTest} is what keeps that true. It goes away with 1.0, which
 * <a href="https://github.com/vanillabp/business-cockpit/wiki/Releases">Releases</a> says as well.
 */
@Deprecated(since = "0.9.0", forRemoval = true)
public class BusinessCockpitApplication {

}
