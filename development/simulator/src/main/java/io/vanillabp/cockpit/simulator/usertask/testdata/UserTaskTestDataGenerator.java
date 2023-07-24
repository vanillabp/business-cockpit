package io.vanillabp.cockpit.simulator.usertask.testdata;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vanillabp.cockpit.simulator.common.FairyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devskiller.jfairy.Fairy;

import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.DetailProperties;
import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskActivatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskSuspendedEvent;

public class UserTaskTestDataGenerator implements Runnable {

    public static volatile boolean shutdown = false;
    
    private Random random;
    
    private int noOfEvents;
    
    private BpmsApi bpmsApi;
    
    private String[] users;
    
    private String[] groups;
    
    private Map<String, Fairy> fairies;
    
    private UserTaskTestDataParameters parameters;
    
    private List<UserTaskCreatedOrUpdatedEvent> created = new LinkedList<>();

    private Logger logger;
    
    public UserTaskTestDataGenerator(
            final int offset,
            final int noOfEvents,
            final BpmsApi bpmsApi,
            final String[] users,
            final String[] groups,
            final Map<String, Fairy> fairies,
            final UserTaskTestDataParameters parameters) {

        
        this.logger = LoggerFactory.getLogger(
                UserTaskTestDataGenerator.class.getSimpleName()
                + "#"
                + offset);
        this.noOfEvents = noOfEvents;
        this.random = new Random(System.currentTimeMillis() * offset);
        this.bpmsApi = bpmsApi;
        this.users = users;
        this.groups = groups;
        this.fairies = fairies;
        this.parameters = parameters;
        
    }

