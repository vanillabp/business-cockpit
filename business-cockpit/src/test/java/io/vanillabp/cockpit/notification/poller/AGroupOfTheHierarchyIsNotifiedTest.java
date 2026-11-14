package io.vanillabp.cockpit.notification.poller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import io.vanillabp.cockpit.notification.model.NotificationOutboxRepository;
import io.vanillabp.cockpit.notification.model.NotificationPollState;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user who sees a task only through a group of the group hierarchy is notified about it. The
 * task list completes the groups of the directory with the hierarchy on every request, so the
 * notifications have to do the same, or the two disagree about who sees the task.
 */
@ExtendWith(SuppressOutputExtension.class)
class AGroupOfTheHierarchyIsNotifiedTest {

  private static final String EMAIL = "email";

  private static final OffsetDateTime CURSOR = OffsetDateTime.parse("2026-11-14T10:00:00Z");

  /** Names nobody else uses, because the hierarchies are kept in a static cache. */
  private static final String WORKFLOW_MODULE = "a-group-of-the-hierarchy-is-notified";

  private static final String TEAM_LEAD = "TEAM_LEAD_OF_THE_HIERARCHY_TEST";

  private static final String TEAM_MEMBER = "TEAM_MEMBER_OF_THE_HIERARCHY_TEST";

  private static final String LEAD = "lead";

  private NotificationPoller poller;

  private NotificationOutboxRepository outbox;

  @BeforeEach
  void aTaskForTeamMembersAndALeadWhoIsNoMember() {

    final var task = new UserTask();
    task.setId("task-1");
    task.setWorkflowModuleId(WORKFLOW_MODULE);
    task.setBpmnProcessId("process");
    task.setCreatedAt(CURSOR.plusSeconds(10));
    task.setReportedAt(CURSOR.plusSeconds(10));
    final var members = new Group();
    members.setId(TEAM_MEMBER);
    task.setCandidateGroups(List.of(members));

    final var state = new NotificationPollState();
    state.setId(NotificationPollState.SINGLETON_ID);
    state.setCursor(CURSOR);

    final var mongoTemplate = mock(MongoTemplate.class);
    when(mongoTemplate.exists(any(Query.class), eq(NotificationPollState.class))).thenReturn(true);
    when(mongoTemplate.findAndModify(
        any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(NotificationPollState.class)))
        .thenReturn(state);
    when(mongoTemplate.find(any(Query.class), eq(UserTask.class))).thenReturn(List.of(task));

    final var lead = new User();
    lead.setId(LEAD);
    lead.setNotificationConfiguration(new NotificationConfiguration(Map.of(EMAIL, true), Map.of()));
    final var users = mock(UserRepository.class);
    when(users.findAll()).thenReturn(List.of(lead));

    // the directory knows the lead as a lead only, and not as a member
    final var directory = mock(UserDirectoryService.class);
    when(directory.getUser(LEAD))
        .thenReturn(Optional.of(new UserDetailsImpl(LEAD, null, null, null, List.of(TEAM_LEAD))));

    final var email = mock(NotificationService.class);
    when(email.getType()).thenReturn(EMAIL);

    outbox = mock(NotificationOutboxRepository.class);
    poller = new NotificationPoller(
        mongoTemplate, users, mock(UserTaskRepository.class), outbox, List
            .of(email), directory, new GroupHierarchyService(), 120, Duration.ofHours(48));

  }

  @AfterEach
  void forgetTheHierarchy() {

    GroupHierarchyService.putGroupHierarchy(WORKFLOW_MODULE, Map.of());

  }

  @Test
  @DisplayName("A lead who stands for the members is told about a task of the members")
  void aLeadWhoStandsForTheMembersIsNotified() {

    final Map<String, Collection<String>> leadsStandForMembers = Map.of(TEAM_LEAD, List.of(TEAM_MEMBER));
    GroupHierarchyService.putGroupHierarchy(WORKFLOW_MODULE, leadsStandForMembers);

    poller.runCycle();

    final var planned = ArgumentCaptor.forClass(NotificationOutboxEntry.class);
    verify(outbox).insert(planned.capture());
    assertThat(planned.getValue().getRecipientUserId()).isEqualTo(LEAD);
    assertThat(planned.getValue().getNotificationType()).isEqualTo(NotificationType.CREATED);
    assertThat(planned.getValue().getMedium()).isEqualTo(EMAIL);

  }

  @Test
  @DisplayName("Without the hierarchy the lead does not see the task and is not told about it")
  void withoutTheHierarchyTheLeadIsNotNotified() {

    poller.runCycle();

    verify(outbox, never()).insert(any(NotificationOutboxEntry.class));

  }

}
