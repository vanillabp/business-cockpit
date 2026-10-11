package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The main task list of the delivered application. It holds everything its user could work on:
 * the tasks they have claimed, the tasks which name them or one of their groups, and the tasks
 * addressed to nobody.
 */
@RestController("tasklistGuiApiController")
@RequestMapping(path = "/gui/api/v1")
public class GuiApiController extends AbstractUserTaskListGuiApiController {

	@Override
	protected UserTaskVisibility userTasksVisibleTo(
			final UserDetails currentUser) {

		return UserTaskVisibility.everythingTheUserMayWorkOn(currentUser);

	}

}
