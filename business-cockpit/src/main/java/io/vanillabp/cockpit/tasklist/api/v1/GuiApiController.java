package io.vanillabp.cockpit.tasklist.api.v1;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.gui.api.v1.Page;
import io.vanillabp.cockpit.gui.api.v1.TasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTaskEvent;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksUpdate;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import io.vanillabp.cockpit.tasklist.UserTaskService;

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
	public ResponseEntity<UserTasks> getUserTasks(
			final Integer pageNumber,
			final Integer pageSize) {
		
		final var tasks = userTaskService.getUserTasks(
				pageNumber,
				pageSize);
	
		final var result = new UserTasks()
				.page(new Page()
						.number(tasks.getNumber())
						.size(tasks.getSize())
						.totalElements(tasks.getTotalElements())
						.totalPages(tasks.getTotalPages()))
				.userTasks(mapper.toApi(tasks.getContent()))
				.serverTimestamp(OffsetDateTime.now());
		
		return ResponseEntity.ok(result);
		
	}
	
	@Override
	public ResponseEntity<UserTasks> getUserTasksUpdate(
	        final UserTasksUpdate userTasksUpdate) {
	    
	    if ((userTasksUpdate == null)
	            || (userTasksUpdate.getSize() < 1)) {
	        return ResponseEntity.badRequest().build();
	    }
	    
        final var tasks = userTaskService.getUserTasksUpdated(
                userTasksUpdate.getSize(),
                userTasksUpdate.getKnownUserTasksIds());
    
        final var result = new UserTasks()
                .page(new Page()
                        .number(tasks.getNumber())
                        .size(tasks.getSize())
                        .totalElements(tasks.getTotalElements())
                        .totalPages(tasks.getTotalPages()))
                .userTasks(mapper.toApi(tasks.getContent()))
                .serverTimestamp(OffsetDateTime.now());
        
        return ResponseEntity.ok(result);
        
	}
	
}
