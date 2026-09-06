package io.vanillabp.cockpit.notification.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.gui.api.v1.NotificationConfiguration;
import io.vanillabp.cockpit.gui.api.v1.NotificationMedium;
import io.vanillabp.cockpit.gui.api.v1.NotificationWorkflow;
import io.vanillabp.cockpit.gui.api.v1.OfficialNotificationConfigApi;
import io.vanillabp.cockpit.gui.api.v1.RecipientConfiguration;
import io.vanillabp.cockpit.gui.api.v1.RecipientMediumConfiguration;
import io.vanillabp.cockpit.gui.api.v1.WorkflowNotificationConfiguration;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GUI API for the notification configuration page (AC func 1/4/6, AC tech 3/10). All operations act
 * on the currently authenticated user; the user id is never taken from the client.
 */
@RestController
@RequestMapping(path = "/gui/api/v1")
public class NotificationConfigGuiApiController implements OfficialNotificationConfigApi {

    private final UserContext userContext;

    private final UserRepository userRepository;

    private final List<NotificationService> notificationServices;

    private final UserTaskService userTaskService;

    public NotificationConfigGuiApiController(
            final UserContext userContext,
            final UserRepository userRepository,
            final List<NotificationService> notificationServices,
            final UserTaskService userTaskService) {

        this.userContext = userContext;
        this.userRepository = userRepository;
        this.notificationServices = notificationServices;
        this.userTaskService = userTaskService;

    }

    @Override
    public ResponseEntity<List<NotificationMedium>> getNotificationMedia() {

        return ResponseEntity.ok(notificationServices
                .stream()
                .map(service -> new NotificationMedium()
                        .type(service.getType())
                        .name(service.getName()))
                .toList());

    }

    @Override
    public ResponseEntity<NotificationConfiguration> getNotificationConfiguration() {

        final var userId = userContext.getUserLoggedInDetails().getId();

        return ResponseEntity.ok(userRepository
                .findById(userId)
                .map(user -> toApiConfig(user.getNotificationConfiguration()))
                .orElseGet(() -> toApiConfig(null)));

    }

    @Override
    public ResponseEntity<Void> saveNotificationConfiguration(
            final NotificationConfiguration notificationConfiguration) {

        final var userId = userContext.getUserLoggedInDetails().getId();
        final var config = toDomainConfig(notificationConfiguration);

        final var user = userRepository
                .findById(userId)
                .orElseGet(() -> {
                    final var created = new User();
                    created.setId(userId);
                    return created;
                });
        user.setNotificationConfiguration(config);
        userRepository.save(user);

        return ResponseEntity.ok().build();

    }

    @Override
    public ResponseEntity<List<RecipientMediumConfiguration>> getRecipientConfiguration() {

        final var userId = userContext.getUserLoggedInDetails().getId();

        return ResponseEntity.ok(notificationServices
                .stream()
                .map(service -> toApiRecipientMediumConfig(service, userId))
                .toList());

    }

    @Override
    public ResponseEntity<Void> saveRecipientConfiguration(
            final String mediumType,
            final Map<String, String> requestBody) {

        final var service = serviceByType(mediumType);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }

        final var userId = userContext.getUserLoggedInDetails().getId();
        service.saveRecipientConfiguration(userId, requestBody);

        return ResponseEntity.ok().build();

    }

    @Override
    public ResponseEntity<List<NotificationWorkflow>> getNotificationWorkflows() {

        final var currentUser = userContext.getUserLoggedInDetails();
        final var userId = currentUser.getId();

        return ResponseEntity.ok(userTaskService
                .getVisibleWorkflows(
                        List.of(userId),
                        List.of(userId),
                        currentUser.getAuthorities(),
                        List.of(userId))
                .stream()
                .map(this::toApiWorkflow)
                .toList());

    }

    private NotificationService serviceByType(
            final String type) {

        return notificationServices.stream()
                .filter(service -> service.getType().equals(type))
                .findFirst()
                .orElse(null);

    }

    private RecipientMediumConfiguration toApiRecipientMediumConfig(
            final NotificationService service,
            final String userId) {

        final var result = new RecipientMediumConfiguration()
                .medium(service.getType())
                .name(service.getName());
        service.getRecipientConfiguration(userId).forEach(rc -> result.addValuesItem(
                new RecipientConfiguration()
                        .type(rc.type())
                        .title(rc.title())
                        .description(rc.description())
                        .value(rc.value())));
        return result;

    }

    private NotificationWorkflow toApiWorkflow(
            final UserTask task) {

        return new NotificationWorkflow()
                .workflowModuleId(task.getWorkflowModuleId())
                .bpmnProcessId(task.getBpmnProcessId())
                .workflowTitle(task.getWorkflowTitle());

    }

    private static NotificationConfiguration toApiConfig(
            final io.vanillabp.cockpit.notification.model.NotificationConfiguration domain) {

        final var result = new NotificationConfiguration();
        if (domain == null) {
            return result;
        }
        if (domain.globalAllViaMedium() != null) {
            result.setGlobalAllViaMedium(domain.globalAllViaMedium());
        }
        if (domain.perWorkflow() != null) {
            domain.perWorkflow().forEach((key, value) -> result.putPerWorkflowItem(key,
                    new WorkflowNotificationConfiguration()
                            .none(value.none())
                            .allViaMedium(value.allViaMedium())));
        }
        return result;

    }

    private static io.vanillabp.cockpit.notification.model.NotificationConfiguration toDomainConfig(
            final NotificationConfiguration api) {

        if (api == null) {
            return new io.vanillabp.cockpit.notification.model.NotificationConfiguration(Map.of(), Map.of());
        }
        final var global = api.getGlobalAllViaMedium() == null ? Map.<String, Boolean>of() : api.getGlobalAllViaMedium();
        final var perWorkflow = new java.util.HashMap<String, io.vanillabp.cockpit.notification.model.WorkflowNotificationConfiguration>();
        if (api.getPerWorkflow() != null) {
            api.getPerWorkflow().forEach((key, value) -> perWorkflow.put(key,
                    new io.vanillabp.cockpit.notification.model.WorkflowNotificationConfiguration(
                            Boolean.TRUE.equals(value.getNone()),
                            value.getAllViaMedium() == null ? Map.of() : value.getAllViaMedium())));
        }
        return new io.vanillabp.cockpit.notification.model.NotificationConfiguration(global, perWorkflow);

    }

}
