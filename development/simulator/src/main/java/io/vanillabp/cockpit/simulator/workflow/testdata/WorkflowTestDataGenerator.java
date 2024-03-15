package io.vanillabp.cockpit.simulator.workflow.testdata;

import com.devskiller.jfairy.Fairy;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.simulator.usertask.testdata.TestData1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorkflowTestDataGenerator implements Runnable {

    public static volatile boolean shutdown = false;

    private Random random;

    private int noOfEvents;

    private BpmsApi bpmsApi;

    private Map<String, Fairy> fairies;

    private WorkflowTestDataParameters parameters;

    private List<WorkflowCreatedOrUpdatedEvent> created = new LinkedList<>();

    private Logger logger;

    public WorkflowTestDataGenerator(
            final int offset,
            final int noOfEvents,
            final BpmsApi bpmsApi,
            final Map<String, Fairy> fairies,
            final WorkflowTestDataParameters parameters) {
        this.logger = LoggerFactory.getLogger(
                WorkflowTestDataGenerator.class.getSimpleName()
                + "#"
                + offset);
        this.noOfEvents = noOfEvents;
        this.random = new Random(System.currentTimeMillis() * offset);
        this.bpmsApi = bpmsApi;
        this.fairies = fairies;
        this.parameters = parameters;
    }

    @Override
    public void run() {
        var current = 0;

        bpmsApi.registerWorkflowModule(
                "TestModule",
                new RegisterWorkflowModuleEvent()
                        .uri("http://localhost:8079/TestModule")
                        .taskProviderApiUriPath("/task-provider/v1")
                        .workflowProviderApiUriPath("/workflow-details-provider/v1"));

        while (!shutdown) {
            ++current;
            final var onlyUpdatesLeft =
                    (created.size() * 100 / noOfEvents) > (100 - parameters.getPercentageUpdates());
            final var updateWanted = created.size() > 0
                    && random.nextInt(100) < parameters.getPercentageUpdates();
            if (onlyUpdatesLeft || updateWanted) {
                final var createdEvent = created.get(
                        random.nextInt(created.size()));
                final var doUpdate = random.nextBoolean();
                if (doUpdate) {
                    final var updatedEvent = buildUpdatedEvent(createdEvent);
                    bpmsApi.workflowUpdatedEvent(
                            createdEvent.getWorkflowId(), updatedEvent);
                } else {
                    final var typeOfEvent = random.nextInt(2);
                    if (typeOfEvent == 0) {
                        bpmsApi.workflowCompletedEvent(
                                createdEvent.getWorkflowId(),
                                buildCompletedEvent(createdEvent));
                    } else {
                        bpmsApi.workflowCancelledEvent(
                                createdEvent.getWorkflowId(),
                                buildCancelledEvent(createdEvent));
                    }
                }
            } else {
                final var event = buildCreatedEvent();
                if (event == null) {
                    return;
                }
                created.add(event);
                bpmsApi.workflowCreatedEvent(event);
                
            }
            
            if (current % 100 == 0) {
                logger.info("Progress: {}%", (current * 100 / noOfEvents));
            }
            
            if (current == noOfEvents) {
                logger.info("Created {} tasks", created.size());
                return;
            }
            
            try {
                Thread.sleep(parameters.getThrottling());
            } catch (InterruptedException e) {
                return;
            }
            
        }
        
    }
    
    private static String getProcessTitle(
            final String language,
            final int process) {
        
        if ("de".equals(language)) {
            
            return switch (process) {
                case 0 -> "Taxifahrt";
                case 1 -> "Bezahlung";
                case 2 -> "Fahrzeug laden";
                default -> "Test"; 
                };
            
        }

        return switch (process) {
            case 0 -> "Taxi ride";
            case 1 -> "Payment";
            case 2 -> "Charge car";
            default -> "Test";
            };

    }
    
    private static String getBpmnProcessId(
            final int process) {
        
        return switch (process) {
                case 0 -> "TaxiRide";
                case 1 -> "Payment";
                case 2 -> "ChargeCar";
                default -> "Test"; 
                };
        
    }

    private WorkflowCreatedOrUpdatedEvent buildUpdatedEvent(
            final WorkflowCreatedOrUpdatedEvent createdEvent) {
        
        final var result = createdEvent;
        result.setId(UUID.randomUUID().toString());
        result.setWorkflowId(createdEvent.getWorkflowId());
        result.setTimestamp(OffsetDateTime.now());
        result.setUpdated(Boolean.TRUE);
        
        result.setTitle(
                fairies
                        .entrySet()
                        .stream()
                        .map(entry -> Map.entry(
                                entry.getKey(),
                                "Workflow " + entry.getValue().textProducer().sentence(4)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        return result;
        
    }

    private WorkflowCancelledEvent buildCancelledEvent(
            final WorkflowCreatedOrUpdatedEvent createdEvent) {
        
        final var result = new WorkflowCancelledEvent();
        
        result.setId(UUID.randomUUID().toString());
        result.setWorkflowId(createdEvent.getWorkflowId());
        result.setComment(fairies.values().iterator().next().textProducer().word(3));
        result.setTimestamp(OffsetDateTime.now());
        
        return result;
        
    }


    private WorkflowCompletedEvent buildCompletedEvent(
            final WorkflowCreatedOrUpdatedEvent createdEvent) {
        
        final var result = new WorkflowCompletedEvent();
        
        result.setId(UUID.randomUUID().toString());
        result.setWorkflowId(createdEvent.getWorkflowId());
        result.setComment(fairies.values().iterator().next().textProducer().word(3));
        result.setTimestamp(OffsetDateTime.now());
        
        return result;
        
    }

    private WorkflowCreatedOrUpdatedEvent buildCreatedEvent() {
        return buildCreatedEvent(
                random,
                fairies);
        
    }
    public static WorkflowCreatedOrUpdatedEvent buildCreatedEvent(
            final Random random,
            final Map<String, Fairy> fairies) {
        
        final var result = new WorkflowCreatedOrUpdatedEvent();
        result.setUpdated(Boolean.FALSE);
        final var process = random.nextInt(10);
        result.setId(UUID.randomUUID().toString());
        result.setWorkflowId(UUID.randomUUID().toString());
        result.setBusinessId(UUID.randomUUID().toString());
        result.setBpmnProcessId(getBpmnProcessId(process));
        result.setTitle(
                fairies
                        .keySet()
                        .stream()
                        .map(language -> Map.entry(language, getProcessTitle(language, process)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        result.setBpmnProcessVersion("0");

        result.setTimestamp(OffsetDateTime.now());
        result.setWorkflowModuleId("TestModule");
        result.setUiUriPath("/remoteEntry.js");
        result.setUiUriType(UiUriType.WEBPACK_MF_REACT);

        final var testData1 = new TestData1();
        testData1.setTestId1(Integer.toString(random.nextInt(5)));
        testData1.setTestId2(random.nextInt(10000));
        result.setDetails(
                Map.of("test1", testData1));

        return result;
    }
    
    public static Fairy buildFairy(
            final String language) {
        
        return Fairy.builder()
                .withLocale(Locale.forLanguageTag(language))
                .withRandomSeed((int) System.currentTimeMillis())
                .build();
        
    }
    
}
