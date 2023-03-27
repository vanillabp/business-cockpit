package io.vanillabp.cockpit.simulator.testdata.usertask;

import com.devskiller.jfairy.Fairy;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
                        .map(l -> Map.entry(l, buildFairy(l.trim())))
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
        
        final var noOfTasks = parameters.getNoOfTasks() / parameters.getNoOfConcurrentRequest();
        var remainingNoOfTasks = parameters.getNoOfTasks();
        for (int i = 1; i <= parameters.getNoOfConcurrentRequest(); ++i) {
            final var generator = new UserTaskTestDataGenerator(
                    (i - 1) * noOfTasks,
                    i != parameters.getNoOfConcurrentRequest()
                            ? noOfTasks
                            : remainingNoOfTasks,
                    bpmsApiProvider.getObject(),
                    users,
                    groups,
                    fairies,
                    parameters);
            remainingNoOfTasks -= noOfTasks;
            final var thread = new Thread(generator);
            thread.start();
        }
                
        return "testdata/usertask/report";
        
    }
    
    private Fairy buildFairy(
            final String language) {
        
        return Fairy.builder()
                .withLocale(Locale.forLanguageTag(language))
                .withRandomSeed((int) System.currentTimeMillis())
                .build();
        
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
