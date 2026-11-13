package io.vanillabp.cockpit.devshell.extending;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import io.vanillabp.cockpit.devshell.simulator.DevShellSimulator;
import io.vanillabp.cockpit.devshell.simulator.config.Properties;

/**
 * An application which extends the simulator, the way a project adds endpoints of its own. It lives
 * outside the simulator's package, so the scan of the simulator's own tests does not find it.
 */
@SpringBootApplication
@EnableConfigurationProperties(Properties.class)
@ComponentScan(basePackageClasses = {
    ExtendingSimulator.class, DevShellSimulator.class
})
public class ExtendingSimulator extends DevShellSimulator {

  /** Creates the application. Spring Boot calls it. */
  public ExtendingSimulator() {
  }

}
