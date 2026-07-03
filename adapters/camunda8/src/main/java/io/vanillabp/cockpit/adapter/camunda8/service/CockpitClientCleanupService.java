package io.vanillabp.cockpit.adapter.camunda8.service;

import io.camunda.client.event.CamundaClientClosingEvent;
import io.vanillabp.cockpit.adapter.camunda8.deployments.Camunda8DeploymentAdapter;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

/**
 * In Spring Boot tests, where application contexts are reused, we do not want to keep references to old
 * Repositories / CamundaClients. These should be recreated when required.
 */
public class CockpitClientCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(CockpitClientCleanupService.class);

    private final Camunda8DeploymentAdapter deploymentAdapter;
    private final Camunda8UserTaskWiring userTaskWiring;
    private final Camunda8WorkflowWiring workflowWiring;

    public CockpitClientCleanupService(Camunda8DeploymentAdapter deploymentAdapter,
                                       Camunda8UserTaskWiring userTaskWiring,
                                       Camunda8WorkflowWiring workflowWiring) {
        this.deploymentAdapter = deploymentAdapter;
        this.userTaskWiring = userTaskWiring;
        this.workflowWiring = workflowWiring;
    }

    @EventListener
    public void onContextClosed(CamundaClientClosingEvent event) {
        logger.debug("Camunda client closed, cleaning up: {}", event);
        deploymentAdapter.doCleanup();
        userTaskWiring.doCleanup();
        workflowWiring.doCleanup();
    }
}