package io.vanillabp.cockpit.util;

import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import io.vanillabp.cockpit.commons.exceptions.BcInvalidRequestException;

/**
 * The keys of a count request of the GUI API. A client names each filter it wants counted, and
 * finds its count in the answer by that name. Two filters with the same name would make the
 * answer ambiguous, so such a request is answered with {@code 400 Bad Request}.
 */
public final class CountKeys {

  private CountKeys() {
  }

  /**
   * Checks that no key of a count request is used twice. The answer names the position of each
   * filter whose key an earlier filter has already, and not the key itself, because an answer
   * never repeats a value the request sent.
   *
   * @param keys The keys of the filters, in the order of the request
   * @throws BcInvalidRequestException if a key is used more than once
   */
  public static void checkThatEachKeyIsUsedOnce(
      final List<String> keys) {

    final var seen = new HashSet<String>();
    final var usedBefore = IntStream
        .range(0, keys.size())
        .filter(position -> !seen.add(keys.get(position)))
        .mapToObj("'filters[%d].key' is used by an earlier filter already"::formatted)
        .toArray(String[]::new);
    if (usedBefore.length > 0) {
      throw new BcInvalidRequestException(usedBefore);
    }

  }

}
