package io.vanillabp.cockpit.simulator.testdata.usertask;

import com.devskiller.jfairy.Fairy;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserTaskTestDataGenerator implements Runnable {

    public static volatile boolean shutdown = false;
    
    private Random random;
    
    private int noOfTasks;
    
    private BpmsApi bpmsApi;
    
    private String[] users;
    
    private String[] groups;
    
    private Map<String, Fairy> fairies;
    
    private UserTaskTestDataParameters parameters;
    
    private List<UserTaskCreatedEvent> updates = new LinkedList<>();

    private Logger logger;
    
    public UserTaskTestDataGenerator(
            final int offset,
            final int noOfTasks,
            final BpmsApi bpmsApi,
            final String[] users,
            final String[] groups,
            final Map<String, Fairy> fairies,
            final UserTaskTestDataParameters parameters) {

        
        this.logger = LoggerFactory.getLogger(
                UserTaskTestDataGenerator.class.getSimpleName()
                + "#"
                + offset);
        this.noOfTasks = noOfTasks;
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
            
            final var hasUpdates = !updates.isEmpty();
            final var onlyUpdatesLeft = hasUpdates
                    && current + updates.size() == noOfTasks;
            final var doUpdate = !updates.isEmpty()
                    && random.nextInt(10) == 0;
            if (onlyUpdatesLeft || doUpdate) {
                
                final var createdEvent = updates.remove(0);
                
            } else {
                
                final var event = buildCreatedEvent();
                if (event == null) {
                    return;
                }
                bpmsApi.userTaskCreatedEvent(event);
                
            }
            
            if (current % 100 == 0) {
                logger.info("Progress: {}%", (current * 100 / noOfTasks));
            }
            
            if (current == noOfTasks) {
                return;
            }
            
            try {
                Thread.sleep(parameters.getThrottling());
            } catch (InterruptedException e) {
                return;
            }
            
        }
        
    }
    
    private String getProcessTitle(
            final String language,
            final int process) {
        
        if ("de".equals(language)) {
            
            return switch (process) {
                case 0 -> "Taxifahrt";
                case 1 -> "Bezahlung";
                case 2 -> "Fahrzeug laden";
                default -> "Demo"; 
                };
            
        }

        return switch (process) {
            case 0 -> "Taxi ride";
            case 1 -> "Payment";
            case 2 -> "Charge car";
            default -> "Demo";
            };

    }
    
    private String getBpmnProcessId(
            final int process) {
        
        return switch (process) {
                case 0 -> "TaxiRide";
                case 1 -> "Payment";
                case 2 -> "ChargeCar";
                default -> "Demo"; 
                };
        
    }
    
    private UserTaskCreatedEvent buildCreatedEvent() {
        
        final var result = new UserTaskCreatedEvent();
        
        final var process = random.nextInt(4);
        
        result.setId(UUID.randomUUID().toString());
        result.setBpmnWorkflowId(UUID.randomUUID().toString());
        result.setBpmnProcessId(getBpmnProcessId(process));
        result.setWorkflowTitle(
                fairies
                        .keySet()
                        .stream()
                        .map(language -> Map.entry(language, getProcessTitle(language, process)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        
        result.setBpmnProcessVersion("0");
        result.setTaskDefinition(
                switch (random.nextInt(4)) {
                case 0 -> "Do1";
                case 1 -> "Do2";
                case 2 -> "Do3";
                default -> "Do4"; 
                });
        result.setWorkflowTaskId(
                result.getTaskDefinition());
        result.setTimestamp(OffsetDateTime.now());
        
        result.setTitle(
                fairies
                        .entrySet()
                        .stream()
                        .map(entry -> Map.entry(
                                entry.getKey(),
                                entry.getValue().textProducer().sentence(5)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        
        if (random.nextInt(100) < parameters.getPercentageUserAssignments()) {
            result.setAssignee(users[random.nextInt(users.length)]);
        }
        
        if (random.nextInt(100) < parameters.getPercentageUserCandidates()) {
            if (result.getAssignee() != null) {
                result.setCandidateUsers(List.of(result.getAssignee()));
            }
        }
        
        if (random.nextInt(100) < parameters.getPercentageGroupCandidates()) {
            if (random.nextBoolean() || (groups.length < 2)) {
                result.setCandidateGroups(List.of(
                        groups[random.nextInt(groups.length)]));
            } else {
                final var firstGroup = groups[random.nextInt(groups.length)];
                String secondGroup;
                while ((secondGroup = groups[random.nextInt(groups.length)]).equals(firstGroup)) { }
                result.setCandidateGroups(List.of(firstGroup, secondGroup));
            }
        }
                
        return result;
        
    }
    
    
}
