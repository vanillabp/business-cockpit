package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/** The application of the tests. It scans no components, so nested configurations stay apart. */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TestApplication {
}
