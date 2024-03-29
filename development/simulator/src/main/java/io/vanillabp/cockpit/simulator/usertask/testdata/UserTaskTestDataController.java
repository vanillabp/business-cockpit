package io.vanillabp.cockpit.simulator.usertask.testdata;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.vanillabp.cockpit.simulator.common.FairyHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import jakarta.annotation.PostConstruct;

@Controller
@RequestMapping(path = "/testdata/usertask")
public class UserTaskTestDataController {
    
    @Autowired
    private ObjectProvider<BpmsApi> bpmsApiProvider;
    
    @GetMapping(path = "/form")
    public String form() {
        
        return "testdata/usertask/form";
        
    }
    
    @PostMapping(
            path = "/generate",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String generate(
            UserTaskTestDataParameters parameters,
            @ModelAttribute("model") ModelMap model) {
        
        final var fairies =
                Arrays
                        .stream(parameters.getLanguages().split(","))
                        .map(l -> Map.entry(l, FairyHelper.buildFairy(l.trim())))
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> e.getValue(),
                                (key1, key2) -> key1,
                                LinkedHashMap::new));
        
        final var users = new String[parameters.getNoOfUsers()];
        for (int i = 0; i < users.length; ++i) {
            users[i] = fairies
                    .values()
                    .iterator()
                    .next()
                    .person()
                    .getUsername();
        }
        
        final var groups = new String[parameters.getNoOfGroups()];
        for (int i = 0; i < groups.length; ++i) {
            final var domain = fairies
                    .values()
                    .iterator()
                    .next()
                    .company()
                    .getDomain();
            groups[i] = domain.substring(0, domain.indexOf('.'));
        }
        
        final var noOfEvents = parameters.getNoOfEvents() / parameters.getNoOfConcurrentRequest();
        var remainingNoOfTasks = parameters.getNoOfEvents();
        for (int i = 1; i <= parameters.getNoOfConcurrentRequest(); ++i) {
            final var generator = new UserTaskTestDataGenerator(
                    (i - 1) * noOfEvents,
                    i != parameters.getNoOfConcurrentRequest()
                            ? noOfEvents
                            : remainingNoOfTasks,
                    bpmsApiProvider.getObject(),
                    users,
                    groups,
                    fairies,
                    parameters);
            remainingNoOfTasks -= noOfEvents;
            final var thread = new Thread(generator);
            thread.start();
        }
                
        return "testdata/usertask/report";
        
    }
    
    @PostConstruct
    public void trackShutdown() {
        
        Runtime
                .getRuntime()
                .addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        UserTaskTestDataGenerator.shutdown = true;
                    }
                }));
        
    }
    
}
