package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.FollowUpDateRequest;
import io.vanillabp.cockpit.gui.api.v1.KwicRequest;
import io.vanillabp.cockpit.gui.api.v1.KwicResults;
import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserSearchResult;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskIds;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.UserTasksUpdateRequest;
import io.vanillabp.cockpit.tasklist.UserTaskAlreadyCompletedException;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.util.SearchQuery;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * Turns the tasklist requests of the GUI API into calls of {@link UserTaskService} and maps the
 * result back. What it leaves open is one question, asked once: which user tasks this view lets the
 * person making the request reach.
 *
 * <p>A subclass answers it in {@link #userTasksVisibleTo(UserDetails)} and is then done. The list,
 * the fulltext suggestions, opening one task and every action on one task are answered from that
 * one {@link UserTaskVisibility}, so a task the list would not show cannot be opened, claimed,
 * assigned or marked as read by guessing its id either. All of those answer HTTP 404 in that case,
 * the same answer an id which was never reported gets, so the answer itself tells nobody the task
 * exists.
 *
 * <p>{@link UserTaskVisibility} names the ready-made views the cockpit ships with. An application
 * which needs another one builds a {@code UserTaskVisibility} of its own rather than overriding the
 * methods below, which is the difference between describing who may see something and rewriting how
 * it is fetched.
 */
public abstract class AbstractUserTaskListGuiApiController implements OfficialTasklistApi {

	@Autowired
	protected UserContext userContext;

	@Autowired
	protected GuiApiMapper mapper;

	@Autowired
	protected PersonAndGroupApiMapper personAndGroupMapper;

	@Autowired
	protected PersonAndGroupMapper personAndGroupModelMapper;

	@Autowired
	protected UserDetailsProvider userDetailsProvider;

	@Autowired
	protected UserTaskService userTaskService;

	/**
	 * The user tasks this view lets the given user reach, which is the only thing a subclass has to
	 * decide. It is asked once per request and used for the list as well as for everything naming a
	 * single task.
	 */
	protected abstract UserTaskVisibility userTasksVisibleTo(
			final UserDetails currentUser);

	protected Page<io.vanillabp.cockpit.tasklist.model.UserTask> getUserTasks(
			final UserTaskVisibility visibility,
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp,
			final Collection<SearchQuery> searchQueries,
			final String sort,
			final boolean sortAscending,
			final UserTaskService.RetrieveItemsMode mode) {

		return userTaskService.getUserTasks(
				visibility,
				pageNumber,
				pageSize,
				initialTimestamp,
				searchQueries,
				sort,
				sortAscending,
				mode);

	}

    @Override
    public ResponseEntity<UserTasks> getUserTasks(
			final UserTasksRequest userTasksRequest,
            final OffsetDateTime initialTimestamp) {

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

		final var currentUser = userContext.getUserLoggedInDetails();

		final var userTasks = getUserTasks(
				userTasksVisibleTo(currentUser),
				userTasksRequest.getPageNumber(),
				userTasksRequest.getPageSize(),
				timestamp,
				mapper.toModel(userTasksRequest.getSearchQueries()),
				userTasksRequest.getSort(),
				userTasksRequest.getSortAscending(),
				userTasksRequest.getMode() != null
						? mapper.toModel(userTasksRequest.getMode())
						: UserTaskService.RetrieveItemsMode.All);

		return ResponseEntity.ok(mapper.toApi(userTasks, timestamp, currentUser.getId()));

	}

	protected Page<io.vanillabp.cockpit.tasklist.model.UserTask> getUserTasksUpdated(
			final UserTaskVisibility visibility,
			final int size,
			final Collection<String> knownUserTasksIds,
			final OffsetDateTime initialTimestamp,
			final Collection<SearchQuery> searchQueries,
			final String sort,
			final boolean sortAscending,
			final UserTaskService.RetrieveItemsMode mode) {

		return userTaskService.getUserTasksUpdated(
				visibility,
				size,
				knownUserTasksIds,
				initialTimestamp,
				searchQueries,
				sort,
				sortAscending,
				mode);

	}

	@Override
	public ResponseEntity<UserTasks> getUserTasksUpdate(
			final UserTasksUpdateRequest userTasksUpdateRequest,
			final OffsetDateTime initialTimestamp) {

		final var timestamp = initialTimestamp != null
				? initialTimestamp
				: OffsetDateTime.now();

		final var currentUser = userContext.getUserLoggedInDetails();

		final var userTasks = getUserTasksUpdated(
				userTasksVisibleTo(currentUser),
				userTasksUpdateRequest.getSize(),
				userTasksUpdateRequest.getKnownUserTasksIds(),
				timestamp,
				mapper.toModel(userTasksUpdateRequest.getSearchQueries()),
				userTasksUpdateRequest.getSort(),
				userTasksUpdateRequest.getSortAscending(),
				userTasksUpdateRequest.getMode() != null
						? mapper.toModel(userTasksUpdateRequest.getMode())
						: UserTaskService.RetrieveItemsMode.OpenTasks);

		return ResponseEntity.ok(mapper.toApi(userTasks, timestamp, currentUser.getId()));

	}

    protected List<io.vanillabp.cockpit.util.kwic.KwicResult> kwic(
            final UserTaskVisibility visibility,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query) {

        return userTaskService.kwic(visibility, endedSince, searchQueries, path, query);

    }

    @Override
    public ResponseEntity<KwicResults> getUserTaskKwicResults(
            final KwicRequest kwicRequest,
            final OffsetDateTime initialTimestamp,
            final String path,
            final String query) {

        final var effectivePath = StringUtils.hasText(path)
                ? path
                : "detailsFulltextSearch";

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        final var currentUser = userContext.getUserLoggedInDetails();

        final var searchQueries = Optional
                .ofNullable(kwicRequest.getSearchQueries())
                .orElse(List.of())
                .stream()
                .map(mapper::toModel)
                .toList();

        final var result =
                kwic(userTasksVisibleTo(currentUser), timestamp, searchQueries, effectivePath, query)
                .stream()
                .map(mapper::toApi)
                .toList();

        return ResponseEntity.ok(new KwicResults().result(result));

    }

	@Override
    public ResponseEntity<UserTask> getUserTask(
            final String userTaskId,
			final Boolean markAsRead) {

		final var currentUser = userContext.getUserLoggedInDetails();
		final var visibility = userTasksVisibleTo(currentUser);

		// the whole task shown to a person, which is what counts as having opened it
		final var found = userTaskService.getUserTask(visibility, userTaskId, true);
		if (found == null) {
			return ResponseEntity.notFound().build();
		}

		final var readAt = found.getReadAt(currentUser.getId());
		final io.vanillabp.cockpit.tasklist.model.UserTask userTask;
		if ((markAsRead == null)        // not required to
				|| !markAsRead          // mark as read or
				|| (readAt != null)) {  // already read by current user
			userTask = found;
		} else {                        // to be marked as read by current user
			userTask = userTaskService.markAsRead(visibility, found.getId(), currentUser.getId());
		}
		if (userTask == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(mapper.toApi(userTask, currentUser.getId()));

    }

	@Override
	public ResponseEntity<Void> markTaskAsRead(
			final String userTaskId,
			final Boolean unread) {

		final var currentUser = userContext.getUserLoggedInDetails();
		final var visibility = userTasksVisibleTo(currentUser);

		final var result = (unread != null) && unread
				? userTaskService.markAsUnread(visibility, userTaskId, currentUser.getId())
				: userTaskService.markAsRead(visibility, userTaskId, currentUser.getId());

		return result == null
				? ResponseEntity.notFound().build()
				: ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<Void> markTasksAsRead(
			final UserTaskIds userTaskIds,
			final Boolean unread) {

		final var currentUser = userContext.getUserLoggedInDetails();
		final var visibility = userTasksVisibleTo(currentUser);

		if ((unread != null) && unread) {
			userTaskService.markAsUnread(visibility, userTaskIds.getUserTaskIds(), currentUser.getId());
		} else {
			userTaskService.markAsRead(visibility, userTaskIds.getUserTaskIds(), currentUser.getId());
		}

		return ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<Void> claimTask(
			final String userTaskId,
			final Boolean unclaim) {

		final var currentUser = userContext.getUserLoggedInDetails();
		final var visibility = userTasksVisibleTo(currentUser);
		final var currentUserId = currentUser.getId();

		final var result = (unclaim != null) && unclaim
				? userTaskService.unclaimTask(visibility, currentUserId, userTaskId, currentUserId)
				: userTaskService.claimTask(
						visibility, userTaskId, personAndGroupModelMapper.toModelPerson(currentUser));

		return result == null
				? ResponseEntity.notFound().build()
				: ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<Void> claimTasks(
			final UserTaskIds userTaskIds,
			final Boolean unclaim) {

		final var currentUser = userContext.getUserLoggedInDetails();
		final var visibility = userTasksVisibleTo(currentUser);
		final var currentUserId = currentUser.getId();

		if ((unclaim != null) && unclaim) {
			userTaskService.unclaimTask(
					visibility, currentUserId, userTaskIds.getUserTaskIds(), currentUserId);
		} else {
			userTaskService.claimTask(
					visibility,
					userTaskIds.getUserTaskIds(),
					personAndGroupModelMapper.toModelPerson(currentUser));
		}

		return ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<Void> assignTask(
			final String userTaskId,
			final Boolean unassign,
			final String userId) {

		final var visibility = userTasksVisibleTo(userContext.getUserLoggedInDetails());

		final var result = (unassign != null) && unassign
				? userTaskService.unassignTask(visibility, userTaskId, userId)
				: userTaskService.assignTask(
						visibility, userTaskId, personAndGroupModelMapper.toModelPerson(userId));

		return result == null
				? ResponseEntity.notFound().build()
				: ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<Void> assignTasks(
			final UserTaskIds body,
			final Boolean unassign,
			final String userId) {

		final var visibility = userTasksVisibleTo(userContext.getUserLoggedInDetails());

		if ((unassign != null) && unassign) {
			userTaskService.unassignTask(visibility, body.getUserTaskIds(), userId);
		} else {
			userTaskService.assignTask(
					visibility, body.getUserTaskIds(), personAndGroupModelMapper.toModelPerson(userId));
		}

		return ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<UserTask> setFollowUpDate(
			final String userTaskId,
			final FollowUpDateRequest followUpDateRequest) {

		final var request = followUpDateRequest != null
				? followUpDateRequest
				: new FollowUpDateRequest();

		final var currentUser = userContext.getUserLoggedInDetails();

		final io.vanillabp.cockpit.tasklist.model.UserTask userTask;
		try {
			userTask = userTaskService.setFollowUpDate(
					userTasksVisibleTo(currentUser), userTaskId, request.getTimestamp());
		} catch (UserTaskAlreadyCompletedException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		if (userTask == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(mapper.toApi(userTask, currentUser.getId()));

	}

	@Override
	public ResponseEntity<UserSearchResult> findUsers(
			final String query,
			final Integer limit) {

		final var trimmedQuery = StringUtils.trimAllWhitespace(query);
		final Collection<UserDetails> users;
		if (!StringUtils.hasText(trimmedQuery) || (trimmedQuery.length() < 3)) {
			users = userDetailsProvider.getAllUsers();
		} else {
			users = userDetailsProvider.findUsers(trimmedQuery);
		}

		final var result = new UserSearchResult();
		result.setUsers(new ArrayList<>());
		users
				.stream()
				.limit(limit)
				.map(UserDetails::getId)
				.map(personAndGroupMapper::toApiPerson)
				.forEach(result::addUsersItem);

		return ResponseEntity.ok(result);

	}

}
