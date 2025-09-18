package io.vanillabp.cockpit.adapter.camunda8.deployments;

import io.camunda.client.CamundaClient;
import io.camunda.spring.client.event.CamundaClientCreatedEvent;
import io.camunda.zeebe.model.bpmn.impl.BpmnModelInstanceImpl;
import io.camunda.zeebe.model.bpmn.impl.BpmnParser;
import io.camunda.zeebe.model.bpmn.instance.BaseElement;
import io.camunda.zeebe.model.bpmn.instance.ExtensionElements;
import io.camunda.zeebe.model.bpmn.instance.Process;
import io.camunda.zeebe.model.bpmn.instance.UserTask;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeExecutionListener;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeExecutionListenerEventType;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeExecutionListeners;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeFormDefinition;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskListener;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskListenerEventType;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeTaskListeners;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeUserTask;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeVersionTag;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8VanillaBpProperties;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8WorkflowConnectable;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowWiring;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.springboot.adapter.ModuleAwareBpmnDeployment;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.camunda.bpm.model.xml.impl.util.IoUtil;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import static io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER;

public class Camunda8DeploymentAdapter extends ModuleAwareBpmnDeployment {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8DeploymentAdapter.class);

    public static final String VERSIONINFO_CURRENT = "current";
    public static final String ADAPTER_PACKAGE = "io.vanillabp.camunda8.businesscockpit";
    public static final String PROPERTY_DEPLOYMENT_PRIORITY = "io.vanillabp.deployment.priority";

    public static final String PROPERTY_TASKLISTENER_PREFIXES = "io.vanillabp.businesscockpit.tasklistener.prefixes";

    public static final String PROPERTY_EXECUTIONLISTENER_PREFIXES = "io.vanillabp.businesscockpit.executionlistener.prefixes";

    private final BpmnParser bpmnParser = new BpmnParser();

    private final ApplicationEventPublisher applicationEventPublisher;

    private final Camunda8UserTaskWiring camunda8UserTaskWiring;

    private final Camunda8WorkflowWiring camunda8WorkflowWiring;

    private final Camunda8VanillaBpProperties camunda8Properties;

    private final VanillaBpCockpitProperties cockpitProperties;

    private CamundaClient client;

    @SuppressWarnings("unchecked")
    public static void initializeCrossCuttingProperties() {
        ModuleAwareBpmnDeployment.adapterProperties.put(
                PROPERTY_TASKLISTENER_PREFIXES,
                List.of(Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER));
        ModuleAwareBpmnDeployment.adapterProperties.put(
                PROPERTY_EXECUTIONLISTENER_PREFIXES,
                List.of(Camunda8WorkflowWiring.TASKDEFINITION_WORKFLOW_DETAILSPROVIDER));

        final var existingPriorities = (List<String>) ModuleAwareBpmnDeployment.adapterProperties
                .get(PROPERTY_DEPLOYMENT_PRIORITY);
        if (existingPriorities == null) {
            final var priorities = new LinkedList<String>();
            priorities.add(ADAPTER_PACKAGE);
            ModuleAwareBpmnDeployment.adapterProperties.put(PROPERTY_DEPLOYMENT_PRIORITY, priorities);
        } else {
            // set priority of business cockpit adapter to lowest, to enforce task-listeners are added after other adapters
            existingPriorities.add(existingPriorities.size(), ADAPTER_PACKAGE);
        }
    }

    public Camunda8DeploymentAdapter(
            final String applicationName,
            final VanillaBpProperties properties,
            final Camunda8VanillaBpProperties camunda8Properties,
            final VanillaBpCockpitProperties cockpitProperties,
            final Camunda8UserTaskWiring camunda8UserTaskWiring,
            final Camunda8WorkflowWiring camunda8WorkflowWiring,
            final ApplicationEventPublisher applicationEventPublisher) {

        super(properties, applicationName);
        this.camunda8Properties = camunda8Properties;
        this.cockpitProperties = cockpitProperties;
        this.camunda8UserTaskWiring = camunda8UserTaskWiring;
        this.camunda8WorkflowWiring = camunda8WorkflowWiring;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    protected Logger getLogger() {

        return logger;

    }

    @Override
    protected String getAdapterId() {

        return Camunda8AdapterConfiguration.ADAPTER_ID;

    }

    private void examineProcessVersionTags(
            final BpmnModelInstanceImpl model,
            final BiConsumer<String, String> versionTagConsumer) {

        model
                .getModelElementsByType(Process.class)
                .stream()
                .filter(Process::isExecutable)
                .filter(process -> process.getSingleExtensionElement(ZeebeVersionTag.class) != null)
                .forEach(process -> versionTagConsumer.accept(process.getId(), process.getSingleExtensionElement(ZeebeVersionTag.class).getValue()));

    }

    @EventListener(ApplicationReadyEvent.class)
    public void deployBpmnModels() {

        synchronized (ModuleAwareBpmnDeployment.bpmnModelCache) {

            if (ModuleAwareBpmnDeployment.bpmnModelCache.isEmpty()) {
                return;
            }

            final var resourcesDeployed = new HashSet<String>();

            ModuleAwareBpmnDeployment.bpmnModelCache
                    .entrySet()
                    .stream()
                    .collect(Collectors.groupingBy(
                            entry -> entry.getValue().getKey(),  // grouping by workflow module id
                            Collectors.mapping(entry -> Map.entry(entry.getKey(), entry.getValue().getValue()), // preserve resource and model
                                    Collectors.toSet())))
                    // for each workflow module
                    .forEach((workflowModuleId, resources) -> {
                        final var tenantId = camunda8Properties.getTenantId(workflowModuleId);
                        final var deployResourceCommand = client.newDeployResourceCommand();
                        final var processVersionTags = new HashMap<String, String>();
                        // deploy all bpmns at once
                        resources
                                .stream()
                                .peek(resource -> {
                                    if (logger.isTraceEnabled()) {
                                        logger.warn("Generated BPMN (business cockpit):\n{}", IoUtil.convertXmlDocumentToString(
                                                ((BpmnModelInstanceImpl) resource.getValue()).getDocument()));
                                    }
                                })
                                .map(resource -> {
                                    final var model = (BpmnModelInstanceImpl) resource.getValue();
                                    examineProcessVersionTags(model, processVersionTags::put);
                                    resourcesDeployed.add(resource.getKey());
                                    return deployResourceCommand.addProcessModel(model, resource.getKey());
                                })
                                .reduce((first, second) -> second)
                                .map(command -> tenantId == null ? command : command.tenantId(tenantId))
                                .map(command -> {
                                    logger.info("About to deploy BPMNs of workflow-module '{}' for business cockpit", workflowModuleId);
                                    return command.send().join();
                                })
                                .ifPresent(result -> {
                                    logger.info("Deployed {} BPMNs of workflow-module '{}' for business cockpit",
                                            result.getProcesses().size(), workflowModuleId);
                                    ModuleAwareBpmnDeployment.bpmnModelCache.remove(workflowModuleId);
                                    applicationEventPublisher.publishEvent(new BpmnModelCacheProcessed(
                                            this.getClass().getName(),
                                            workflowModuleId,
                                            result
                                                    .getProcesses()
                                                    .stream()
                                                    .map(process -> Map.entry(
                                                            process.getBpmnProcessId(),
                                                            processVersionTags.containsKey(process.getBpmnProcessId())
                                                                    ? "%s:%d".formatted(processVersionTags.get(process.getBpmnProcessId()), process.getVersion())
                                                                    : "%d".formatted(process.getVersion())))
                                                    .toList()));
                                });
                    });

            resourcesDeployed.forEach(ModuleAwareBpmnDeployment.bpmnModelCache::remove);

        }

    }

    @EventListener
    @SuppressWarnings("unchecked")
    public void camundaClientCreated(
            final CamundaClientCreatedEvent event) {

        final var existingPriorities = (List<String>) ModuleAwareBpmnDeployment.adapterProperties
                .get(PROPERTY_DEPLOYMENT_PRIORITY);
        if (existingPriorities.isEmpty()) {
            return;
        }
        if (!existingPriorities.get(0).equals(ADAPTER_PACKAGE)) {
            return;
        }
        existingPriorities.remove(0);

        this.client = event.getClient();
        camunda8WorkflowWiring.accept(client);
        camunda8UserTaskWiring.accept(client);

        // deploy only modules listed in configuration, if modules are in classpath not syncing to business cockpit
        deploySelectedWorkflowModules(cockpitProperties.getWorkflowModules().keySet());

        // next adapter
        applicationEventPublisher.publishEvent(event);

    }

    @Override
    public void doDeployment(
            final String workflowModuleId,
            final Resource[] bpmns,
            final Resource[] dmns,
            final Resource[] cmms) throws Exception {

        final var tenantId = camunda8Properties.getTenantId(workflowModuleId);

        // Add all BPMNs to model cache: on one hand to deploy them and on the
        // other hand to wire them to the using project beans according to the SPI
        Arrays
                .stream(bpmns)
                .forEach(resource -> {
                    try (var inputStream = resource.getInputStream()) {

                        final var filename = resource.getFilename();
                        logger.info("About to process '{}' of workflow-module '{}' for business cockpit",
                                filename,
                                workflowModuleId);
                        Optional
                                .ofNullable(ModuleAwareBpmnDeployment.bpmnModelCache.get(filename))
                                .or(() -> {
                                    final var uncachedModel = bpmnParser.parseModelFromStream(inputStream);
                                    final var entry = Map.<String, Object>entry(workflowModuleId, uncachedModel);
                                    ModuleAwareBpmnDeployment.bpmnModelCache.put(filename, entry);
                                    return Optional.of(entry);
                                })
                                .map(Map.Entry::getValue)
                                .ifPresent(model -> processBpmnModel(
                                            workflowModuleId, VERSIONINFO_CURRENT, (BpmnModelInstanceImpl) model, true)
                                );

                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                });

        // BPMNs which were deployed in the past need to be forced to be parsed for wiring
        int currentPage = 0;
        while (currentPage != -1) {
            final var finalPage = currentPage;
            var request = client
                    .newProcessDefinitionSearchRequest()
                    .sort(sort -> sort.name().version())
                    .page(page -> page.from(finalPage).limit(1));
            if (tenantId != null) {
                request = request.filter(filter -> filter.tenantId(tenantId));
            }
            var processDefinitions = request.send().join();
            currentPage = processDefinitions.page().hasMoreTotalItems() ? currentPage + 1 : -1;

            processDefinitions
                    .items()
                    .stream()
                    .map(processDefinition -> Map.entry(processDefinition, client.newProcessDefinitionGetXmlRequest(processDefinition.getProcessDefinitionKey())))
                    .map(bpmnRequest -> Map.entry(bpmnRequest.getKey(), bpmnRequest.getValue().send().join()))
                    .map(bpmn -> Map.entry(bpmn.getKey(), new ByteArrayInputStream(bpmn.getValue().getBytes(StandardCharsets.UTF_8))))
                    .map(bpmnStream -> Map.entry(bpmnStream.getKey(), bpmnParser.parseModelFromStream(bpmnStream.getValue())))
                    .map(bpmnModel -> {
                        final var versionTag = bpmnModel.getKey().getVersionTag();
                        final String versionInfo;
                        if (versionTag != null) {
                            versionInfo = "%s:%d".formatted(versionTag, bpmnModel.getKey().getVersion());
                        } else {
                            versionInfo = "%d".formatted(bpmnModel.getKey().getVersion());
                        }
                        return Map.entry(versionInfo, bpmnModel.getValue());
                    })
                    .forEach(model -> processBpmnModel(
                            workflowModuleId,
                            model.getKey(),
                            model.getValue(),
                            false));
        }

    }

    private void processBpmnModel(
            final String workflowModuleId,
            final String versionInfo,
            final BpmnModelInstanceImpl model,
            final boolean isNewProcess) {

        List<Process> executableProcesses = model
                .getModelElementsByType(Process.class)
                .stream()
                .filter(Process::isExecutable)
                .toList();

        wireWorkflows(workflowModuleId, versionInfo, executableProcesses, isNewProcess);
        wireUserTasks(workflowModuleId, versionInfo, model, executableProcesses, isNewProcess);

    }

    private void wireWorkflows(
            final String workflowModuleId,
            final String versionInfo,
            final List<Process> executableProcesses,
            final boolean isNewProcess) {
        executableProcesses
                .stream()
                .flatMap(process -> getWorkflowConnectables(workflowModuleId, process, versionInfo, isNewProcess))
                .forEach(connectable -> {
                    camunda8WorkflowWiring.wireService(workflowModuleId, connectable);
                    camunda8WorkflowWiring.wireWorkflow(workflowModuleId, connectable);
                });
    }

    @SuppressWarnings("unchecked")
    public Stream<Camunda8WorkflowConnectable> getWorkflowConnectables(
            final String workflowModuleId,
            final Process process,
            final String versionInfo,
            final boolean isNewProcess) {

        final var executionListenerPrefixes = adapterProperties
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(PROPERTY_EXECUTIONLISTENER_PREFIXES))
                .flatMap(entry -> ((Collection<String>) entry.getValue()).stream())
                .toList();

        final var tenantId = camunda8Properties.getTenantId(workflowModuleId);

        final List<Camunda8WorkflowConnectable> result = new LinkedList<>();

        getExistingExecutionListenersJobTypes(executionListenerPrefixes, process)
                .stream()
                .map(jobType ->new Camunda8WorkflowConnectable(
                        workflowModuleId,
                        tenantId,
                        process,
                        versionInfo))
                .forEach(result::add);

        if (isNewProcess) {
            addExecutionListenersToBpmnModel(process);
            result
                    .add(new Camunda8WorkflowConnectable(
                            workflowModuleId,
                            tenantId,
                            process,
                            versionInfo));
        }

        return result.stream();

    }

    /**
     * Order of listeners:
     *
     * <ul>
     *     <li>any custom "start"</li>
     *     <li>Business Cockpit "start": Business Cockpit should reflect all modifications done by previous listeners</li>
     *     <li>any custom listener</li>
     *     <li>Business Cockpit "end": Business Cockpit should reflect all modifications done by previous listeners</li>
     *     <li>Business Cockpit "canceled": Business Cockpit should reflect all modifications done by previous listeners</li>
     * </ul>
     *
     * @param element
     */
    private void addExecutionListenersToBpmnModel(
            final Process element) {

        final var bpmnProcessId = element.getId();

        final ZeebeExecutionListeners executionListeners;
        final boolean isNew;
        if (element.getSingleExtensionElement(ZeebeExecutionListeners.class) != null) {
            executionListeners = element.getSingleExtensionElement(ZeebeExecutionListeners.class);
            isNew = false;
        } else {
            final ExtensionElements extensionElements;
            if (element.getExtensionElements() == null) {
                extensionElements = element.getModelInstance().newInstance(ExtensionElements.class);
                element.addChildElement(extensionElements);
            } else {
                extensionElements = element.getExtensionElements();
            }
            executionListeners = extensionElements.addExtensionElement(ZeebeExecutionListeners.class);
            isNew = true;
        }

        final var startListener = element.getModelInstance().newInstance(ZeebeExecutionListener.class);
        startListener.setEventType(ZeebeExecutionListenerEventType.start);
        startListener.setType(JOBTYPE_DETAILSPROVIDER + bpmnProcessId);
        startListener.setRetries("0");

        if (isNew) {
            executionListeners.insertElementAfter(startListener, null); // insert as first listener
        } else {
            final var previousListeners = new LinkedList<>(executionListeners.getExecutionListeners()
                    .stream()
                    .filter(listener -> listener.getEventType().equals(ZeebeExecutionListenerEventType.start))
                    .toList());
            executionListeners.insertElementAfter(startListener, previousListeners.isEmpty() ? null : previousListeners.getLast());
        }

        final var endListener = element.getModelInstance().newInstance(ZeebeExecutionListener.class);
        endListener.setEventType(ZeebeExecutionListenerEventType.end);
        endListener.setType(JOBTYPE_DETAILSPROVIDER + bpmnProcessId);
        endListener.setRetries("0");

        if (isNew) {
            executionListeners.insertElementAfter(endListener, startListener);
        } else {
            final var previousListener = new LinkedList<>(executionListeners.getExecutionListeners())
                    .getLast();
            executionListeners.insertElementAfter(endListener, previousListener);
        }

    }

    private List<String> getExistingExecutionListenersJobTypes(
            final List<String> executionListenerPrefixes,
            final BaseElement element) {

        return Optional
                .ofNullable(element.getSingleExtensionElement(ZeebeExecutionListeners.class))
                .stream()
                .flatMap(zeebeTaskListeners -> zeebeTaskListeners.getExecutionListeners().stream())
                .map(ZeebeExecutionListener::getType)
                .filter(type -> executionListenerPrefixes
                        .stream()
                        .anyMatch(type::startsWith))
                .toList();

    }

    private void wireUserTasks(
            final String workflowModuleId,
            final String versionInfo,
            final BpmnModelInstanceImpl model,
            final List<Process> executableProcesses,
            final boolean isNewProcess) {
        executableProcesses
                .stream()
                .flatMap(process -> getUserTaskConnectables(workflowModuleId, process, versionInfo, model, isNewProcess))
                .forEach(connectable ->
                        camunda8UserTaskWiring.wireTask(workflowModuleId, connectable));
    }

    @SuppressWarnings("unchecked")
    public Stream<Camunda8UserTaskConnectable> getUserTaskConnectables(
            final String workflowModuleId,
            final Process process,
            final String versionInfo,
            final BpmnModelInstanceImpl model,
            final boolean isNewProcess) {

        final var taskListenerPrefixes = adapterProperties
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(PROPERTY_TASKLISTENER_PREFIXES))
                .flatMap(entry -> ((Collection<String>) entry.getValue()).stream())
                .toList();

        final var tenantId = camunda8Properties.getTenantId(workflowModuleId);
        return model
                .getModelElementsByType(UserTask.class)
                .stream()
                .filter(element -> Objects.equals(getOwningProcess(element), process))
                .filter(element -> element.getSingleExtensionElement(ZeebeUserTask.class) != null) // only Camunda user tasks
                .flatMap(element -> {
                    final List<Camunda8UserTaskConnectable> result = new LinkedList<>();
                    final var externalFormReference = getTaskDefinition(element);
                    getExistingTaskListenersJobTypes(taskListenerPrefixes, element)
                            .stream()
                            .map(jobType ->new Camunda8UserTaskConnectable(
                                    workflowModuleId,
                                    tenantId,
                                    process,
                                    versionInfo,
                                    element.getId(),
                                    jobType.startsWith(JOBTYPE_DETAILSPROVIDER) ? externalFormReference : jobType,
                                    getTaskName(element)))
                            .forEach(result::add);
                    if (isNewProcess && StringUtils.hasText(externalFormReference)) {
                        addTaskListenersToBpmnModel(externalFormReference, element);
                        result
                                .add(new Camunda8UserTaskConnectable(
                                        workflowModuleId,
                                        tenantId,
                                        process,
                                        versionInfo,
                                        element.getId(),
                                        externalFormReference,
                                        getTaskName(element)));
                    }
                    return result.stream();
                });

    }

    /**
     * Order of listeners:
     *
     * <ul>
     *     <li>VanillaBP "creating"</li>
     *     <li>any custom "creating"</li>
     *     <li>Business Cockpit "creating": Business Cockpit should reflect all modifications done by previous listeners</li>
     *     <li>any custom listener other than "creating"</li>
     *     <li>VanillaBP "canceling"</li>
     *     <li>Business Cockpit "canceling": Business Cockpit should reflect all modifications done by previous listeners</li>
     *     <li>Business Cockpit "completing": Business Cockpit should reflect all modifications done by previous listeners</li>
     * </ul>
     *
     * @param externalFormReference
     * @param element
     */
    private void addTaskListenersToBpmnModel(
            final String externalFormReference,
            final BaseElement element) {

        final ZeebeTaskListeners taskListeners;
        final boolean isNew;
        if (element.getSingleExtensionElement(ZeebeTaskListeners.class) != null) {
            taskListeners = element.getSingleExtensionElement(ZeebeTaskListeners.class);
            isNew = false;
        } else {
            taskListeners = element.getExtensionElements().addExtensionElement(ZeebeTaskListeners.class);
            isNew = true;
        }

        final var createListener = element.getModelInstance().newInstance(ZeebeTaskListener.class);
        createListener.setEventType(ZeebeTaskListenerEventType.creating);
        createListener.setType(JOBTYPE_DETAILSPROVIDER + externalFormReference);
        createListener.setRetries("0");

        if (isNew) {
            taskListeners.insertElementAfter(createListener, null); // insert as first listener
        } else {
            final var previousListeners = new LinkedList<>(taskListeners.getTaskListeners()
                    .stream()
                    .filter(listener -> listener.getEventType().equals(ZeebeTaskListenerEventType.creating))
                    .toList());
            taskListeners.insertElementAfter(createListener, previousListeners.isEmpty() ? null : previousListeners.getLast());
        }

        final var cancelListener = element.getModelInstance().newInstance(ZeebeTaskListener.class);
        cancelListener.setEventType(ZeebeTaskListenerEventType.canceling);
        cancelListener.setType(JOBTYPE_DETAILSPROVIDER + externalFormReference);
        cancelListener.setRetries("0");

        if (isNew) {
            taskListeners.insertElementAfter(cancelListener, createListener);
        } else {
            final var previousListener = new LinkedList<>(taskListeners.getTaskListeners())
                    .getLast();
            taskListeners.insertElementAfter(cancelListener, previousListener);
        }

        final var completeListener = element.getModelInstance().newInstance(ZeebeTaskListener.class);
        completeListener.setEventType(ZeebeTaskListenerEventType.completing);
        completeListener.setType(JOBTYPE_DETAILSPROVIDER + externalFormReference);
        completeListener.setRetries("0");

        if (isNew) {
            taskListeners.insertElementAfter(completeListener, createListener);
        } else {
            final var previousListener = new LinkedList<>(taskListeners.getTaskListeners())
                    .getLast();
            taskListeners.insertElementAfter(completeListener, previousListener);
        }

    }

    private List<String> getExistingTaskListenersJobTypes(
            final List<String> taskListenerPrefixes,
            final BaseElement element) {

        return Optional
                .ofNullable(element.getSingleExtensionElement(ZeebeTaskListeners.class))
                .stream()
                .flatMap(zeebeTaskListeners -> zeebeTaskListeners.getTaskListeners().stream())
                .map(ZeebeTaskListener::getType)
                .filter(type -> taskListenerPrefixes
                        .stream()
                        .anyMatch(type::startsWith))
                .toList();

    }

    private static Process getOwningProcess(
            final ModelElementInstance element) {

        if (element instanceof Process) {
            return (Process) element;
        }

        ModelElementInstance parent = element.getParentElement();
        return parent == null ? null : getOwningProcess(parent);
    }

    private String getTaskDefinition(final BaseElement element) {
        return Optional
                .ofNullable(element.getSingleExtensionElement(ZeebeFormDefinition.class))
                .map(formDefinition -> formDefinition.getFormKey() != null
                        ? formDefinition.getFormKey()
                        : formDefinition.getExternalReference())
                .orElse(null);
    }

    private String getTaskName(
            final BaseElement element
    ){
        return element.getAttributeValue("name");
    }
}
