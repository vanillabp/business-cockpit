package io.vanillabp.cockpit.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Notifications form one group per business case, against a real MongoDB. A task of a process the
 * case started by a call activity counts for the case's process. A process with a workflow
 * aggregate of its own is a case of its own and keeps its own group. See decision 61 in the repository's DECISIONS.md.
 * <p>
 * Two places answer that question: {@link CaseProcess#of}, which the notification poller asks, and
 * {@link UserTaskService#getVisibleWorkflows}, which lists the groups on the page for notification
 * settings. Both are asked about the same tasks here, so the two cannot drift apart.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ATaskOfACalledProcessCountsForItsCaseTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-09T08:00:00Z");

    private static final String MODULE = "orders";

    private static final String CLERKS = "clerks";

    private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

    private static MongoClient mongoClient;

    private MongoTemplate mongoTemplate;

    @BeforeAll
    static void startMongoDb() {

        MONGODB.start();
        mongoClient = MongoClients.create(MONGODB.getConnectionString());

    }

    @AfterAll
    static void stopMongoDb() {

        if (mongoClient != null) {
            mongoClient.close();
        }
        MONGODB.stop();

    }

    /** A template with the converters of {@code MongoDbConfiguration}, in a database of its own. */
    @BeforeEach
    void aDatabaseOfItsOwn() {

        final var factory = new SimpleMongoClientDatabaseFactory(mongoClient, "cockpit-" + UUID.randomUUID());
        final var conversions = new MongoCustomConversions(List.of(
                new OffsetDateTimeReadConverter(),
                new OffsetDateTimeWriteConverter(),
                new BigDecimalReadConverter(),
                new BigDecimalWriteConverter()));
        final var mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        final var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        mongoTemplate = new MongoTemplate(factory, converter);

    }

    private void aCase(
            final String workflowId,
            final String bpmnProcessId) {

        final var workflow = new Workflow();
        workflow.setId(workflowId);
        workflow.setWorkflowModuleId(MODULE);
        workflow.setBpmnProcessId(bpmnProcessId);
        workflow.setCreatedAt(CREATED_AT);
        mongoTemplate.save(workflow);

    }

    private UserTask aTask(
            final String userTaskId,
            final String workflowId,
            final String bpmnProcessId,
            final String workflowTitle) {

        final var task = new UserTask();
        task.setId(userTaskId);
        task.setCreatedAt(CREATED_AT);
        task.setWorkflowModuleId(MODULE);
        task.setWorkflowId(workflowId);
        task.setBpmnProcessId(bpmnProcessId);
        task.setWorkflowTitle(Map.of("en", workflowTitle));
        final var clerks = new Group();
        clerks.setId(CLERKS);
        task.setCandidateGroups(List.of(clerks));
        return mongoTemplate.save(task);

    }

    private List<UserTask> theGroupsOnTheSettingsPage() {

        final var service = new UserTaskService();
        ReflectionTestUtils.setField(service, "mongoTemplate", mongoTemplate);
        // a clerk's view, as the page asks for the tasks a user may work on
        return service.getVisibleWorkflows(new UserTaskVisibility(
                false, false, null, null, List.of(CLERKS), null, null));

    }

    private static String keyOf(
            final UserTask group) {

        return group.getWorkflowModuleId() + "/" + group.getBpmnProcessId();

    }

    @Test
    @DisplayName("A task of a called process counts for the process of its case")
    void aTaskOfACalledProcessCountsForItsCase() {

        aCase("case-1", "order");
        final var ofTheCase = aTask("t1", "case-1", "order", "Order");
        final var ofTheCalledProcess = aTask("t2", "case-1", "delivery", "Delivery");

        final var cases = CaseProcess.of(mongoTemplate, List.of(ofTheCase, ofTheCalledProcess));
        assertThat(cases.apply(ofTheCase)).isEqualTo(new CaseProcess(MODULE, "order"));
        assertThat(cases.apply(ofTheCalledProcess)).isEqualTo(new CaseProcess(MODULE, "order"));

        final var groups = theGroupsOnTheSettingsPage();
        assertThat(groups).extracting(ATaskOfACalledProcessCountsForItsCaseTest::keyOf).containsExactly(MODULE + "/order");
        // the title of the case's own process names the group, whichever task MongoDB met first
        assertThat(groups.getFirst().getWorkflowTitle()).isEqualTo(Map.of("en", "Order"));

    }

    @Test
    @DisplayName("A called process with an aggregate of its own is a case of its own")
    void aProcessWithAnAggregateOfItsOwnIsACaseOfItsOwn() {

        // the workflow module files such a task under the case of its own aggregate, so the task
        // names a workflow of its own process
        aCase("case-1", "order");
        aCase("own-1", "complaint");
        final var ofTheCase = aTask("t1", "case-1", "order", "Order");
        final var ofItsOwnCase = aTask("t3", "own-1", "complaint", "Complaint");

        final var cases = CaseProcess.of(mongoTemplate, List.of(ofTheCase, ofItsOwnCase));
        assertThat(cases.apply(ofItsOwnCase)).isEqualTo(new CaseProcess(MODULE, "complaint"));
        assertThat(theGroupsOnTheSettingsPage())
                .extracting(ATaskOfACalledProcessCountsForItsCaseTest::keyOf)
                .containsExactlyInAnyOrder(MODULE + "/order", MODULE + "/complaint");

    }

    @Test
    @DisplayName("A task whose case the cockpit does not hold counts for its own process")
    void aTaskWithoutItsCaseCountsForItsOwnProcess() {

        // the report of the workflow has not arrived yet, or the module reports none
        final var task = aTask("t4", "not-reported", "delivery", "Delivery");

        assertThat(CaseProcess.of(mongoTemplate, List.of(task)).apply(task))
                .isEqualTo(new CaseProcess(MODULE, "delivery"));
        final var groups = theGroupsOnTheSettingsPage();
        assertThat(groups).extracting(ATaskOfACalledProcessCountsForItsCaseTest::keyOf).containsExactly(MODULE + "/delivery");
        assertThat(groups.getFirst().getWorkflowTitle()).isEqualTo(Map.of("en", "Delivery"));

    }

    @Test
    @DisplayName("Where only tasks of a called process are visible, one of them names the group")
    void onlyTasksOfACalledProcessStillFormTheGroupOfTheCase() {

        aCase("case-1", "order");
        aTask("t2", "case-1", "delivery", "Delivery");

        final var groups = theGroupsOnTheSettingsPage();
        assertThat(groups).extracting(ATaskOfACalledProcessCountsForItsCaseTest::keyOf).containsExactly(MODULE + "/order");
        assertThat(groups.getFirst().getWorkflowTitle()).isEqualTo(Map.of("en", "Delivery"));

    }

}
