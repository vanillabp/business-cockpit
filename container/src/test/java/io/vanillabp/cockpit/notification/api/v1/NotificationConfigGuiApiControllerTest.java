package io.vanillabp.cockpit.notification.api.v1;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.RecipientConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationConfigGuiApiControllerTest {

    private UserContext userContext;
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

    private MockMvc clientWith(List<NotificationService> media) {
        controller = new NotificationConfigGuiApiController(
                userContext, userRepository, media, userTaskService);
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    @BeforeEach
    void setUp() {
        userContext = mock(UserContext.class);
        userRepository = mock(UserRepository.class);
        userTaskService = mock(UserTaskService.class);
        when(userContext.getUserLoggedInDetails())
                .thenReturn(userDetails("u1", List.of("g1")));
    }

    @Test
    void media_empty_whenNoNotificationService() throws Exception {
        clientWith(List.of()).perform(get("/gui/api/v1/notifications/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void media_reflectsRegisteredBeans() throws Exception {
        clientWith(List.of(emailMedium())).perform(get("/gui/api/v1/notifications/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("email"))
                .andExpect(jsonPath("$[0].name.de").value("E-Mail"));
    }

    @Test
    void config_get_returnsStoredConfiguration() throws Exception {
        final var user = new User();
        user.setId("u1");
        user.setNotificationConfiguration(new NotificationConfiguration(Map.of("email", true), Map.of()));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        clientWith(List.of(emailMedium())).perform(get("/gui/api/v1/notifications/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalAllViaMedium.email").value(true));
    }

    @Test
    void config_get_returnsEmptyWhenUserUnknown() throws Exception {
        when(userRepository.findById("u1")).thenReturn(Optional.empty());

        clientWith(List.of(emailMedium())).perform(get("/gui/api/v1/notifications/config"))
                .andExpect(status().isOk());
    }

    @Test
    void config_put_persistsDomainConfiguration() throws Exception {
        final var user = new User();
        user.setId("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        clientWith(List.of(emailMedium())).perform(put("/gui/api/v1/notifications/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"globalAllViaMedium\": { \"email\": true } }"))
                .andExpect(status().isOk());

        final ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        final var config = saved.getValue().getNotificationConfiguration();
        assertTrue(config.globalAllViaMedium().get("email"));
    }

    @Test
    void recipientConfig_returnsPerMediumValues() throws Exception {
        clientWith(List.of(emailMedium())).perform(get("/gui/api/v1/notifications/recipient-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medium").value("email"))
                .andExpect(jsonPath("$[0].values[0].type").value("emailAddress"))
                .andExpect(jsonPath("$[0].values[0].value").value("a@b.c"));
    }

    @Test
    void recipientConfig_save_unknownMedium_notFound() throws Exception {
        clientWith(List.of(emailMedium())).perform(put("/gui/api/v1/notifications/recipient-config/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"phone\": \"123\" }"))
                .andExpect(status().isNotFound());
    }

    @Test
    void workflows_returnsUserVisibleWorkflows() throws Exception {
        final var task = new UserTask();
        task.setWorkflowModuleId("wfmA");
        task.setBpmnProcessId("procX");
        task.setWorkflowTitle(Map.of("en", "Invoice"));
        when(userTaskService.getVisibleWorkflows(any(), any(), any(), any()))
                .thenReturn(List.of(task));

        clientWith(List.of(emailMedium())).perform(get("/gui/api/v1/notifications/workflows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workflowModuleId").value("wfmA"))
                .andExpect(jsonPath("$[0].bpmnProcessId").value("procX"))
                .andExpect(jsonPath("$[0].workflowTitle.en").value("Invoice"));
    }

    @Test
    void workflows_passesCurrentUserVisibility() throws Exception {
        when(userTaskService.getVisibleWorkflows(any(), any(), any(), any())).thenReturn(List.of());

        clientWith(List.of(emailMedium())).perform(get("/gui/api/v1/notifications/workflows"))
                .andExpect(status().isOk());

        // assignees = candidateUsers = candidatesToBeExcluded = [u1], candidateGroups = authorities
        verify(userTaskService).getVisibleWorkflows(List.of("u1"), List.of("u1"), List.of("g1"), List.of("u1"));
    }

}
