package io.vanillabp.cockpit.extension.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import io.vanillabp.integration.extension.spi.handler.HandlerContract;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.TaskParam;

/**
 * The process variables the application's <code>&#64;UserTaskDetailsProvider</code> methods read
 * with <code>&#64;TaskParam</code>, by the user task they serve.
 * <p>
 * A BPMS half needs this where its engine hands out only the variables somebody asked for.
 * Camunda 8 is such an engine. A worker names the variables its jobs carry, and a variable it
 * does not name never reaches the job. Asking for the whole scope instead would make every job
 * carry every variable of its workflow. So the half asks for the names the providers of a task
 * read, and for nothing more.
 * <p>
 * The names are noted while VanillaBP scans the workflow services. The contract of
 * <code>&#64;UserTaskDetailsProvider</code> hands every method to {@link #note}, at the moment
 * VanillaBP binds its parameters. So these are the methods VanillaBP will call, and nothing
 * walks the classes of the application a second time.
 * <p>
 * The names are kept by lookup key only, and not by workflow module or BPMN process. The scan
 * shows a method and its annotation, but not the process the method was registered for. So the
 * answer for one user task is the union over every provider of the application which serves a
 * key of the same spelling. A job may therefore carry a variable which a provider of another
 * process reads under the same element id or task definition. It never misses a variable its own
 * provider reads.
 */
public final class DetailsProviderTaskParams {

  /**
   * The names the providers of each lookup key read. A provider writing
   * {@link UserTaskDetailsProvider#ALL} is noted under {@link HandlerContract#EVERY_KEY}.
   */
  private final Map<String, Set<String>> namesByLookupKey = new ConcurrentHashMap<>();

  /**
   * Creates an empty record. It is filled while VanillaBP scans the workflow services.
   */
  public DetailsProviderTaskParams() {
  }

  /**
   * Notes what one occurrence of <code>&#64;UserTaskDetailsProvider</code> reads. VanillaBP calls
   * this for every occurrence it finds, through the contract's annotation check.
   *
   * @param annotation One occurrence of <code>&#64;UserTaskDetailsProvider</code>
   * @param method The method carrying it
   */
  public void note(
      final Annotation annotation,
      final Method method) {

    final var names = Arrays
        .stream(method.getParameters())
        .map(parameter -> parameter.getAnnotation(TaskParam.class))
        .filter(Objects::nonNull)
        .map(TaskParam::value)
        .toList();
    if (names.isEmpty()) {
      return;
    }
    final var lookupKeys = new LinkedList<>(BusinessCockpitHandlers.userTaskLookupKeys(annotation));
    if (lookupKeys.isEmpty()) {
      // the convention every VanillaBP annotation follows, and VanillaBP registers such a method
      // under this key too: an annotation naming nothing serves the method's own name
      lookupKeys.add(method.getName());
    }
    lookupKeys
        .forEach(
            lookupKey -> namesByLookupKey
                .computeIfAbsent(lookupKey, key -> ConcurrentHashMap.newKeySet())
                .addAll(names));

  }

  /**
   * The variables the providers of one user task read.
   * <p>
   * The answer includes what a provider writing {@link UserTaskDetailsProvider#ALL} reads. Such
   * a provider runs only where no provider names the task, but whether one does may differ by
   * the version of the process. So its names count for every task.
   *
   * @param lookupKeys The keys the task is served by, as
   *          {@link BusinessCockpitHandlers#lookupKeysOf(String, String)} builds them
   * @return The names, sorted so that a subscription built from them stays the same across
   *         restarts. Empty where no provider of the task reads a variable
   */
  public List<String> namesReadBy(
      final Collection<String> lookupKeys) {

    final var names = new TreeSet<String>();
    lookupKeys.forEach(lookupKey -> names.addAll(namesByLookupKey.getOrDefault(lookupKey, Set.of())));
    names.addAll(namesByLookupKey.getOrDefault(HandlerContract.EVERY_KEY, Set.of()));
    return List.copyOf(names);

  }

}
