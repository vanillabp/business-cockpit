package io.vanillabp.cockpit.simulator.workflow.testdata;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.cockpit.simulator.common.FairyHelper;
import jakarta.annotation.PostConstruct;

/**
 * The web page which generates test workflows. A form takes the parameters, and the generation
 * sends the workflows to the Business Cockpit in the background.
 */
@Controller
@RequestMapping(path = "/testdata/workflow")
public class WorkflowTestDataController {

  @Autowired
  private BpmsApi bpmsApiProvider;

  /**
   * Creates the controller. Spring creates it, nobody else does.
   */
  public WorkflowTestDataController() {
  }

  /**
   * Shows the form for the parameters of the generation.
   *
   * @return The name of the form's template
   */
  @GetMapping(path = "/form")
  public String form() {

    return "testdata/workflow/form";

  }

  /**
   * Starts the generation. The events are split among as many threads as the parameters ask
   * for concurrent requests. The method does not wait for the threads.
   *
   * @param parameters The parameters from the form
   * @param model The model of the page
   * @return The name of the report's template
   */
  @PostMapping(
      path = "/generate",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String generate(
      WorkflowTestDataParameters parameters,
      @ModelAttribute("model") ModelMap model) {

    final var fairies = Arrays
        .stream(parameters.getLanguages().split(","))
        .map(l -> Map.entry(l, FairyHelper.buildFairy(l.trim())))
        .collect(Collectors.toMap(
            e -> e.getKey(),
            e -> e.getValue(),
            (
                key1,
                key2) -> key1,
            LinkedHashMap::new));

    final var noOfEvents = parameters.getNoOfEvents() / parameters.getNoOfConcurrentRequest();
    var remainingNoOfTasks = parameters.getNoOfEvents();
    for (int i = 1; i <= parameters.getNoOfConcurrentRequest(); ++i) {
      final var generator = new WorkflowTestDataGenerator(
          (i - 1) * noOfEvents, i != parameters.getNoOfConcurrentRequest()
              ? noOfEvents
              : remainingNoOfTasks, bpmsApiProvider, fairies, parameters);
      remainingNoOfTasks -= noOfEvents;
      final var thread = new Thread(generator);
      thread.start();
    }

    return "testdata/workflow/report";
  }

  /**
   * Stops all running generators when the application shuts down.
   */
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
