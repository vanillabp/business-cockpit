package io.vanillabp.cockpit.support.security.usercontext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The groups of a login come from the directory on every request. A request of a user the directory
 * does not know goes on without groups, and a directory which fails fails the request.
 */
@ExtendWith(SuppressOutputExtension.class)
class DirectoryUserLookupTest {

  private ChangeableUserDirectory directory;

  private RecordingLogger logger;

  private Instant now;

  private DirectoryUserLookup lookup;

  @BeforeEach
  void setUp() {

    directory = new ChangeableUserDirectory().with("john", "ADMIN", "SALES");
    logger = new RecordingLogger();
    now = Instant.parse("2026-11-17T10:00:00Z");
    lookup = new DirectoryUserLookup(directory, Function.identity(), logger, () -> now);

  }

  @Test
  void theGroupsComeFromTheDirectoryOnEveryLookup() {

    final var user = lookup.lookUp("john");

    assertThat(user.id()).isEqualTo("john");
    assertThat(user.directoryUser()).isPresent();
    assertThat(user.authorities()).containsExactlyInAnyOrder("ADMIN", "SALES", "USER_john");

    directory.with("john", "SALES");

    assertThat(lookup.lookUp("john").authorities())
        .as("the directory took ADMIN away")
        .containsExactlyInAnyOrder("SALES", "USER_john");

  }

  @Test
  void theGroupResolverAddsTheGroupsAHierarchyImplies() {

    final var withHierarchy = new DirectoryUserLookup(directory, groups -> {
      final var resolved = new HashSet<>(groups);
      if (groups.contains("ADMIN")) {
        resolved.add("TASK_VIEWER");
      }
      return resolved;
    });

    assertThat(withHierarchy.lookUp("john").authorities())
        .containsExactlyInAnyOrder("ADMIN", "SALES", "TASK_VIEWER", "USER_john");

  }

  @Test
  void aUserWithoutGroupsHasOnlyItsOwnAuthority() {

    directory.with(new UserDetailsImpl("jane", null, null, null, null));

    assertThat(lookup.lookUp("jane").authorities()).containsExactly("USER_jane");

  }

  @Test
  void aUserTheDirectoryDoesNotKnowHasOnlyItsOwnAuthorityAndIsWarnedAboutOnceAMinute() {

    directory.remove("john");

    final var user = lookup.lookUp("john");
    lookup.lookUp("john");
    lookup.lookUp("john");

    assertThat(user.directoryUser()).isEmpty();
    assertThat(user.authorities()).containsExactly("USER_john");
    assertThat(logger.messagesAt(Level.WARN))
        .as("three requests within a minute give one warning")
        .singleElement()
        .asString()
        .contains("'john'", "USER_john");

    now = now.plus(Duration.ofSeconds(59));
    lookup.lookUp("john");
    assertThat(logger.messagesAt(Level.WARN)).hasSize(1);

    now = now.plus(Duration.ofSeconds(1));
    lookup.lookUp("john");
    assertThat(logger.messagesAt(Level.WARN))
        .as("a minute later the user is still unknown, which is worth saying again")
        .hasSize(2);

  }

  @Test
  void manyUnknownUsersDoNotFillTheMemory() {

    for (var user = 0; user < 1000; user++) {
      lookup.lookUp("unknown-"
          + user);
    }
    now = now.plus(Duration.ofMinutes(1));
    lookup.lookUp("one-more");

    assertThat(logger.messagesAt(Level.WARN)).hasSize(1001);

  }

  @Test
  void aFailingDirectoryFailsTheLookupWithALoggedError() {

    final var failure = new IllegalStateException("LDAP is down");
    directory.failWith(failure);

    assertThatThrownBy(() -> lookup.lookUp("john")).isSameAs(failure);
    assertThat(logger.messagesAt(Level.ERROR))
        .singleElement()
        .asString()
        .contains("'john'", "the request fails");

  }

  /**
   * Notes every line it is given, so a test can read what was logged without a logging backend.
   */
  private static class RecordingLogger extends AbstractLogger {

    private static final long serialVersionUID = 1L;

    private final List<Line> lines = new ArrayList<>();

    private record Line(
                        Level level,
                        String message) {
    }

    List<String> messagesAt(
        final Level level) {

      return lines
          .stream()
          .filter(line -> line.level() == level)
          .map(Line::message)
          .toList();

    }

    @Override
    protected String getFullyQualifiedCallerName() {
      return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(
        final Level level,
        final Marker marker,
        final String messagePattern,
        final Object[] arguments,
        final Throwable throwable) {

      lines.add(new Line(level, MessageFormatter.basicArrayFormat(messagePattern, arguments)));

    }

    @Override
    public boolean isTraceEnabled() {
      return true;
    }

    @Override
    public boolean isTraceEnabled(
        final Marker marker) {
      return true;
    }

    @Override
    public boolean isDebugEnabled() {
      return true;
    }

    @Override
    public boolean isDebugEnabled(
        final Marker marker) {
      return true;
    }

    @Override
    public boolean isInfoEnabled() {
      return true;
    }

    @Override
    public boolean isInfoEnabled(
        final Marker marker) {
      return true;
    }

    @Override
    public boolean isWarnEnabled() {
      return true;
    }

    @Override
    public boolean isWarnEnabled(
        final Marker marker) {
      return true;
    }

    @Override
    public boolean isErrorEnabled() {
      return true;
    }

    @Override
    public boolean isErrorEnabled(
        final Marker marker) {
      return true;
    }

  }

}
