package io.vanillabp.cockpit.itest;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.v1.AbstractUserTaskListGuiApiController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stands in for an application built on this library which needs a view none of the delivered ones
 * offers, and shows what writing one costs: a visibility of its own and nothing else.
 *
 * <p>The view here is what a supervisor would ask for. It holds every task addressed to one of the
 * user's groups, including the ones which name the user as excluded, and it holds nothing else. A
 * four-eyes exclusion keeps somebody from working on a task, and this view is about seeing where
 * the work stands rather than about doing it.
 *
 * <p>It lives in the test sources because it is an example, not part of the delivered application.
 * The component scan of the test context picks it up the way it picks up the delivered controllers.
 */
@RestController("groupWideTasksGuiApiController")
@RequestMapping(path = "/gui/api/v1/itest-group-wide")
public class GroupWideTasksGuiApiController extends AbstractUserTaskListGuiApiController {

    @Override
    protected UserTaskVisibility userTasksVisibleTo(
            final UserDetails currentUser) {

        return new UserTaskVisibility(
                false, false, null, null, currentUser.getAuthorities(), null, null);

    }

}
