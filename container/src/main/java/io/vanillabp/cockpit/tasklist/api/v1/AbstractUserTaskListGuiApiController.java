package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import io.vanillabp.cockpit.gui.api.v1.KwicRequest;
import io.vanillabp.cockpit.gui.api.v1.KwicResults;
import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserSearchResult;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskIds;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.UserTasksUpdateRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class AbstractUserTaskListGuiApiController implements OfficialTasklistApi {

	@Autowired
	protected ReactiveUserContext userContext;

	@Autowired
	protected GuiApiMapper mapper;

	@Autowired
	protected PersonAndGroupApiMapper personAndGroupMapper;

	@Autowired
	protected UserDetailsProvider userDetailsProvider;
	
	protected abstract Mono<Page<io.vanillabp.cockpit.tasklist.model.UserTask>> getUserTasks(
			final UserDetails currentUser,
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp,
			final Collection<SearchQuery> searchQueries,
			final String sort,
			final boolean sortAscending,
			final UserTaskService.RetrieveItemsMode mode);

    @Override
    public Mono<ResponseEntity<UserTasks>> getUserTasks(
			final Mono<UserTasksRequest> userTasksRequest,
            final OffsetDateTime initialTimestamp,
            final ServerWebExchange exchange) {
		
        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

		return Mono.zip(
						userContext.getUserLoggedInDetailsAsMono(),
						userTasksRequest)
				.flatMap(entry -> getUserTasks(
						entry.getT1(),
						entry.getT2().getPageNumber(),
						entry.getT2().getPageSize(),
						timestamp,
						mapper.toModel(entry.getT2().getSearchQueries()),
						entry.getT2().getSort(),
						entry.getT2().getSortAscending(),
						entry.getT2().getMode() != null ? mapper.toModel(entry.getT2().getMode()): UserTaskService.RetrieveItemsMode.All)
				.map(userTasks -> mapper.toApi(userTasks, timestamp, entry.getT1().getId())))
				.map(ResponseEntity::ok);

	}

	public abstract Mono<Page<io.vanillabp.cockpit.tasklist.model.UserTask>> getUserTasksUpdated(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final int size,
			final Collection<String> knownUserTasksIds,
			final OffsetDateTime initialTimestamp,
			final Collection<SearchQuery> searchQueries,
			final String sort,
			final boolean sortAscending);

	@Override
	public Mono<ResponseEntity<UserTasks>> getUserTasksUpdate(
			final Mono<UserTasksUpdateRequest> userTasksUpdateRequest,
			final OffsetDateTime initialTimestamp,
			final ServerWebExchange exchange) {

		return userContext
				.getUserLoggedInDetailsAsMono()
				.flatMap(user -> userTasksUpdateRequest
						.zipWhen(update -> Mono.just(initialTimestamp != null
								? initialTimestamp
								: OffsetDateTime.now()))
						.flatMap(entry -> Mono.zip(
								getUserTasksUpdated(
										user,
										entry.getT1().getSize(),
										entry.getT1().getKnownUserTasksIds(),
										entry.getT2(),
										mapper.toModel(entry.getT1().getSearchQueries()),
										entry.getT1().getSort(),
										entry.getT1().getSortAscending()),
								Mono.just(entry.getT2())))
						.map(entry -> mapper.toApi(entry.getT1(), entry.getT2(), user.getId()))
						.map(ResponseEntity::ok)
						.switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()))
				);
            
	}

    protected abstract Flux<io.vanillabp.cockpit.util.kwic.KwicResult> kwic(
            final UserDetails currentUser,
            final OffsetDateTime endedSince,
            final List<SearchQuery> searchQueries,
            final String path,
            final String query);

    @Override
    public Mono<ResponseEntity<KwicResults>> getUserTaskKwicResults(
            Mono<KwicRequest> kwicRequest,
            OffsetDateTime initialTimestamp,
            String path,
            String query,
            ServerWebExchange exchange) {

        final var effectivePath = StringUtils.hasText(path)
                ? path
                : "detailsFulltextSearch";

        final var timestamp = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        return Mono.zip(
                           userContext.getUserLoggedInDetailsAsMono(),
                           kwicRequest)
                   .flatMapMany(entry -> {
                       final var searchQueries = Optional.ofNullable(
                                                                 entry.getT2().getSearchQueries())
                                                         .orElse(List.of())
                                                         .stream()
                                                         .map(mapper::toModel)
                                                         .toList();
                       return kwic(entry.getT1(), timestamp, searchQueries, effectivePath, query);
                   })
                   .map(mapper::toApi)
                   .collectList()
                   .map(result -> new KwicResults().result(result))
                   .map(ResponseEntity::ok);
    }

    protected abstract Mono<io.vanillabp.cockpit.tasklist.model.UserTask> getUserTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId);

	protected abstract Mono<io.vanillabp.cockpit.tasklist.model.UserTask> markAsRead(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final boolean unread);

	protected abstract Flux<io.vanillabp.cockpit.tasklist.model.UserTask> markAsRead(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final boolean unread);

	@Override
    public Mono<ResponseEntity<UserTask>> getUserTask(
            final String userTaskId,
			final Boolean markAsRead,
            final ServerWebExchange exchange) {

		final var taskAndUser = userContext
				.getUserLoggedInDetailsAsMono()
				.flatMap(user -> Mono.zip(
						getUserTask(user, userTaskId),
						Mono.just(user)));

        return taskAndUser
				.flatMap(tnu -> {
					final var readAt = tnu.getT1().getReadAt(tnu.getT2().getId());
					final Mono<io.vanillabp.cockpit.tasklist.model.UserTask> userTask;
					if ((markAsRead == null)        // not required to
							|| !markAsRead          // mark as read or
							|| (readAt != null)) {  // already read by current user
						userTask = Mono.just(tnu.getT1());
					} else {                        // to be marked as read by current user
						userTask = markAsRead(tnu.getT2(), tnu.getT1().getId(), false);
					}
					return Mono.zip(userTask, Mono.just(tnu.getT2()));
				})
				.map(tnu -> mapper.toApi(tnu.getT1(), tnu.getT2().getId())
				)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

    }

	@Override
	public Mono<ResponseEntity<Void>> markTaskAsRead(
			final String userTaskId,
			final Boolean unread,
			final ServerWebExchange exchange) {

		final var currentUser = userContext
				.getUserLoggedInDetailsAsMono();

		final Mono<io.vanillabp.cockpit.tasklist.model.UserTask> result;
		if ((unread != null) && unread) {
			result = currentUser
					.flatMap(user -> markAsRead(user, userTaskId, true));
		} else {
			result = currentUser
					.flatMap(user -> markAsRead(user, userTaskId, false));

		}

		return result
                .map(userTask -> ResponseEntity.ok().<Void>build())
				.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

	}

	@Override
	public Mono<ResponseEntity<Void>> markTasksAsRead(
			final Mono<UserTaskIds> userTaskIds,
			final Boolean unread,
			final ServerWebExchange exchange) {

		final var currentUser = userContext
				.getUserLoggedInDetailsAsMono();

		final Mono<List<io.vanillabp.cockpit.tasklist.model.UserTask>> result;
		final var inputData = Mono
				.zip(currentUser, userTaskIds);
		if ((unread != null) && unread) {
			result = inputData.flatMap(tuple -> markAsRead(
					tuple.getT1(),
					tuple.getT2().getUserTaskIds(),
					true).collectList());
		} else {
			result = inputData.flatMap(tuple -> markAsRead(
					tuple.getT1(),
					tuple.getT2().getUserTaskIds(),
					false).collectList());
		}

		return result
				.map(userTasks -> ResponseEntity.ok().<Void>build())
				.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

	}

	protected abstract Mono<io.vanillabp.cockpit.tasklist.model.UserTask> claimTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final boolean unclaim);

	protected abstract Flux<io.vanillabp.cockpit.tasklist.model.UserTask> claimTasks(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final boolean unclaim);

	@Override
	public Mono<ResponseEntity<Void>> claimTask(
			final String userTaskId,
			final Boolean unclaim,
			final ServerWebExchange exchange) {

		final var currentUser = userContext
				.getUserLoggedInDetailsAsMono();

		final Mono<io.vanillabp.cockpit.tasklist.model.UserTask> result;
		if ((unclaim != null) && unclaim) {
			result = currentUser
					.flatMap(user -> claimTask(user, userTaskId, true));
		} else {
			result = currentUser
					.flatMap(user -> claimTask(user, userTaskId, false));

		}

		return result
				.map(userTask -> ResponseEntity.ok().<Void>build())
				.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

	}

	@Override
	public Mono<ResponseEntity<Void>> claimTasks(
			final Mono<UserTaskIds> userTaskIds,
			final Boolean unclaim,
			final ServerWebExchange exchange) {

		final var currentUser = userContext
				.getUserLoggedInDetailsAsMono();

		final Mono<List<io.vanillabp.cockpit.tasklist.model.UserTask>> result;
		final var inputData = Mono
				.zip(currentUser, userTaskIds);
		if ((unclaim != null) && unclaim) {
			result = inputData.flatMap(tuple -> claimTasks(
					tuple.getT1(),
					tuple.getT2().getUserTaskIds(),
					true).collectList());
		} else {
			result = inputData.flatMap(tuple -> claimTasks(
					tuple.getT1(),
					tuple.getT2().getUserTaskIds(),
					false).collectList());
		}

		return result
				.map(userTasks -> ResponseEntity.ok().<Void>build())
				.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

	}

	protected abstract Mono<io.vanillabp.cockpit.tasklist.model.UserTask> assignTask(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final String userTaskId,
			final String userId,
			final boolean unassign);

	protected abstract Flux<io.vanillabp.cockpit.tasklist.model.UserTask> assignTasks(
			final io.vanillabp.cockpit.commons.security.usercontext.UserDetails currentUser,
			final List<String> userTaskIds,
			final String userId,
			final boolean unassign);

	@Override
	public Mono<ResponseEntity<Void>> assignTask(
			final String userTaskId,
			final Boolean unassign,
			final String userId,
			final ServerWebExchange exchange) {

		final var currentUser = userContext
				.getUserLoggedInDetailsAsMono();

		final Mono<io.vanillabp.cockpit.tasklist.model.UserTask> result;
		if ((unassign != null) && unassign) {
			result = currentUser
					.flatMap(user -> assignTask(user, userTaskId, userId, true));
		} else {
			result = currentUser
					.flatMap(user -> assignTask(user, userTaskId, userId, false));

		}

		return result
				.map(userTask -> ResponseEntity.ok().<Void>build())
				.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

	}

	@Override
	public Mono<ResponseEntity<Void>> assignTasks(
			final Mono<UserTaskIds> body,
			final Boolean unassign,
			final String userId,
			final ServerWebExchange exchange) {

		final var currentUser = userContext
				.getUserLoggedInDetailsAsMono();

		final Mono<List<io.vanillabp.cockpit.tasklist.model.UserTask>> result;
		final var inputData = Mono
				.zip(currentUser, body);
		if ((unassign != null) && unassign) {
			result = inputData.flatMap(tuple -> assignTasks(
					tuple.getT1(),
					tuple.getT2().getUserTaskIds(),
					userId,
					true).collectList());
		} else {
			result = inputData.flatMap(tuple -> assignTasks(
					tuple.getT1(),
					tuple.getT2().getUserTaskIds(),
					userId,
					false).collectList());
		}

		return result
				.map(userTasks -> ResponseEntity.ok().<Void>build())
				.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));

	}

	@Override
	public Mono<ResponseEntity<UserSearchResult>> findUsers(
			final String query,
			final Integer limit,
			final ServerWebExchange exchange) {

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

		return Mono.just(ResponseEntity.ok(result));

	}

}
