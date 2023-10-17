package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.utils.UserContext;
import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.Page;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskEvent;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksUpdate;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestController("tasklistGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController implements OfficialTasklistApi {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private UserTaskService userTaskService;

	@Autowired
	private UserContext userContext;
	
	@Autowired
	private GuiApiMapper mapper;
	
    @EventListener(classes = UserTaskChangedNotification.class)
    public void updateClients(
            final UserTaskChangedNotification notification) {
        
        applicationEventPublisher.publishEvent(
                new GuiEvent(
                        notification.getSource(),
                        notification.getTargetRoles(),
                        new UserTaskEvent()
                                .name("UserTask")
                                .id(notification.getUserTaskId())
                                .type(notification.getType().toString())));
        
    }

    @Override
    public Mono<ResponseEntity<UserTasks>> getUserTasks(
            final Integer pageNumber,
            final Integer pageSize,
            final OffsetDateTime initialTimestamp,
            final ServerWebExchange exchange) {
		
        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

		final var tasksAndUser = Mono.zip(
				userTaskService.getUserTasks(
						pageNumber,
						pageSize,
						timestamp),
				userContext.getUserLoggedInAsMono());

		return tasksAndUser.map(tnu -> ResponseEntity.ok(
		        new UserTasks()
        				.page(new Page()
        						.number(tnu.getT1().getNumber())
        						.size(tnu.getT1().getSize())
        						.totalElements(tnu.getT1().getTotalElements())
        						.totalPages(tnu.getT1().getTotalPages()))
        				.userTasks(mapper.toApi(tnu.getT1().getContent(), tnu.getT2()))
        				.serverTimestamp(timestamp)));
		
	}
	
    @Override
    public Mono<ResponseEntity<UserTasks>> getUserTasksUpdate(
            final Mono<UserTasksUpdate> userTasksUpdate,
            final ServerWebExchange exchange) {
	    
        return userTasksUpdate
                .zipWhen(update -> Mono.just(update.getInitialTimestamp() != null
                        ? update.getInitialTimestamp()
                        : OffsetDateTime.now()))
                .flatMap(entry -> Mono.zip(
                        userTaskService.getUserTasksUpdated(
                                entry.getT1().getSize(),
                                entry.getT1().getKnownUserTasksIds(),
                                entry.getT2()),
                        Mono.just(entry.getT2()),
						userContext.getUserLoggedInAsMono()))
                .map(entry -> ResponseEntity.ok(
                        new UserTasks()
                                .page(new Page()
                                        .number(entry.getT1().getNumber())
                                        .size(entry.getT1().getSize())
                                        .totalElements(entry.getT1().getTotalElements())
                                        .totalPages(entry.getT1().getTotalPages()))
                                .userTasks(mapper.toApi(entry.getT1().getContent(), entry.getT3()))
                                .serverTimestamp(entry.getT2())))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));
            
	}
	
    @Override
    public Mono<ResponseEntity<UserTask>> getUserTask(
            final String userTaskId,
			final Boolean markAsRead,
            final ServerWebExchange exchange) {

		final var taskAndUser = Mono.zip(
				userTaskService.getUserTask(userTaskId),
				userContext.getUserLoggedInAsMono());

        return taskAndUser
				.flatMap(tnu -> {
					final var readAt = tnu.getT1().getReadAt(tnu.getT2());
					final Mono<io.vanillabp.cockpit.tasklist.model.UserTask> userTask;
					if ((markAsRead == null)        // not required to
							|| !markAsRead          // mark as read or
							|| (readAt != null)) {  // already read by current user
						userTask = Mono.just(tnu.getT1());
					} else {                        // to be marked as read by current user
						userTask = userTaskService
								.markAsRead(tnu.getT1().getId(), tnu.getT2());
					}
					return Mono.zip(userTask, Mono.just(tnu.getT2()));
				})
				.map(tnu -> mapper.toApi(tnu.getT1(), tnu.getT2())
				)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

    }

	@Override
	public Mono<ResponseEntity<Void>> usertaskUserTaskIdMarkAsReadPatch(
			final String userTaskId,
			final Boolean unread,
			final ServerWebExchange exchange) {

		final var currentUser = userContext
				.getUserLoggedInAsMono();

		final Mono<io.vanillabp.cockpit.tasklist.model.UserTask> result;
		if ((unread != null) && unread) {
			result = currentUser
					.flatMap(userId -> userTaskService.markAsUnread(userTaskId, userId));
		} else {
			result = currentUser
					.flatMap(userId -> userTaskService.markAsRead(userTaskId, userId));

		}

		return result
                .map(userTask -> ResponseEntity.ok().<Void>build())
				.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

	}

}
