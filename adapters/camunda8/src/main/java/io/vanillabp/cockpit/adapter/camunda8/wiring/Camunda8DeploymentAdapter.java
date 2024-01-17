package io.vanillabp.cockpit.adapter.camunda8.wiring;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.model.bpmn.impl.BpmnModelInstanceImpl;
import io.camunda.zeebe.model.bpmn.impl.BpmnParser;
import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.Process;
import io.camunda.zeebe.model.bpmn.instance.UserTask;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeFormDefinition;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeLoopCharacteristics;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskDefinition;
import io.camunda.zeebe.spring.client.event.ZeebeClientCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedBpmn;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentService;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.utils.HashCodeInputStream;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8Connectable.Type;
import io.vanillabp.springboot.adapter.ModuleAwareBpmnDeployment;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Transactional
public class Camunda8DeploymentAdapter extends ModuleAwareBpmnDeployment {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8DeploymentAdapter.class);

    private final BpmnParser bpmnParser = new BpmnParser();

    private final DeploymentService deploymentService;

    private final Camunda8UserTaskWiring camunda8UserTaskWiring;

    private ZeebeClient client;

    public Camunda8DeploymentAdapter(
            final VanillaBpProperties properties,
            final DeploymentService deploymentService,
            Camunda8UserTaskWiring camunda8UserTaskWiring) {

        super(properties);
        this.deploymentService = deploymentService;
        this.camunda8UserTaskWiring = camunda8UserTaskWiring;
    }

    @Override
    protected Logger getLogger() {

        return logger;

    }

    @Override
    protected String getAdapterId() {

        return Camunda8AdapterConfiguration.ADAPTER_ID;

    }

    @EventListener
    public void zeebeClientCreated(
            final ZeebeClientCreatedEvent event) {

        this.client = event.getClient();

        deployAllWorkflowModules();
    }

    @Override
    public void doDeployment(
            final String workflowModuleId,
            final Resource[] bpmns,
            final Resource[] dmns,
            final Resource[] cmms) throws Exception {

        final var deploymentHashCode = new int[] { 0 };

        final var deployResourceCommand = client.newDeployResourceCommand();
        final var deployedProcesses = new HashMap<String, DeployedBpmn>();

        final boolean hasDeployables[] = { false };

        // Add all BPMNs to deploy-command: on one hand to deploy them and on the
        // other hand to wire them to the using project beans according to the SPI
        final var deploymentCommand = Arrays
                .stream(bpmns)
                .map(resource -> {
                    try (var inputStream = new HashCodeInputStream(
                            resource.getInputStream(),
                            deploymentHashCode[0])) {

                        logger.info("About to deploy '{}' of workflow-module with id '{}'",
                                resource.getFilename(), workflowModuleId);
                        final var model = bpmnParser.parseModelFromStream(inputStream);

                        final var bpmn = deploymentService.addBpmn(
                                model,
                                inputStream.hashCode(),
                                resource.getDescription());

                        processBpmnModel(workflowModuleId, deployedProcesses, bpmn, model, false);
                        deploymentHashCode[0] = inputStream.getTotalHashCode();

                        hasDeployables[0] = true;

                        return deployResourceCommand.addProcessModel(model, resource.getFilename());

                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                })
                .filter(Objects::nonNull)
                .reduce((first, second) -> second);

//        if (hasDeployables[0]) {
//            final var deployedResources = deploymentCommand
//                    .map(command -> command.send().join())
//                    .orElseThrow();
//
//            // BPMNs which are part of the current package will stored
//            deployedResources
//                    .getProcesses()
//                    .forEach(process ->  deploymentService.addProcess(
//                                    deploymentHashCode[0],
//                                    process,
//                                    deployedProcesses.get(process.getBpmnProcessId())).getDefinitionKey());
//
//        }

        // BPMNs which were deployed in the past need to be forced to be parsed for wiring
        deploymentService
                .getBpmnNotOfPackage(deploymentHashCode[0])
                .forEach(bpmn -> {
                    try (var inputStream = new ByteArrayInputStream(
                            bpmn.getResource())) {

                        logger.info("About to verify old BPMN '{}' of workflow-module with id '{}'",
                                bpmn.getResourceName(), workflowModuleId);
                        final var model = bpmnParser.parseModelFromStream(inputStream);

                        processBpmnModel(workflowModuleId, deployedProcesses, bpmn, model, true);

                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                });

    }

    private void processBpmnModel(
            final String workflowModuleId,
            final Map<String, DeployedBpmn> deployedProcesses,
            final DeployedBpmn bpmn,
            final BpmnModelInstanceImpl model,
            final boolean oldVersionBpmn) {

        List<Process> executableProcesses = model
                .getModelElementsByType(Process.class)
                .stream()
                .filter(Process::isExecutable)
                .toList();

        executableProcesses.forEach(process -> {
            deployedProcesses.put(process.getId(), bpmn);
        });

        executableProcesses
                .stream()
                .flatMap(process -> connectablesForType(process, model, UserTask.class))
                .forEach(connectable -> {
                    camunda8UserTaskWiring.wireTask(workflowModuleId, connectable);
                });

    }


    public Stream<Camunda8Connectable> connectablesForType(
            final Process process,
            final BpmnModelInstanceImpl model,
            final Class<? extends BaseElement> type) {

        final var kind = UserTask.class.isAssignableFrom(type) ? Type.USERTASK : Type.TASK;

        final var stream = model
                .getModelElementsByType(type)
                .stream()
                .filter(element -> Objects.equals(getOwningProcess(element), process))
                .map(element -> new Camunda8Connectable(
                        process,
                        element.getId(),
                        kind,
                        getTaskDefinition(kind, element),
                        element.getSingleExtensionElement(ZeebeLoopCharacteristics.class)))
                .filter(Camunda8Connectable::isExecutableProcess);

        if (kind == Type.USERTASK) {
            return stream;
        }

        return stream.filter(connectable -> connectable.getTaskDefinition() != null);

    }


    static Process getOwningProcess(
            final ModelElementInstance element) {

        if (element instanceof Process) {
            return (Process) element;
        }

        final var parent = element.getParentElement();
        if (parent == null) {
            return null;
        }

        return getOwningProcess(parent);

    }

    private String getTaskDefinition(
            final Type kind,
            final BaseElement element) {

        if (kind == Type.USERTASK) {

            final var formDefinition = element.getSingleExtensionElement(ZeebeFormDefinition.class);
            if (formDefinition == null) {
                return null;
            }
            return formDefinition.getFormKey();

        }

        final var taskDefinition = element.getSingleExtensionElement(ZeebeTaskDefinition.class);
        if (taskDefinition == null) {
            return null;
        }
        return taskDefinition.getType();

    }


}
