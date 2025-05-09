package io.vanillabp.cockpit.devshell.simulator;

import io.vanillabp.cockpit.devshell.simulator.config.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(Properties.class)
public class DevShellSimulator {

    public static void main(String[] args) {
        SpringApplication.run(DevShellSimulator.class, args);
    }
}
