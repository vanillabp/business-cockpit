package io.vanillabp.cockpit.workflowlist;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WorkflowlistService {

    @Autowired
    private Logger logger;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MicroserviceProxyRegistry microserviceProxyRegistry;

    public Mono<Boolean> createWorkflow(
            final Workflow workflow) {

        if (workflow == null) {
            return Mono.just(Boolean.FALSE);
        }

        return workflowRepository
                .save(workflow)
                .doOnNext(item -> microserviceProxyRegistry
                        .registerMicroservice(
                                item.getWorkflowModule(),
                                item.getWorkflowModuleUri()))
                .map(item -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });
    }

}
