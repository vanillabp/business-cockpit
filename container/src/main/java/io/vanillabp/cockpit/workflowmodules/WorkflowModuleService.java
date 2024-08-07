package io.vanillabp.cockpit.workflowmodules;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModuleRepository;
import jakarta.annotation.PostConstruct;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class WorkflowModuleService {

    @Autowired
    private WorkflowModuleRepository workflowModules;

    @Autowired
    private MicroserviceProxyRegistry microserviceProxyRegistry;

    @PostConstruct
    public void registerProxiesForWorkflowModules() {

        workflowModules
                .findAll()
                .collectList()
                .map(all -> all
                        .stream()
                        .collect(Collectors.toMap(
                                WorkflowModule::getId,
                                WorkflowModule::getUri)))
                .doOnNext(microserviceProxyRegistry::registerMicroservices)
                .subscribe();

    }

    public Mono<Boolean> registerOrUpdateWorkflowModule(
            final String id,
            final String uri,
            final String taskProviderApiUriPath,
            final String workflowProviderApiUriPath) {

        final var updateWorkflowModule = workflowModules
                .findById(id)
                .switchIfEmpty(Mono.just(WorkflowModule.withId(id)))
                .flatMap(workflowModule -> {
                    if ((uri == null) && (workflowModule.getUri() != null)) return Mono.just(workflowModule);
                    if ((uri != null) && (workflowModule.getUri() == null)) return Mono.just(workflowModule);
                    if ((uri != null) && !uri.equals(workflowModule.getUri())) return Mono.just(workflowModule);
                    if ((taskProviderApiUriPath == null) && (workflowModule.getTaskProviderApiUriPath() != null)) return Mono.just(workflowModule);
                    if ((taskProviderApiUriPath != null) && (workflowModule.getTaskProviderApiUriPath() == null)) return Mono.just(workflowModule);
                    if ((taskProviderApiUriPath != null) && !taskProviderApiUriPath.equals(workflowModule.getTaskProviderApiUriPath())) return Mono.just(workflowModule);
                    if ((workflowProviderApiUriPath == null) && (workflowModule.getWorkflowProviderApiUriPath() != null)) return Mono.just(workflowModule);
                    if ((workflowProviderApiUriPath != null) && (workflowModule.getWorkflowProviderApiUriPath() == null)) return Mono.just(workflowModule);
                    if ((workflowProviderApiUriPath != null) && !workflowProviderApiUriPath.equals(workflowModule.getWorkflowProviderApiUriPath())) return Mono.just(workflowModule);
                    return Mono.empty();
                })
                .doOnNext(workflowModule -> {
                    workflowModule.setUri(uri);
                    workflowModule.setTaskProviderApiUriPath(taskProviderApiUriPath);
                    workflowModule.setWorkflowProviderApiUriPath(workflowProviderApiUriPath);
                })
                .flatMap(workflowModules::save)
                .doOnNext(workflowModule -> {
                    microserviceProxyRegistry.registerMicroservice(
                            workflowModule.getId(),
                            workflowModule.getUri());
                });

        return updateWorkflowModule
                .onErrorResume(OptimisticLockingFailureException.class, e -> updateWorkflowModule)
                .map(workflowModule -> Boolean.TRUE)
                .switchIfEmpty(Mono.just(Boolean.FALSE));

    }

    public Mono<WorkflowModule> getWorkflowModule(
            final String id) {

        if (id == null) {
            return Mono.empty();
        }
        return workflowModules.findById(id);

    }

    public Flux<WorkflowModule> getWorkflowModules() {

        return workflowModules.findAll(); // TODO: Limit to permitted workflow modules

    }

}
