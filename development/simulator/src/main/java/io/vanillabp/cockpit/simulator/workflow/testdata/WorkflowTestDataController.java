package io.vanillabp.cockpit.simulator.workflow.testdata;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.vanillabp.cockpit.simulator.common.FairyHelper;
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

import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;

@Controller
@RequestMapping(path = "/testdata/workflow")
public class WorkflowTestDataController {

    @Autowired
    private ObjectProvider<BpmsApi> bpmsApiProvider;

    @GetMapping(path = "/form")
    public String form() {

        return "testdata/workflow/form";

    }

    @PostMapping(
            path = "/generate",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String generate(
            WorkflowTestDataParameters parameters,
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

        final var noOfEvents = parameters.getNoOfEvents() / parameters.getNoOfConcurrentRequest();
        var remainingNoOfTasks = parameters.getNoOfEvents();
        for (int i = 1; i <= parameters.getNoOfConcurrentRequest(); ++i) {
            final var generator = new WorkflowTestDataGenerator(
                    (i - 1) * noOfEvents,
                    i != parameters.getNoOfConcurrentRequest()
                            ? noOfEvents
                            : remainingNoOfTasks,
                    bpmsApiProvider.getObject(),
                    fairies,
                    parameters);
            remainingNoOfTasks -= noOfEvents;
            final var thread = new Thread(generator);
            thread.start();
        }

        return "testdata/workflow/report";
    }

    @PostConstruct
    public void trackShutdown() {
        Runtime
                .getRuntime()
                .addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        WorkflowTestDataGenerator.shutdown = true;
                    }
                }));
    }

}