    @Override
    public void run() {
        
        var current = 0;
        
        while (!shutdown) {
            
            ++current;
            
            final var onlyUpdatesLeft =
                    (created.size() * 100 / noOfEvents) > (100 - parameters.getPercentageUpdates());
            final var updateWanted = created.size() > 0
                    && random.nextInt(100) < parameters.getPercentageUpdates();
            if (onlyUpdatesLeft || updateWanted) {
                
                final var createdEvent = created.get(
                        random.nextInt(created.size()));
                
                final var doUpdate = true; // random.nextBoolean();
                if (doUpdate) {
                    
                    final var updatedEvent = buildUpdatedEvent(createdEvent);
                    bpmsApi.userTaskUpdatedEvent(
                            createdEvent.getUserTaskId(), updatedEvent);
                    
                } else {
                    
                    final var typeOfEvent = random.nextInt();
                    switch (typeOfEvent) {
                    case 0:
                            bpmsApi.userTaskCompletedEvent(
                                    createdEvent.getUserTaskId(),
                                    buildCompletedEvent(createdEvent));
                            break;
                    case 1:
                            bpmsApi.userTaskCancelledEvent(
                                    createdEvent.getUserTaskId(),
                                    buildCancelledEvent(createdEvent));
                            break;
                    case 2:
                            bpmsApi.userTaskSuspendedEvent(
                                    createdEvent.getUserTaskId(),
                                    buildSuspendedEvent(createdEvent));
                            break;
                    default:
                            bpmsApi.userTaskActivatedEvent(
                                    createdEvent.getUserTaskId(),
                                    buildActivatedEvent(createdEvent));
                    }
                    
                }
                
            } else {
                
                final var event = buildCreatedEvent();
                if (event == null) {
                    return;
                }
                created.add(event);
                bpmsApi.userTaskCreatedEvent(event);
                
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

    private UserTaskCreatedOrUpdatedEvent buildUpdatedEvent(
            final UserTaskCreatedOrUpdatedEvent createdEvent) {
        
        final var result = createdEvent;
        result.setId(UUID.randomUUID().toString());
        result.setTimestamp(OffsetDateTime.now());
        result.setUpdated(Boolean.TRUE);
        
        result.setTitle(
                fairies
                        .entrySet()
                        .stream()
                        .map(entry -> Map.entry(
                                entry.getKey(),
                                entry.getValue().textProducer().sentence(5)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        return result;
        
    }

    private UserTaskCancelledEvent buildCancelledEvent(
            final UserTaskCreatedOrUpdatedEvent createdEvent) {
        
        final var result = new UserTaskCancelledEvent();
        
        result.setId(UUID.randomUUID().toString());
        result.setComment(fairies.values().iterator().next().textProducer().word(3));
        result.setTimestamp(OffsetDateTime.now());
        
        return result;
        
    }


    private UserTaskCompletedEvent buildCompletedEvent(
            final UserTaskCreatedOrUpdatedEvent createdEvent) {
        
        final var result = new UserTaskCompletedEvent();
        
        result.setId(UUID.randomUUID().toString());
        result.setComment(fairies.values().iterator().next().textProducer().word(3));
        result.setTimestamp(OffsetDateTime.now());
        
        return result;
        
    }


    private UserTaskActivatedEvent buildActivatedEvent(
            final UserTaskCreatedOrUpdatedEvent createdEvent) {
        
        final var result = new UserTaskActivatedEvent();
        
        result.setId(UUID.randomUUID().toString());
        result.setComment(fairies.values().iterator().next().textProducer().word(3));
        result.setTimestamp(OffsetDateTime.now());
        
        return result;
        
    }


    private UserTaskSuspendedEvent buildSuspendedEvent(
            final UserTaskCreatedOrUpdatedEvent createdEvent) {
        
        final var result = new UserTaskSuspendedEvent();
        
        result.setId(UUID.randomUUID().toString());
        result.setComment(fairies.values().iterator().next().textProducer().word(3));
        result.setTimestamp(OffsetDateTime.now());
        
        return result;
        
    }

    private UserTaskCreatedOrUpdatedEvent buildCreatedEvent() {
        
        final String assignee;
        if (random.nextInt(100) < parameters.getPercentageUserAssignments()) {
            assignee = users[random.nextInt(users.length)];
        } else {
            assignee = null;
        }

        final List<String> candidateUsers;
        if ((assignee != null)
                && (random.nextInt(100) < parameters.getPercentageUserCandidates())) {
            candidateUsers = List.of(assignee);
        } else {
            candidateUsers = null;
        }

        final List<String> candidateGroups;
        if (random.nextInt(100) < parameters.getPercentageGroupCandidates()) {
            if (random.nextBoolean() || (groups.length < 2)) {
                candidateGroups = List.of(
                        groups[random.nextInt(groups.length)]);
            } else {
                final var firstGroup = groups[random.nextInt(groups.length)];
                String secondGroup;
                while ((secondGroup = groups[random.nextInt(groups.length)]).equals(firstGroup)) { }
                candidateGroups = List.of(firstGroup, secondGroup);
            }
        } else {
            candidateGroups = null;
        }
        
        return buildCreatedEvent(
                random,
                fairies,
                assignee,
                candidateUsers,
                candidateGroups);
        
    }
    public static UserTaskCreatedOrUpdatedEvent buildCreatedEvent(
            final Random random,
            final Map<String, Fairy> fairies,
            final String assignee,
            final List<String> candidateUsers,
            final List<String> candidateGroups) {
        
        final var result = new UserTaskCreatedOrUpdatedEvent();
        result.setUpdated(Boolean.FALSE);
        
        final var process = random.nextInt(10);
        
        result.setId(UUID.randomUUID().toString());
        result.setUserTaskId(UUID.randomUUID().toString());
        result.setBusinessId(UUID.randomUUID().toString());
        result.setBpmnProcessId(getBpmnProcessId(process));
        result.setWorkflowTitle(
                fairies
                        .keySet()
                        .stream()
                        .map(language -> Map.entry(language, getProcessTitle(language, process)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        
        result.setBpmnProcessVersion("0");
        final var formDef = random.nextInt(4);
        result.setTaskDefinition(
                switch (formDef) {
                case 0 -> "TestForm1";
                case 1 -> "TestForm2";
                case 2 -> "TestForm3";
                default -> "TestForm4"; 
                });
        result.setBpmnTaskId(
                result.getTaskDefinition());
        result.setTimestamp(OffsetDateTime.now());
        
        result.setWorkflowModule("TestModule");
        result.setWorkflowModuleUri("http://localhost:8079/wm/TestModule");
        result.setUiUriPath("/remoteEntry.js");
        result.setUiUriType(UiUriType.WEBPACK_MF_REACT);
        result.setTaskProviderApiUriPath("/task-provider/v1");
        
        result.setTitle(
                fairies
                        .entrySet()
                        .stream()
                        .map(entry -> Map.entry(
                                entry.getKey(),
                                entry.getValue().textProducer().sentence(5)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        
        result.setAssignee(assignee);
        result.setCandidateUsers(candidateUsers);
        result.setCandidateGroups(candidateGroups);

        if (formDef % 2 == 0) {
            result.setDueDate(OffsetDateTime.now().plusHours(
                    random.nextInt(48) - 6));
        }

        final var projectData = new ProjectData();
        final var textProducer = FairyHelper.buildFairy("de").textProducer();
        projectData.setProjectPk(textProducer.randomString(12));
        projectData.setName(textProducer.word(3));

        if (formDef % 2 == 0) {
            final var testData1 = new TestData1();
            testData1.setTestId1(Integer.toString(random.nextInt(5)));
            testData1.setTestId2(random.nextInt(10000));
            result.setDetails(
                    Map.of("test1", testData1,
                            "project", projectData));
            result.setDetailsProperties(List.of(
                    new DetailProperties().path("test1.testId1").filterable(true).sortable(true),
                    new DetailProperties().path("test1.testId2").filterable(false).sortable(false),
                    new DetailProperties().path("project.name").filterable(true).sortable(true)));
        } else {
            final var testData1 = new TestData1();
            testData1.setTestId1(Integer.toString(random.nextInt(5)));
            testData1.setTestId2(random.nextInt(10000));
            final var testData2 = new TestData2();
            testData2.setTestId3(Integer.toString(random.nextInt(5)));
            testData2.setTestId2(random.nextInt(10000));
            result.setDetails(
                    Map.of("test1", testData1,
                            "test2", testData2,
                            "project", projectData));
            result.setDetailsProperties(List.of(
                    new DetailProperties().path("test1.testId1").filterable(true).sortable(true),
                    new DetailProperties().path("test2.testId2").filterable(false).sortable(false),
                    new DetailProperties().path("test2.testId3").filterable(true).sortable(true),
                    new DetailProperties().path("project.name").filterable(true).sortable(true)));
        }
                
        return result;
        
    }

}
