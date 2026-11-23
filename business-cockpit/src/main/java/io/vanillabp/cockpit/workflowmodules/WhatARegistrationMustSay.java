package io.vanillabp.cockpit.workflowmodules;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.vanillabp.cockpit.exceptions.BcInvalidRequestException;
import io.vanillabp.cockpit.workflowmodules.model.GroupHierarchy;

/**
 * The rules a registration of a workflow module has to follow before the cockpit stores it.
 * <p>
 * The cockpit serves each module below {@code /wm/<id>/} and passes those requests on to the URI
 * of the module. Both values go into the route of the module, so a registration whose id or URI
 * cannot become a route is refused, and nothing of it is stored.
 * <p>
 * The id is a plain segment of a path. It holds letters, digits, {@code -}, {@code _}, {@code .}
 * and {@code ~} only, the characters a URI never has to encode, and it is not made of dots only,
 * because {@code .} and {@code ..} are steps in a path. Anything else, like {@code *} or
 * {@code {x}}, would be read as a pattern by the route. Each module keeps to its own path. The
 * extension checks the same rule when the workflow module starts.
 * <p>
 * The URI is an absolute {@code http} or {@code https} URI with a host. The cockpit calls it, so
 * nothing else would work anyway.
 */
public final class WhatARegistrationMustSay {

  private static final Pattern PLAIN_PATH_SEGMENT = Pattern.compile("[A-Za-z0-9._~-]+");

  private static final Pattern ONLY_DOTS = Pattern.compile("\\.+");

  private static final Set<String> SCHEMES = Set.of("http", "https");

  private WhatARegistrationMustSay() {
  }

  /**
   * Whether the id of a workflow module can be part of its route.
   *
   * @param id The id of the workflow module
   * @return Why it cannot, or {@code null} if it can
   */
  public static String whatIsWrongWithTheId(
      final String id) {

    if ((id == null) || id.isEmpty()) {
      return "'id' is missing";
    }
    if (!PLAIN_PATH_SEGMENT.matcher(id).matches() || ONLY_DOTS.matcher(id).matches()) {
      return "'id' may hold only letters, digits, '-', '_', '.' and '~', and not only dots";
    }
    return null;

  }

  /**
   * Whether the URI of a workflow module is one the cockpit can pass requests on to.
   *
   * @param uri The URI the module registered
   * @return Why it cannot, or {@code null} if it can
   */
  public static String whatIsWrongWithTheUri(
      final String uri) {

    if ((uri == null) || uri.isBlank()) {
      return "'uri' is missing";
    }
    final URI parsed;
    try {
      parsed = new URI(uri);
    } catch (URISyntaxException e) {
      return "'uri' is no valid URI";
    }
    if (!parsed.isAbsolute() || !SCHEMES
        .contains(parsed.getScheme().toLowerCase(Locale.ROOT)) || (parsed.getHost() == null)) {
      return "'uri' is no absolute http or https URI with a host";
    }
    return null;

  }

  /**
   * Refuses a registration whose id or URI cannot become a route.
   *
   * @param id The id of the workflow module
   * @param uri The URI the module registered
   * @throws BcInvalidRequestException naming each value which is wrong
   */
  public static void checkIdAndUri(
      final String id,
      final String uri) {

    final var violations = new LinkedList<String>();
    final var wrongId = whatIsWrongWithTheId(id);
    if (wrongId != null) {
      violations.add(wrongId);
    }
    final var wrongUri = whatIsWrongWithTheUri(uri);
    if (wrongUri != null) {
      violations.add(wrongUri);
    }
    if (!violations.isEmpty()) {
      throw new BcInvalidRequestException(violations.toArray(String[]::new));
    }

  }

  /**
   * The group hierarchy of a registration as the cockpit keeps it: one entry per group. A target
   * which is {@code null} is dropped, and an entry without targets has none. Both REST and Kafka
   * send the hierarchy as a list of entries, so this is where a list becomes the map.
   *
   * @param <E> The type of an entry as the API sends it
   * @param entries The entries, or {@code null} for a registration without a hierarchy
   * @param group Reads the group of an entry
   * @param targets Reads the targets of an entry
   * @return The hierarchy, or {@code null} for a registration without one
   * @throws BcInvalidRequestException if an entry has no group or a group has two entries
   */
  public static <E> Map<String, Collection<String>> groupHierarchyOf(
      final List<E> entries,
      final Function<E, String> group,
      final Function<E, ? extends Collection<String>> targets) {

    if (entries == null) {
      return null;
    }
    final var result = new LinkedHashMap<String, Collection<String>>();
    for (final var entry : entries) {
      final var name = entry == null ? null : group.apply(entry);
      if ((name == null) || name.isBlank()) {
        throw new BcInvalidRequestException("'groupHierarchy' has an entry without a group");
      }
      if (result.containsKey(name)) {
        throw new BcInvalidRequestException("'groupHierarchy' has two entries for one group");
      }
      result.put(name, withoutNull(targets.apply(entry)));
    }
    return result;

  }

  /**
   * The group hierarchy of a stored registration, in the shape the resolution of groups takes.
   * A stored document is not refused: what it says is used as far as it can be. Entries without a
   * group and targets which are {@code null} are dropped, and two entries for one group are joined.
   * Such a document can only come from an older version of the cockpit or from a hand.
   *
   * @param stored The hierarchy as the document keeps it, or {@code null}
   * @return The hierarchy, or {@code null} if the document has none
   */
  public static Map<String, Collection<String>> groupHierarchyOf(
      final List<GroupHierarchy> stored) {

    if (stored == null) {
      return null;
    }
    final var result = new LinkedHashMap<String, Collection<String>>();
    stored
        .stream()
        .filter(Objects::nonNull)
        .filter(entry -> entry.group() != null)
        .forEach(entry -> result
            .computeIfAbsent(entry.group(), group -> new LinkedList<>())
            .addAll(withoutNull(entry.targets())));
    return result;

  }

  /**
   * The same hierarchy, with every target which is {@code null} dropped.
   *
   * @param groupHierarchy For each group, the groups it stands for, or {@code null}
   * @return The hierarchy without {@code null} targets, or {@code null}
   */
  public static Map<String, Collection<String>> withoutNullTargets(
      final Map<String, Collection<String>> groupHierarchy) {

    if (groupHierarchy == null) {
      return null;
    }
    final var result = new LinkedHashMap<String, Collection<String>>();
    groupHierarchy.forEach((
        group,
        targets) -> result.put(group, withoutNull(targets)));
    return result;

  }

  private static List<String> withoutNull(
      final Collection<String> targets) {

    if (targets == null) {
      return new LinkedList<>();
    }
    return targets
        .stream()
        .filter(Objects::nonNull)
        .collect(LinkedList::new, List::add, List::addAll);

  }

}
