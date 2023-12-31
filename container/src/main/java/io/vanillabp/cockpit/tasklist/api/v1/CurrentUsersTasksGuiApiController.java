package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * A tasklist API controller which gives access to the tasks
 * <ul>
 *     <li>the user has claimed</li>
 *     <li>the user is assigned to</li>
 *     <li>the user is part of a group which is assigned to</li>
 *     <li>which have no user or group claimed or assigned (dangling tasks)</li>
 * </ul>
 */
@RestController("currentUsersTasksGuiApiController")
@RequestMapping(path = "/gui/api/v1/current-user")
public class CurrentUsersTasksGuiApiController extends AbstractUserTaskListGuiApiController {

	@Autowired
	private UserTaskService userTaskService;

	@Override
	protected Mono<Page<UserTask>> getUserTasks(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp,
			final String sort,
			final boolean sortAscending) {

		return userTaskService.getUserTasks(
				false,
				false,
				List.of(currentUser.getId()),
				List.of(currentUser.getId()),
				null,
				pageNumber,
				pageSize,
				initialTimestamp,
				sort,
				sortAscending);

	}

	@Override
	public Mono<Page<UserTask>> getUserTasksUpdated(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final int size,
			final Collection<String> knownUserTasksIds,
			final OffsetDateTime initialTimestamp,
			final String sort,
			final boolean sortAscending) {

		return userTaskService.getUserTasksUpdated(
				false,
				false,
				List.of(currentUser.getId()),
				List.of(currentUser.getId()),
				null,
				size,
				knownUserTasksIds,
				initialTimestamp,
				sort,
				sortAscending);

	}

	@Override
	protected Mono<UserTask> getUserTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId) {

		return userTaskService.getUserTask(userTaskId);

	}

	@Override
	protected Mono<UserTask> markAsRead(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final boolean unread) {

		if (unread) {
			return userTaskService.markAsUnread(userTaskId, currentUser.getId());
		}
		return userTaskService.markAsRead(userTaskId, currentUser.getId());

	}

	@Override
	protected Flux<UserTask> markAsRead(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final boolean unread) {

		if (unread) {
			return userTaskService.markAsUnread(userTaskIds, currentUser.getId());
		}
		return userTaskService.markAsRead(userTaskIds, currentUser.getId());

	}

	@Override
	protected Mono<UserTask> claimTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final boolean unclaim) {

		if (unclaim) {
			return userTaskService.unclaimTask(userTaskId, currentUser.getId());
		}
		return userTaskService.claimTask(userTaskId, currentUser.getId());

	}

	@Override
	protected Flux<UserTask> claimTasks(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final boolean unclaim) {

		if (unclaim) {
			return userTaskService.unclaimTask(userTaskIds, currentUser.getId());
		}
		return userTaskService.claimTask(userTaskIds, currentUser.getId());

	}

	@Override
	protected Mono<UserTask> assignTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final String userId,
			final boolean unassign) {

		if (unassign) {
			return userTaskService.unassignTask(userTaskId, userId);
		}
		return userTaskService.assignTask(userTaskId, userId);

	}

	@Override
	protected Flux<UserTask> assignTasks(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final String userId,
			final boolean unassign) {

		if (unassign) {
			return userTaskService.unassignTask(userTaskIds, userId);
		}
		return userTaskService.assignTask(userTaskIds, userId);

	}

}
