package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The list of what is the user's own, without the tasks their groups may take and without the ones
 * addressed to nobody.
 */
@RestController("currentUsersTasksGuiApiController")
@RequestMapping(path = "/gui/api/v1/current-user")
public class CurrentUsersTasksGuiApiController extends AbstractUserTaskListGuiApiController {

	@Override
	protected UserTaskVisibility userTasksVisibleTo(
			final UserDetails currentUser) {

		return UserTaskVisibility.onlyWhatIsTheUsersOwn(currentUser);

	}

}
