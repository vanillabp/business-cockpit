package io.vanillabp.cockpit.extension.springboot.deployedversionparts;

import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.bpmsdouble.DummyTaskWiringSource;
import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionApplication;
import io.vanillabp.integration.adapter.spi.workflowtask.BpmnTaskSpec;

/**
 * Makes the BPMN file of the BPMS double hold a second process, the one a call activity calls.
 */
@Configuration
public class CalledProcessInTheSameFile {

  @Bean
  public DummyTaskWiringSource aFileWithTwoProcesses() {

    return new DummyTaskWiringSource() {

      @Override
      public Collection<BpmnTaskSpec> tasksOf(
          final String adapterId,
          final String workflowModuleId,
          final String bpmnProcessId) {

        // the user task is no @WorkflowTask, so VanillaBP's wiring has nothing to check
        return List.of();

      }

      @Override
      public List<String> executableProcessesOf(
          final String adapterId,
          final String workflowModuleId,
          final String filename) {

        return List.of(DeployedVersionApplication.BPMN_PROCESS, DeployedVersionApplication.CALLED_PROCESS);

      }

    };

  }

}
