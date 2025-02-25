package io.vanillabp.cockpit.devshell.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackageClasses = BlueprintDevShellSimulator.class)
public class BlueprintDevShellSimulator {

    public static void main(String[] args) {
        SpringApplication.run(BlueprintDevShellSimulator.class);
    }
}
