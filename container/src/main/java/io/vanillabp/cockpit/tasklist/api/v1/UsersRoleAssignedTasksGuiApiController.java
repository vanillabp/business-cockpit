package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A tasklist API controller which gives access to the tasks
 * <ul>
 *     <li>the user has claimed</li>
 *     <li>the user is assigned to</li>
 *     <li>the user is part of a group which is assigned to</li>
 *     <li>which have no user or group claimed or assigned (dangling tasks)</li>
 * </ul>
 */
@RestController("usersRulesAssignedTasksGuiApiController")
@RequestMapping(path = "/gui/api/v1/user-roles")
public class UsersRoleAssignedTasksGuiApiController extends AbstractUserTaskListGuiApiController {

	@Autowired
	private UserTaskService userTaskService;

	@Autowired
	private PersonAndGroupMapper personAndGroupMapper;

	@Override
	protected Mono<Page<UserTask>> getUserTasks(
			final UserDetails currentUser,
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp,
			final Collection<SearchQuery> searchQueries,
			final String sort,
			final boolean sortAscending,
			final UserTaskService.RetrieveItemsMode mode) {

		return userTaskService.getUserTasks(
				true,
				true,
				List.of(currentUser.getId()),
				null,
				currentUser.getAuthorities(),
				List.of(currentUser.getId()),
				pageNumber,
				pageSize,
				initialTimestamp,
				searchQueries,
				sort,
				sortAscending,
				mode);

	}

	@Override
	public Mono<Page<UserTask>> getUserTasksUpdated(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final int size,
			final Collection<String> knownUserTasksIds,
			final OffsetDateTime initialTimestamp,
			final Collection<SearchQuery> searchQueries,
			final String sort,
			final boolean sortAscending) {

		return userTaskService.getUserTasksUpdated(
				true,
				true,
				List.of(currentUser.getId()),
				null,
				currentUser.getAuthorities(),
				List.of(currentUser.getId()),
				size,
				knownUserTasksIds,
				initialTimestamp,
				searchQueries,
				sort,
				sortAscending);

	}

	@Override
	protected Flux<KwicResult> kwic(UserDetails currentUser,
									OffsetDateTime endedSince,
									List<SearchQuery> searchQueries,
									String path,
									String query) {

		return userTaskService.kwic(
				true,
				true,
				List.of(currentUser.getId()),
				null,
				currentUser.getAuthorities(),
				List.of(currentUser.getId()),
				endedSince,
				searchQueries,
				path,
				query
		);

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
			final var currentUserId = currentUser.getId();
			return userTaskService.unclaimTask(currentUserId, userTaskId, currentUserId);
		}
		return userTaskService.claimTask(userTaskId, personAndGroupMapper.toModelPerson(currentUser));

	}

	@Override
	protected Flux<UserTask> claimTasks(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final boolean unclaim) {

		if (unclaim) {
			final var currentUserId = currentUser.getId();
			return userTaskService.unclaimTask(currentUserId, userTaskIds, currentUserId);
		}
		return userTaskService.claimTask(userTaskIds, personAndGroupMapper.toModelPerson(currentUser));

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
		return userTaskService.assignTask(userTaskId, personAndGroupMapper.toModelPerson(userId));

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
		return userTaskService.assignTask(userTaskIds, personAndGroupMapper.toModelPerson(userId));

	}

}
