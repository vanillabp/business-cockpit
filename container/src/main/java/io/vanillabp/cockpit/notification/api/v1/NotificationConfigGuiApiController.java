package io.vanillabp.cockpit.notification.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * GUI API for the notification configuration page (AC func 1/4/6, AC tech 3/10). All operations act
 * on the currently authenticated user; the user id is never taken from the client.
 * <p>
 * The blocking {@link NotificationService} methods (recipient configuration) are executed on a
 * bounded-elastic scheduler so the Netty event loop is not blocked.
 */
@RestController
@RequestMapping(path = "/gui/api/v1")
public class NotificationConfigGuiApiController implements OfficialNotificationConfigApi {

    private final ReactiveUserContext userContext;

    private final UserRepository userRepository;

    private final List<NotificationService> notificationServices;

    private final UserTaskService userTaskService;

    public NotificationConfigGuiApiController(
            final ReactiveUserContext userContext,
            final UserRepository userRepository,
            final List<NotificationService> notificationServices,
            final UserTaskService userTaskService) {

        this.userContext = userContext;
        this.userRepository = userRepository;
        this.notificationServices = notificationServices;
        this.userTaskService = userTaskService;

    }

    @Override
    public Mono<ResponseEntity<Flux<NotificationMedium>>> getNotificationMedia(
            final ServerWebExchange exchange) {

        final Flux<NotificationMedium> media = Flux
                .fromIterable(notificationServices)
                .map(service -> new NotificationMedium()
                        .type(service.getType())
                        .name(service.getName()));
        return Mono.just(ResponseEntity.ok(media));

    }

    @Override
    public Mono<ResponseEntity<NotificationConfiguration>> getNotificationConfiguration(
            final ServerWebExchange exchange) {

        return userContext
                .getUserLoggedInDetailsAsMono()
                .map(UserDetails::getId)
                .flatMap(userRepository::findById)
                .map(user -> ResponseEntity.ok(toApiConfig(user.getNotificationConfiguration())))
                .switchIfEmpty(Mono.fromSupplier(() -> ResponseEntity.ok(toApiConfig(null))));

    }

    @Override
    public Mono<ResponseEntity<Void>> saveNotificationConfiguration(
            final Mono<NotificationConfiguration> notificationConfiguration,
            final ServerWebExchange exchange) {

        return userContext
                .getUserLoggedInDetailsAsMono()
                .map(UserDetails::getId)
                .zipWith(notificationConfiguration)
                .flatMap(t -> {
                    final var userId = t.getT1();
                    final var config = toDomainConfig(t.getT2());
                    return userRepository
                            .findById(userId)
                            .switchIfEmpty(Mono.fromSupplier(() -> {
                                final var created = new User();
                                created.setId(userId);
                                return created;
                            }))
                            .flatMap(user -> {
                                user.setNotificationConfiguration(config);
                                return userRepository.save(user);
                            });
                })
                .thenReturn(ResponseEntity.ok().<Void>build());

    }

    @Override
    public Mono<ResponseEntity<Flux<RecipientMediumConfiguration>>> getRecipientConfiguration(
            final ServerWebExchange exchange) {

        return userContext
                .getUserLoggedInDetailsAsMono()
                .map(UserDetails::getId)
                .map(userId -> {
                    final Flux<RecipientMediumConfiguration> body = Flux
                            .fromIterable(notificationServices)
                            .flatMap(service -> Mono
                                    .fromCallable(() -> toApiRecipientMediumConfig(service, userId))
                                    .subscribeOn(Schedulers.boundedElastic()));
                    return ResponseEntity.ok(body);
                });

    }

    @Override
    public Mono<ResponseEntity<Void>> saveRecipientConfiguration(
            final String mediumType,
            final Mono<Map<String, String>> requestBody,
            final ServerWebExchange exchange) {

        final var service = serviceByType(mediumType);
        if (service == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return userContext
                .getUserLoggedInDetailsAsMono()
                .map(UserDetails::getId)
                .zipWith(requestBody)
                .flatMap(t -> Mono
                        .fromRunnable(() -> service.saveRecipientConfiguration(t.getT1(), t.getT2()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .thenReturn(ResponseEntity.ok().<Void>build());

    }

    @Override
    public Mono<ResponseEntity<Flux<NotificationWorkflow>>> getNotificationWorkflows(
            final ServerWebExchange exchange) {

        return userContext
                .getUserLoggedInDetailsAsMono()
                .map(currentUser -> {
                    final var userId = currentUser.getId();
                    final Flux<NotificationWorkflow> body = userTaskService
                            .getVisibleWorkflows(
                                    List.of(userId),
                                    List.of(userId),
                                    currentUser.getAuthorities(),
                                    List.of(userId))
                            .map(this::toApiWorkflow);
                    return ResponseEntity.ok(body);
                });

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
