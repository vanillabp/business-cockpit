package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The list of work still up for grabs: what the user's groups may take and the user has not claimed
 * yet, together with the tasks addressed to nobody.
 */
@RestController("usersRulesAssignedTasksGuiApiController")
@RequestMapping(path = "/gui/api/v1/user-roles")
public class UsersRoleAssignedTasksGuiApiController extends AbstractUserTaskListGuiApiController {

	@Override
	protected UserTaskVisibility userTasksVisibleTo(
			final UserDetails currentUser) {

		return UserTaskVisibility.whatTheUsersGroupsMayTake(currentUser);

	}

}
