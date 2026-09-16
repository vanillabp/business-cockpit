package io.vanillabp.cockpit.config.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mongodb.WriteConcern;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties.Mode;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * What a cockpit is told about the write concern it writes MongoDB with, and which of it stops the
 * start.
 *
 * <p>Every assertion is about what makes a message worth reading: the value the cockpit really
 * writes with, the property to set and an example of a value. Nothing here asserts on wording
 * beyond that, so the texts stay free to improve.
 */
@ExtendWith(SuppressOutputExtension.class)
class WriteConcernCheckTest {

    /** What a connection which says nothing about write concerns hands over. */
    private static final WriteConcern NOTHING_ON_THE_CONNECTION = WriteConcern.ACKNOWLEDGED;

    private static final String EXAMPLE =
            "Example: " + CockpitConfiguration.MONGODB_WRITE_CONCERN + ": majority";

    private ListAppender<ILoggingEvent> recordedLog;

    @BeforeEach
    void recordWhatIsLogged() {

        recordedLog = new ListAppender<>();
        recordedLog.start();
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(WriteConcernCheck.class))
                .addAppender(recordedLog);

    }

    @AfterEach
    void stopRecording() {

        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(WriteConcernCheck.class))
                .detachAppender(recordedLog);

    }

    private String warnings() {

        return recordedLog
                .list
                .stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

    }

    private void report(
            final WriteConcern applied) {

        WriteConcernCheck.reportWhatTheCockpitWritesWith(
                applied, applied, NOTHING_ON_THE_CONNECTION, Mode.MONGODB_4_8);

    }

    private static MongoDbProperties configuredWith(
            final String writeConcern,
            final Boolean journal) {

        final var properties = new MongoDbProperties();
        properties.setWriteConcern(writeConcern);
        properties.setWriteConcernJournal(journal);
        return properties;

    }

    @Test
    void aMajorityIsWrittenWithoutAWord() {

        report(WriteConcern.MAJORITY);

        assertThat(warnings()).isEmpty();

    }

    @Test
    void aMajorityWithAJournalIsWrittenWithoutAWord() {

        report(WriteConcern.MAJORITY.withJournal(Boolean.TRUE).withWTimeout(5, TimeUnit.SECONDS));

        assertThat(warnings()).isEmpty();

    }

    @Test
    void aWriteOnlyThePrimaryHasIsAWarningNamingThePropertyAndAnExample() {

        report(WriteConcern.W1);

        assertThat(warnings())
                .contains("'w: 1'")
                .contains(CockpitConfiguration.MONGODB_WRITE_CONCERN)
                .contains(EXAMPLE);

    }

    /**
     * The value nobody configures and everybody ends up with: {@code ACKNOWLEDGED} leaves the 'w'
     * to the server, and a server answers as soon as the primary has the write.
     */
    @Test
    void leavingTheWriteConcernToTheServerIsTheSameWarning() {

        report(WriteConcern.ACKNOWLEDGED);

        assertThat(warnings())
                .contains("'w: 1'")
                .contains(EXAMPLE);

    }

    @Test
    void aJournalDoesNotMakeASingleNodeSafeAndTheWarningSaysSo() {

        report(WriteConcern.W1.withJournal(Boolean.TRUE));

        assertThat(warnings())
                .contains("'w: 1'")
                .contains("journal");

    }

    @Test
    void aNumberOfNodesTheCockpitCannotJudgeIsAWarningNamingTheNumber() {

        report(WriteConcern.W2);

        assertThat(warnings())
                .contains("'w: 2'")
                .contains(EXAMPLE);

    }

    @Test
    void aReplicaSetTagIsAWarningNamingTheTag() {

        report(new WriteConcern("eu-west"));

        assertThat(warnings())
                .contains("eu-west")
                .contains(EXAMPLE);

    }

    @Test
    void aWriteConcernNobodyAcknowledgesEndsTheStart() {

        assertThatThrownBy(() -> report(WriteConcern.UNACKNOWLEDGED))
                .isInstanceOf(WritesAreNotAcknowledgedException.class)
                .hasMessageContaining("'w: 0'")
                .hasMessageContaining(CockpitConfiguration.MONGODB_WRITE_CONCERN)
                .hasMessageContaining(EXAMPLE);

    }

    /**
     * A template which checks its write results raises an unacknowledged write to an acknowledged
     * one, so what is applied looks harmless. The start ends all the same, because an installation
     * which asked for 'w: 0' would otherwise run on a promise it never made.
     */
    @Test
    void aWriteConcernNobodyAcknowledgesEndsTheStartEvenWhereSpringDataCorrectsIt() {

        assertThatThrownBy(() -> WriteConcernCheck.reportWhatTheCockpitWritesWith(
                WriteConcern.UNACKNOWLEDGED,
                WriteConcern.ACKNOWLEDGED,
                NOTHING_ON_THE_CONNECTION,
                Mode.MONGODB_4_8))
                .isInstanceOf(WritesAreNotAcknowledgedException.class)
                .hasMessageContaining("'w: 0'");

    }

    /**
     * The one place people write a write concern into first, and the one place a cockpit which
     * checks its write results never reads.
     */
    @Test
    void aWriteConcernOnTheConnectionIsNamedBecauseTheCockpitDoesNotUseIt() {

        WriteConcernCheck.reportWhatTheCockpitWritesWith(
                null, WriteConcern.ACKNOWLEDGED, WriteConcern.MAJORITY, Mode.MONGODB_4_8);

        assertThat(warnings())
                .contains("The connection asks for 'w: majority'")
                .contains(CockpitConfiguration.MONGODB_WRITE_CONCERN);

    }

    /**
     * The value the database migration used to set on the cockpit's template, and the reason this
     * check exists. It asks for a journal and says nothing about nodes, so the template drops the
     * journal and writes with one node.
     */
    @Test
    void askingForAJournalWithoutNodesIsNamedBecauseTheJournalIsDropped() {

        final var configured = WriteConcernCheck
                .writeConcernConfigured(configuredWith("journaled", null))
                .orElseThrow();

        assertThat(configured.getWObject()).isNull();
        assertThat(configured.getJournal()).isTrue();

        WriteConcernCheck.reportWhatTheCockpitWritesWith(
                configured, WriteConcern.ACKNOWLEDGED, NOTHING_ON_THE_CONNECTION, Mode.MONGODB_4_8);

        assertThat(warnings())
                .contains("'w: 1'")
                .contains(CockpitConfiguration.MONGODB_WRITE_CONCERN_JOURNAL);

    }

    @Test
    void azureCosmosIsToldThatTheWarningIsAboutAnotherKindOfServer() {

        WriteConcernCheck.reportWhatTheCockpitWritesWith(
                WriteConcern.W1,
                WriteConcern.W1,
                NOTHING_ON_THE_CONNECTION,
                Mode.AZURE_COSMOS_MONGO_4_2);

        assertThat(warnings())
                .contains("Azure Cosmos DB")
                .contains("consistency level");

    }

    @Test
    void anUnconfiguredCockpitAsksForNothing() {

        assertThat(WriteConcernCheck.writeConcernConfigured(configuredWith(null, null)))
                .isEmpty();
        assertThat(warnings()).isEmpty();

    }

    @Test
    void aJournalWithoutANumberOfNodesIsDroppedAndTheWarningSaysSo() {

        assertThat(WriteConcernCheck.writeConcernConfigured(configuredWith("  ", Boolean.TRUE)))
                .isEmpty();

        assertThat(warnings())
                .contains(CockpitConfiguration.MONGODB_WRITE_CONCERN_JOURNAL)
                .contains(EXAMPLE);

    }

    @Test
    void aMajorityIsBuiltWithTheConfiguredTimeoutWhateverItsSpelling() {

        final var writeConcern = WriteConcernCheck
                .writeConcernConfigured(configuredWith("MAJORITY", Boolean.TRUE))
                .orElseThrow();

        assertThat(writeConcern.getWObject()).isEqualTo("majority");
        assertThat(writeConcern.getJournal()).isTrue();
        assertThat(writeConcern.getWTimeout(TimeUnit.SECONDS)).isEqualTo(5);

    }

    /**
     * The driver refuses a journal on a write nobody waits for, so the journal is left off and the
     * value reaches the check, which says in words what is wrong with it.
     */
    @Test
    void aWriteConcernNobodyAcknowledgesIsBuiltWithoutTheJournal() {

        final var writeConcern = WriteConcernCheck
                .writeConcernConfigured(configuredWith("0", Boolean.TRUE))
                .orElseThrow();

        assertThat(writeConcern.getWObject()).isEqualTo(0);
        assertThat(writeConcern.getJournal()).isNull();

    }

    @Test
    void aNumberIsBuiltAsANumberOfNodesAndAnythingElseAsATag() {

        assertThat(WriteConcernCheck
                .writeConcernConfigured(configuredWith("2", null))
                .orElseThrow()
                .getWObject())
                .isEqualTo(2);

        assertThat(WriteConcernCheck
                .writeConcernConfigured(configuredWith("eu-west", null))
                .orElseThrow()
                .getWObject())
                .isEqualTo("eu-west");

    }

}
