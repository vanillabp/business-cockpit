package io.vanillabp.cockpit.devshell.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.vanillabp.cockpit.devshell.simulator.config.Properties;

/**
 * The Spring Boot application of the dev shell simulator. It plays the Business Cockpit for the
 * dev shell of a workflow module: it takes the events of the BPMS API, serves the official API
 * and lets the developer switch between made-up users.
 */
@SpringBootApplication
@EnableConfigurationProperties(Properties.class)
public class DevShellSimulator {

  /**
   * Creates the application. Spring Boot calls it, nobody else does.
   */
  public DevShellSimulator() {
  }

  /**
   * Starts the simulator.
   *
   * @param args The command line arguments, passed on to Spring Boot
   */
  public static void main(
      String[] args) {
    SpringApplication.run(DevShellSimulator.class, args);
  }
}
