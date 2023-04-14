package io.vanillabp.cockpit.tasklist.api.v1;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.Page;
import io.vanillabp.cockpit.gui.api.v1.TasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskEvent;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksUpdate;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController implements TasklistApi {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private UserTaskService userTaskService;
	
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
            final ServerWebExchange exchange) {
		
		final var tasks = userTaskService.getUserTasks(
				pageNumber,
				pageSize);

		return tasks.map(page -> ResponseEntity.ok(
		        new UserTasks()
        				.page(new Page()
        						.number(page.getNumber())
        						.size(page.getSize())
        						.totalElements(page.getTotalElements())
        						.totalPages(page.getTotalPages()))
        				.userTasks(mapper.toApi(page.getContent()))
        				.serverTimestamp(OffsetDateTime.now())));
		
	}
	
    @Override
    public Mono<ResponseEntity<UserTasks>> getUserTasksUpdate(
            final Mono<UserTasksUpdate> userTasksUpdate,
            final ServerWebExchange exchange) {
	    
        return userTasksUpdate
                .flatMap(update -> userTaskService.getUserTasksUpdated(
                            update.getSize(),
                            update.getKnownUserTasksIds()))
                .map(page -> ResponseEntity.ok(
                        new UserTasks()
                                .page(new Page()
                                        .number(page.getNumber())
                                        .size(page.getSize())
                                        .totalElements(page.getTotalElements())
                                        .totalPages(page.getTotalPages()))
                                .userTasks(mapper.toApi(page.getContent()))
                                .serverTimestamp(OffsetDateTime.now())))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));
        
	}
	
    @Override
    public Mono<ResponseEntity<UserTask>> getUserTask(
            final String userTaskId,
            final ServerWebExchange exchange) {
        
        return userTaskService
                .getUserTask(userTaskId)
                .map(mapper::toApi)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
        
    }
    
}
