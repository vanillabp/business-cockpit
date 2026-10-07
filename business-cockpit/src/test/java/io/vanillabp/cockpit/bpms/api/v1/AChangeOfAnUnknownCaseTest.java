package io.vanillabp.cockpit.bpms.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

/**
 * What version 1 of the API does with a change of a case the cockpit was never told was created.
 * <p>
 * A workflow module does not always report the start of a case. Some report a case first when it
 * changes, for example a case which never had a user task. The tests drive the REST ingress with
 * the generated mapper and the real service, and keep the cases in memory instead of in MongoDB.
 */
@ExtendWith(SuppressOutputExtension.class)
class AChangeOfAnUnknownCaseTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-11T08:00:00Z");

    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-09-11T09:00:00Z");

    private final Map<String, Workflow> workflows = new HashMap<>();

    private BpmsApiController bpmsApi;

    @BeforeEach
    void setUp() throws Exception {

        final var workflowMapper = new WorkflowMapperV1Impl();
        set(WorkflowMapper.class, workflowMapper, "personAndGroupMapper", mock(PersonAndGroupMapper.class));

        final var repository = mock(WorkflowRepository.class);
        when(repository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(workflows.get(invocation.getArgument(0))));
        when(repository.save(any(Workflow.class))).thenAnswer(invocation -> {
            final Workflow workflow = invocation.getArgument(0);
            workflows.put(workflow.getId(), workflow);
            return workflow;
        });

        final var workflowlistService = new WorkflowlistService();
        set(WorkflowlistService.class, workflowlistService, "workflowRepository", repository);
        set(WorkflowlistService.class, workflowlistService, "logger", mock(Logger.class));
        set(WorkflowlistService.class, workflowlistService, "mongoTemplate", mock(MongoTemplate.class));

        bpmsApi = new BpmsApiController();
        set(BpmsApiController.class, bpmsApi, "workflowMapper", workflowMapper);
        set(BpmsApiController.class, bpmsApi, "workflowlistService", workflowlistService);

    }

    private static void set(
            final Class<?> declaringClass,
            final Object target,
            final String field,
            final Object value) throws Exception {

        final var declaredField = declaringClass.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);

    }

    private static WorkflowCreatedOrUpdatedEvent workflowReported(
            final OffsetDateTime timestamp,
            final String customer) {

        return new WorkflowCreatedOrUpdatedEvent()
                .id("event-" + customer)
                .workflowId("workflow-1")
                .timestamp(timestamp)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .title(Map.of("en", "A ride"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT")
                .details(new LinkedHashMap<>(Map.of("customer", customer)))
                .detailsFulltextSearch(customer);

    }

    @Test
    void aChangeOfACaseTheCockpitNeverSawCreatesIt() {

        final var answer = bpmsApi.workflowUpdatedEvent("workflow-1", workflowReported(CHANGED_AT, "Berta"));

        assertEquals(HttpStatus.OK, answer.getStatusCode());
        final var stored = workflows.get("workflow-1");
        assertNotNull(stored, "the change of an unknown case has to create it");
        assertEquals(CHANGED_AT, stored.getCreatedAt());
        assertEquals(CHANGED_AT, stored.getLatestEventAt());
        assertNull(stored.getEndedAt());
        assertEquals("ride", stored.getBpmnProcessId());
        assertEquals(Map.of("customer", "Berta"), stored.getDetails());

    }

    /**
     * The change gave the case the time of the change as its start. A creation which was only late
     * knows when the case began, so it corrects the start. The rest stays as the younger change
     * stored it.
     */
    @Test
    void aCreationArrivingAfterTheChangeWhichCreatedTheCaseCorrectsItsStart() {

        bpmsApi.workflowUpdatedEvent("workflow-1", workflowReported(CHANGED_AT, "Berta"));
        bpmsApi.workflowCreatedEvent(workflowReported(CREATED_AT, "Anna"));

        final var stored = workflows.get("workflow-1");
        assertEquals(CREATED_AT, stored.getCreatedAt());
        assertEquals(CHANGED_AT, stored.getLatestEventAt());
        assertEquals(Map.of("customer", "Berta"), stored.getDetails());

    }

    /** A creation whose start is not earlier than the stored one changes nothing. */
    @Test
    void aCreationNoEarlierThanTheChangeWhichCreatedTheCaseChangesNothing() {

        bpmsApi.workflowUpdatedEvent("workflow-1", workflowReported(CHANGED_AT, "Berta"));
        bpmsApi.workflowCreatedEvent(workflowReported(CHANGED_AT, "Anna"));

        final var stored = workflows.get("workflow-1");
        assertEquals(CHANGED_AT, stored.getCreatedAt());
        assertEquals(Map.of("customer", "Berta"), stored.getDetails());

    }

}
