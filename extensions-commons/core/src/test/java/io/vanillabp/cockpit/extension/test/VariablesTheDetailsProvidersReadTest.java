package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;

/**
 * <code>variablesTheDetailsProvidersRead</code> passes the question on to VanillaBP. VanillaBP
 * knows each provider together with its workflow module and BPMN process, so the extension keeps
 * no list of its own.
 */
@ExtendWith(SuppressOutputExtension.class)
public class VariablesTheDetailsProvidersReadTest {

  /** What the extension asked VanillaBP, one entry per question. */
  private final List<List<Object>> questions = new ArrayList<>();

  @Test
  @DisplayName("The question goes to VanillaBP with module, process and the keys of the task, element id first")
  public void theQuestionGoesToVanillaBp() {

    final var extension = new Extension(true);

    final var names = extension.variablesTheDetailsProvidersRead("module", "Main", "approve", "Activity_approve");

    assertEquals(List.of("amount", "customer"), names);
    assertEquals(
        List
            .of(
                List
                    .of(
                        UserTaskDetailsProvider.class, "module", "Main",
                        List.of("Activity_approve", "approve"))),
        questions);

  }

  @Test
  @DisplayName("A task without a task definition is asked for by its element id alone")
  public void aTaskWithoutATaskDefinition() {

    new Extension(true).variablesTheDetailsProvidersRead("module", "Other", null, "Activity_sign");

    assertEquals(List.of("Activity_sign"), questions.getFirst().get(3));
    assertEquals("Other", questions.getFirst().get(2));

  }

  @Test
  @DisplayName("Where no user task is reported, no provider is ever called, and VanillaBP is not asked")
  public void noUserTasksNoQuestion() {

    final var names = new Extension(false)
        .variablesTheDetailsProvidersRead("module", "Main", "approve", "Activity_approve");

    assertEquals(List.of(), names);
    assertEquals(List.of(), questions);

  }

  /**
   * VanillaBP as far as this question goes: it notes what it was asked and answers two names.
   * Every other method of the interface is not used here.
   */
  private ExtensionHandlers handlers() {

    return (ExtensionHandlers) Proxy
        .newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{
                ExtensionHandlers.class
            },
            (
                proxy,
                method,
                arguments) -> {
              if (!method.getName().equals("taskParameterNames")) {
                throw new UnsupportedOperationException(method.getName());
              }
              questions.add(List.of(arguments));
              return List.of("amount", "customer");
            });

  }

  /** The extension with user tasks switched on or off, and no configuration behind it. */
  private final class Extension extends BusinessCockpitExtension {

    private final boolean reportsUserTasks;

    private Extension(
        final boolean reportsUserTasks) {

      super(null, null, List.of(), List.of(), handlers(), null, null, null, null);
      this.reportsUserTasks = reportsUserTasks;

    }

    @Override
    public boolean reportsUserTasks() {

      return reportsUserTasks;

    }

  }

}
