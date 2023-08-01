package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.Page;
import io.vanillabp.cockpit.gui.api.v1.TasklistApi;
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
            final OffsetDateTime initialTimestamp,
            final ServerWebExchange exchange) {
		
        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

		final var tasks = userTaskService.getUserTasks(
				pageNumber,
				pageSize,
				timestamp);

		return tasks.map(page -> ResponseEntity.ok(
		        new UserTasks()
        				.page(new Page()
        						.number(page.getNumber())
        						.size(page.getSize())
        						.totalElements(page.getTotalElements())
        						.totalPages(page.getTotalPages()))
        				.userTasks(mapper.toApi(page.getContent()))
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
                        Mono.just(entry.getT2())))
                .map(entry -> ResponseEntity.ok(
                        new UserTasks()
                                .page(new Page()
                                        .number(entry.getT1().getNumber())
                                        .size(entry.getT1().getSize())
                                        .totalElements(entry.getT1().getTotalElements())
                                        .totalPages(entry.getT1().getTotalPages()))
                                .userTasks(mapper.toApi(entry.getT1().getContent()))
                                .serverTimestamp(entry.getT2())))
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
