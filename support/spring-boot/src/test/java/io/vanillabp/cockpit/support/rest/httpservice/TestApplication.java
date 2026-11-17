package io.vanillabp.cockpit.support.rest.httpservice;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * The application of the tests. It scans no components, so the groups a test declares in a nested
 * configuration do not leak into the other tests.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TestApplication {
}
