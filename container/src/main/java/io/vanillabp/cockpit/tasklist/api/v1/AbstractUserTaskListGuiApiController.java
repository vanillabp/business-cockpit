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
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
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

public abstract class AbstractUserTaskListGuiApiController implements OfficialTasklistApi {

	@Autowired
	protected UserContext userContext;

	@Autowired
	protected GuiApiMapper mapper;

	@Autowired
	protected PersonAndGroupApiMapper personAndGroupMapper;

	@Autowired
	protected UserDetailsProvider userDetailsProvider;

	protected abstract Page<io.vanillabp.cockpit.tasklist.model.UserTask> getUserTasks(
			final UserDetails currentUser,
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp,
			final Collection<SearchQuery> searchQueries,
			final String sort,
			final boolean sortAscending,
			final UserTaskService.RetrieveItemsMode mode);

    @Override
    public ResponseEntity<UserTasks> getUserTasks(
			final UserTasksRequest userTasksRequest,
            final OffsetDateTime initialTimestamp) {

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

		final var currentUser = userContext.getUserLoggedInDetails();

		final var userTasks = getUserTasks(
				currentUser,
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

	public abstract Page<io.vanillabp.cockpit.tasklist.model.UserTask> getUserTasksUpdated(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final int size,
			final Collection<String> knownUserTasksIds,
			final OffsetDateTime initialTimestamp,
			final Collection<SearchQuery> searchQueries,
			final String sort,
			final boolean sortAscending,
			final UserTaskService.RetrieveItemsMode mode);

	@Override
	public ResponseEntity<UserTasks> getUserTasksUpdate(
			final UserTasksUpdateRequest userTasksUpdateRequest,
			final OffsetDateTime initialTimestamp) {

		final var timestamp = initialTimestamp != null
				? initialTimestamp
				: OffsetDateTime.now();

		final var currentUser = userContext.getUserLoggedInDetails();

		final var userTasks = getUserTasksUpdated(
				currentUser,
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

    protected abstract List<io.vanillabp.cockpit.util.kwic.KwicResult> kwic(
            final UserDetails currentUser,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query);

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

        final var result = kwic(currentUser, timestamp, searchQueries, effectivePath, query)
                .stream()
                .map(mapper::toApi)
                .toList();

        return ResponseEntity.ok(new KwicResults().result(result));

    }

    protected abstract io.vanillabp.cockpit.tasklist.model.UserTask getUserTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId);

	protected abstract io.vanillabp.cockpit.tasklist.model.UserTask markAsRead(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final boolean unread);

	protected abstract List<io.vanillabp.cockpit.tasklist.model.UserTask> markAsRead(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final boolean unread);

	@Override
    public ResponseEntity<UserTask> getUserTask(
            final String userTaskId,
			final Boolean markAsRead) {

		final var currentUser = userContext.getUserLoggedInDetails();

		final var found = getUserTask(currentUser, userTaskId);
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
			userTask = markAsRead(currentUser, found.getId(), false);
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

		final var result = markAsRead(currentUser, userTaskId, (unread != null) && unread);

		return result == null
				? ResponseEntity.notFound().build()
				: ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<Void> markTasksAsRead(
			final UserTaskIds userTaskIds,
			final Boolean unread) {

		final var currentUser = userContext.getUserLoggedInDetails();

		markAsRead(currentUser, userTaskIds.getUserTaskIds(), (unread != null) && unread);

		return ResponseEntity.ok().build();

	}

	protected abstract io.vanillabp.cockpit.tasklist.model.UserTask claimTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final boolean unclaim);

	protected abstract List<io.vanillabp.cockpit.tasklist.model.UserTask> claimTasks(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final boolean unclaim);

	@Override
	public ResponseEntity<Void> claimTask(
			final String userTaskId,
			final Boolean unclaim) {

		final var currentUser = userContext.getUserLoggedInDetails();

		final var result = claimTask(currentUser, userTaskId, (unclaim != null) && unclaim);

		return result == null
				? ResponseEntity.notFound().build()
				: ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<Void> claimTasks(
			final UserTaskIds userTaskIds,
			final Boolean unclaim) {

		final var currentUser = userContext.getUserLoggedInDetails();

		claimTasks(currentUser, userTaskIds.getUserTaskIds(), (unclaim != null) && unclaim);

		return ResponseEntity.ok().build();

	}

	protected abstract io.vanillabp.cockpit.tasklist.model.UserTask assignTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final String userId,
			final boolean unassign);

	protected abstract List<io.vanillabp.cockpit.tasklist.model.UserTask> assignTasks(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final String userId,
			final boolean unassign);

	@Override
	public ResponseEntity<Void> assignTask(
			final String userTaskId,
			final Boolean unassign,
			final String userId) {

		final var currentUser = userContext.getUserLoggedInDetails();

		final var result = assignTask(currentUser, userTaskId, userId, (unassign != null) && unassign);

		return result == null
				? ResponseEntity.notFound().build()
				: ResponseEntity.ok().build();

	}

	@Override
	public ResponseEntity<Void> assignTasks(
			final UserTaskIds body,
			final Boolean unassign,
			final String userId) {

		final var currentUser = userContext.getUserLoggedInDetails();

		assignTasks(currentUser, body.getUserTaskIds(), userId, (unassign != null) && unassign);

		return ResponseEntity.ok().build();

	}

	protected abstract io.vanillabp.cockpit.tasklist.model.UserTask setFollowUpDate(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final OffsetDateTime followUpDate);

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
			userTask = setFollowUpDate(currentUser, userTaskId, request.getTimestamp());
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
