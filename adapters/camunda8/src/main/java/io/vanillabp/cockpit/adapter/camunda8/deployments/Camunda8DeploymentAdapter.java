package io.vanillabp.cockpit.adapter.camunda8.deployments;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.model.bpmn.impl.BpmnModelInstanceImpl;
import io.camunda.zeebe.model.bpmn.impl.BpmnParser;
import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.Process;
import io.camunda.zeebe.model.bpmn.instance.UserTask;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeFormDefinition;
import io.camunda.zeebe.spring.client.event.ZeebeClientCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.utils.HashCodeInputStream;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8WorkflowConnectable;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowWiring;
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

    private final Camunda8WorkflowWiring camunda8WorkflowWiring;

    private final String applicationName;

    private ZeebeClient client;

    public Camunda8DeploymentAdapter(
            final String applicationName,
            final VanillaBpProperties properties,
            final DeploymentService deploymentService,
            final Camunda8UserTaskWiring camunda8UserTaskWiring,
            final Camunda8WorkflowWiring camunda8WorkflowWiring) {

        super(properties);
        this.deploymentService = deploymentService;
        this.applicationName = applicationName;
        this.camunda8UserTaskWiring = camunda8UserTaskWiring;
        this.camunda8WorkflowWiring = camunda8WorkflowWiring;
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

                        processBpmnModel(workflowModuleId, deployedProcesses, bpmn, model);
                        deploymentHashCode[0] = inputStream.getTotalHashCode();

                        hasDeployables[0] = true;

                        return deployResourceCommand.addProcessModel(model, resource.getFilename());

                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                })
                .filter(Objects::nonNull)
                .reduce((first, second) -> second);

        if (hasDeployables[0]) {
            final var tenantId = workflowModuleId == null ? applicationName : workflowModuleId;
            final DeploymentEvent deployedResources = deploymentCommand
                    .map(command -> tenantId == null ? command : command.tenantId(tenantId))
                    .map(command -> command.send().join())
                    .orElseThrow();

            // BPMNs which are part of the current package will stored
            deployedResources
                    .getProcesses()
                    .forEach(process ->  {
                        deploymentService.addProcess(
                                deploymentHashCode[0],
                                process,
                                deployedProcesses.get(process.getBpmnProcessId()));
                    });

        }

        // BPMNs which were deployed in the past need to be forced to be parsed for wiring
        deploymentService
                .getBpmnNotOfPackage(deploymentHashCode[0])
                .forEach(bpmn -> {
                    try (var inputStream = new ByteArrayInputStream(
                            bpmn.getResource())) {

                        logger.info("About to verify old BPMN '{}' of workflow-module with id '{}'",
                                bpmn.getResourceName(), workflowModuleId);
                        final var model = bpmnParser.parseModelFromStream(inputStream);

                        processBpmnModel(workflowModuleId, deployedProcesses, bpmn, model);

                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                });

    }

    private void processBpmnModel(
            final String workflowModuleId,
            final Map<String, DeployedBpmn> deployedProcesses,
            final DeployedBpmn bpmn,
            final BpmnModelInstanceImpl model) {

        List<Process> executableProcesses = model
                .getModelElementsByType(Process.class)
                .stream()
                .filter(Process::isExecutable)
                .toList();

        executableProcesses
                .forEach(process -> deployedProcesses.put(process.getId(), bpmn));

        wireWorkflows(workflowModuleId, executableProcesses);
        wireUserTasks(workflowModuleId, model, executableProcesses);

    }

    private void wireWorkflows(String workflowModuleId, List<Process> executableProcesses) {
        executableProcesses
                .stream()
                .map(process -> new Camunda8WorkflowConnectable(process.getId(), process.getName()))
                .forEach(connectable -> camunda8WorkflowWiring.wireWorkflow(workflowModuleId, connectable));
    }

    private void wireUserTasks(String workflowModuleId, BpmnModelInstanceImpl model, List<Process> executableProcesses) {
        executableProcesses
                .stream()
                .flatMap(process -> getUserTaskConnectables(process, model))
                .forEach(connectable ->
                        camunda8UserTaskWiring.wireTask(workflowModuleId, connectable));
    }


    public Stream<Camunda8UserTaskConnectable> getUserTaskConnectables(
            final Process process,
            final BpmnModelInstanceImpl model) {

        return model
                .getModelElementsByType(UserTask.class)
                .stream()
                .filter(element -> Objects.equals(getOwningProcess(element), process))
                .map(element -> new Camunda8UserTaskConnectable(
                        process,
                        element.getId(),
                        getFormKey(element),
                        getTaskName(element)))
                .filter(Camunda8UserTaskConnectable::isExecutableProcess);
    }


    private static Process getOwningProcess(
            final ModelElementInstance element) {

        if (element instanceof Process) {
            return (Process) element;
        }

        ModelElementInstance parent = element.getParentElement();
        return parent == null ? null : getOwningProcess(parent);
    }

    private String getFormKey(final BaseElement element) {
        final ZeebeFormDefinition formDefinition = element.getSingleExtensionElement(ZeebeFormDefinition.class);
        return formDefinition == null ? null : formDefinition.getFormKey();
    }

    private String getTaskName(
            final BaseElement element
    ){
        return element.getAttributeValue("name");
    }
}
