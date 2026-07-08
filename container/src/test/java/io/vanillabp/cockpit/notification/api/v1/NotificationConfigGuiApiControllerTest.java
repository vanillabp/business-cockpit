package io.vanillabp.cockpit.notification.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.RecipientConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class NotificationConfigGuiApiControllerTest {

    private ReactiveUserContext userContext;
    private UserRepository userRepository;
    private UserTaskService userTaskService;
    private NotificationConfigGuiApiController controller;

    private static UserDetails userDetails(String id, List<String> authorities) {
        return new UserDetails() {
            public String getId() {
                return id;
            }

            public String getEmail() {
                return id + "@example.org";
            }

            public String getDisplay() {
                return id;
            }

            public String getDisplayShort() {
                return id;
            }

            public List<String> getAuthorities() {
                return authorities;
            }
        };
    }

    /** Minimal email medium. */
    private static NotificationService emailMedium() {
        return new NotificationService() {
            public String getType() {
                return "email";
            }

            public Map<String, String> getName() {
                return Map.of("de", "E-Mail", "en", "Email");
            }

            public List<RecipientConfiguration> getRecipientConfiguration(String userId) {
                return List.of(new RecipientConfiguration("emailAddress",
                        Map.of("en", "E-mail address"), Map.of("en", "used for notifications"), "a@b.c"));
            }

            public void saveRecipientConfiguration(String userId, Map<String, String> values) {
            }

            public void sendNotification(List<String> userIds, UserTask userTask) {
            }
        };
    }

    private WebTestClient clientWith(List<NotificationService> media) {
        controller = new NotificationConfigGuiApiController(
                userContext, userRepository, media, userTaskService);
        return WebTestClient.bindToController(controller).build();
    }

    @BeforeEach
    void setUp() {
        userContext = mock(ReactiveUserContext.class);
        userRepository = mock(UserRepository.class);
        userTaskService = mock(UserTaskService.class);
        when(userContext.getUserLoggedInDetailsAsMono())
                .thenReturn(Mono.just(userDetails("u1", List.of("g1"))));
    }

    @Test
    void media_empty_whenNoNotificationService() throws Exception {
        clientWith(List.of()).get().uri("/gui/api/v1/notifications/media")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void media_reflectsRegisteredBeans() throws Exception {
        clientWith(List.of(emailMedium())).get().uri("/gui/api/v1/notifications/media")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].type").isEqualTo("email")
                .jsonPath("$[0].name.de").isEqualTo("E-Mail");
    }

    @Test
    void config_get_returnsStoredConfiguration() throws Exception {
        final var user = new User();
        user.setId("u1");
        user.setNotificationConfiguration(new NotificationConfiguration(Map.of("email", true), Map.of()));
        when(userRepository.findById("u1")).thenReturn(Mono.just(user));

        clientWith(List.of(emailMedium())).get().uri("/gui/api/v1/notifications/config")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.globalAllViaMedium.email").isEqualTo(true);
    }

    @Test
    void config_get_returnsEmptyWhenUserUnknown() throws Exception {
        when(userRepository.findById("u1")).thenReturn(Mono.empty());

        clientWith(List.of(emailMedium())).get().uri("/gui/api/v1/notifications/config")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void config_put_persistsDomainConfiguration() throws Exception {
        final var user = new User();
        user.setId("u1");
        when(userRepository.findById("u1")).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        clientWith(List.of(emailMedium())).put().uri("/gui/api/v1/notifications/config")
                .bodyValue(Map.of("globalAllViaMedium", Map.of("email", true)))
                .exchange()
                .expectStatus().isOk();

        final ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        final var config = saved.getValue().getNotificationConfiguration();
        assertTrue(config.globalAllViaMedium().get("email"));
    }

    @Test
    void recipientConfig_returnsPerMediumValues() throws Exception {
        clientWith(List.of(emailMedium())).get().uri("/gui/api/v1/notifications/recipient-config")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].medium").isEqualTo("email")
                .jsonPath("$[0].values[0].type").isEqualTo("emailAddress")
                .jsonPath("$[0].values[0].value").isEqualTo("a@b.c");
    }

    @Test
    void recipientConfig_save_unknownMedium_notFound() throws Exception {
        clientWith(List.of(emailMedium())).put().uri("/gui/api/v1/notifications/recipient-config/sms")
                .bodyValue(Map.of("phone", "123"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void workflows_returnsUserVisibleWorkflows() throws Exception {
        final var task = new UserTask();
        task.setWorkflowModuleId("wfmA");
        task.setBpmnProcessId("procX");
        task.setWorkflowTitle(Map.of("en", "Invoice"));
        when(userTaskService.getVisibleWorkflows(any(), any(), any(), any()))
                .thenReturn(Flux.just(task));

        clientWith(List.of(emailMedium())).get().uri("/gui/api/v1/notifications/workflows")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].workflowModuleId").isEqualTo("wfmA")
                .jsonPath("$[0].bpmnProcessId").isEqualTo("procX")
                .jsonPath("$[0].workflowTitle.en").isEqualTo("Invoice");
    }

    @Test
    void workflows_passesCurrentUserVisibility() throws Exception {
        when(userTaskService.getVisibleWorkflows(any(), any(), any(), any())).thenReturn(Flux.empty());

        clientWith(List.of(emailMedium())).get().uri("/gui/api/v1/notifications/workflows")
                .exchange()
                .expectStatus().isOk();

        // assignees = candidateUsers = candidatesToBeExcluded = [u1], candidateGroups = authorities
        verify(userTaskService).getVisibleWorkflows(List.of("u1"), List.of("u1"), List.of("g1"), List.of("u1"));
    }

}
