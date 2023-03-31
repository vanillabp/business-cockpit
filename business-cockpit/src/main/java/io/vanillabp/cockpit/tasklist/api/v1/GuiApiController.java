package io.vanillabp.cockpit.tasklist.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.gui.api.v1.Page;
import io.vanillabp.cockpit.gui.api.v1.TasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.tasklist.UserTaskService;

@RestController
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController implements TasklistApi {

	@Autowired
	private UserTaskService userTaskService;
	
	@Autowired
	private GuiApiMapper mapper;
	
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
				.userTasks(mapper.toApi(tasks.getContent()));
		
		return ResponseEntity.ok(result);
		
	}
	
}
