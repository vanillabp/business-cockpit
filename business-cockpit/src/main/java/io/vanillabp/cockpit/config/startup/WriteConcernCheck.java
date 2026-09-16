package io.vanillabp.cockpit.config.startup;

import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.MONGODB_WRITE_CONCERN;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.MONGODB_WRITE_CONCERN_JOURNAL;

import com.mongodb.WriteConcern;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties.Mode;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Builds the write concern the cockpit was configured with, and says on every start what the
 * cockpit really writes with.
 * <p>
 * The cockpit used to get a write concern by accident. The MongoDB changeset library set
 * {@code JOURNALED} on the application's template while it migrated and never took it back, so the
 * cockpit ran for years with a promise nobody had written down. The library gives the template back
 * the way it found it now, which leaves the cockpit to ask for what it needs.
 * <p>
 * What it needs, and what goes wrong with the rest, is written down in
 * {@code business-cockpit/README.md}. In short: a write which only the primary of a replica set has
 * is lost when that primary steps down, and the cockpit has answered the BPMS adapter by then that
 * the user task is stored. A write nobody acknowledges at all cannot be checked for a concurrent
 * change, which every versioned document of the cockpit depends on, so that one ends the start.
 */
public final class WriteConcernCheck {

    private static final Logger logger = LoggerFactory.getLogger(WriteConcernCheck.class);

    private static final String MAJORITY = "majority";

    /**
     * The block every message ends with: the property to set, what it decides, and the line to copy
     * into the application's configuration. It is the layout {@code StartupConfigurationCheck} uses
     * for a missing value, because a developer should recognise it.
     */
    private static final String WHAT_TO_SET =
            "  " + MONGODB_WRITE_CONCERN + "\n"
            + "      How many nodes of the replica set have to have a write before the cockpit is "
            + "told it is stored. 'majority' is the one value which survives a failover of the "
            + "primary, whatever the size of the replica set is.\n"
            + "      Example: " + MONGODB_WRITE_CONCERN + ": " + MAJORITY;

    private WriteConcernCheck() {
    }

    /**
     * The write concern the configuration asks for, or empty where it asks for nothing. Empty is
     * not the same as "write with whatever is there": a cockpit which checks its write results
     * falls back to a single acknowledgement, and {@link #reportWhatTheCockpitWritesWith} says so.
     *
     * @param properties the {@code mongodb.*} settings of the application
     */
    public static Optional<WriteConcern> writeConcernConfigured(
            final MongoDbProperties properties) {

        final var configured = properties.getWriteConcern();
        if (!StringUtils.hasText(configured)) {
            warnAboutAJournalWithoutANumberOfNodes(properties);
            return Optional.empty();
        }

        var writeConcern = asWriteConcern(configured.trim());
        // the journal is left off a write nobody waits for. The driver refuses that combination
        // with 'journal is false when w is 0', and such a configuration is meant to reach
        // reportWhatTheCockpitWritesWith, which says what is wrong with it in words
        if (properties.getWriteConcernJournal() != null && !isUnacknowledged(writeConcern)) {
            writeConcern = writeConcern.withJournal(properties.getWriteConcernJournal());
        }
        return Optional.of(writeConcern.withWTimeout(
                Duration.parse(properties.getUseTimeout()).get(ChronoUnit.SECONDS),
                TimeUnit.SECONDS));

    }

    /**
     * Reads the configured value the way MongoDB spells a 'w': a number of nodes, the word
     * 'majority', or the name of a tag set defined in the replica set configuration.
     * <p>
     * The names the driver knows are read first, in any spelling, so that 'MAJORITY' is a majority
     * and not a tag nobody defined. It also catches 'journaled', which is what the database
     * migration used to set and therefore the value somebody may copy. That one says nothing about
     * a number of nodes, and {@link #whatTheConfigurationAsksFor} is what tells them.
     */
    private static WriteConcern asWriteConcern(
            final String configured) {

        final var knownToTheDriver = WriteConcern.valueOf(configured);
        if (knownToTheDriver != null) {
            return knownToTheDriver;
        }
        try {
            return new WriteConcern(Integer.parseInt(configured));
        } catch (NumberFormatException e) {
            return new WriteConcern(configured);
        }

    }

    /**
     * A journal flag alone is not a write concern. MongoDB's own {@code JOURNALED} is exactly that
     * mistake in the driver: it says 'j: true' and leaves the 'w' open, and a template which checks
     * its write results replaces the whole thing with a plain acknowledged write, journal flag
     * included. So the flag is only read together with a number of nodes, and saying so is worth a
     * line.
     */
    private static void warnAboutAJournalWithoutANumberOfNodes(
            final MongoDbProperties properties) {

        if (properties.getWriteConcernJournal() == null) {
            return;
        }

        logger.warn("""
                '{}' is set, but '{}' is not, so the journal flag is dropped. A journal flag on its \
                own is not a write concern. Set both:

                {}""",
                MONGODB_WRITE_CONCERN_JOURNAL, MONGODB_WRITE_CONCERN, WHAT_TO_SET);

    }

    /**
     * Judges what the cockpit really writes with. Where the setting can lose a report it is a
     * warning, because an installation may know its database. Where the cockpit cannot work it ends
     * the start.
     *
     * @param configured what the application asked for, or {@code null} where it asked for nothing
     * @param applied what a write of the cockpit's template promises, after the template, its
     *      resolver and the connection have all had their say
     * @param ofTheConnection what the connection string or the {@code MongoClient} asks for, which
     *      a template checking its write results never reaches
     * @param mode which kind of server the cockpit talks to
     */
    public static void reportWhatTheCockpitWritesWith(
            final WriteConcern configured,
            final WriteConcern applied,
            final WriteConcern ofTheConnection,
            final Mode mode) {

        // what was asked for is refused as well as what would be written with. A MongoTemplate
        // which checks its write results turns an unacknowledged write into an acknowledged one
        // behind the application's back, and an installation which asked for 'w: 0' would run on a
        // promise it never made without a word being said about it
        if (isUnacknowledged(configured)) {
            throw writesAreNotAcknowledged(configured);
        }
        if (isUnacknowledged(applied)) {
            throw writesAreNotAcknowledged(applied);
        }

        final var w = applied.getWObject();
        if (MAJORITY.equals(w)) {
            return;
        }

        logger.warn("{}\n\n{}{}{}{}",
                describe(w),
                WHAT_TO_SET,
                whatTheConfigurationAsksFor(configured),
                whatTheConnectionAsksFor(applied, ofTheConnection),
                whatAzureCosmosMakesOfIt(mode));

    }

    /**
     * Whether a write concern waits for nobody. This is the rule {@code MongoTemplate} applies when
     * it decides whether it can read the result of a write, and not
     * {@link WriteConcern#isAcknowledged()}. The driver counts 'w: 0' plus a journal as
     * acknowledged, while the server rejects that combination and the template still has no result
     * to read.
     */
    private static boolean isUnacknowledged(
            final WriteConcern writeConcern) {

        return writeConcern != null
                && writeConcern.getWObject() instanceof Number nodes
                && nodes.intValue() < 1;

    }

    /**
     * What is wrong with a 'w' which is neither unacknowledged nor a majority.
     */
    private static String describe(
            final Object w) {

        if (w == null || Integer.valueOf(1).equals(w)) {
            // ACKNOWLEDGED leaves the 'w' to the server, and a server answers as soon as the
            // primary has the write. So both spellings mean the same thing to the reader.
            return """
                    The Business Cockpit writes to MongoDB with 'w: 1', so a write counts as stored \
                    once the primary alone has it. A primary which steps down before another node \
                    has that write takes it with it. The cockpit has answered the BPMS adapter by \
                    then that the user task is stored, and the task never appears. A journal does \
                    not help against this: it protects the one node against a crash, not the \
                    replica set against a failover.""";
        }
        if (w instanceof Number nodes) {
            return """
                    The Business Cockpit writes to MongoDB with 'w: %s'. Whether %s nodes are a \
                    majority of your replica set depends on how many nodes it has, and the cockpit \
                    cannot see that. Below a majority a write is lost when the primary steps down, \
                    and the user task the cockpit has already confirmed to the BPMS adapter never \
                    appears.""".formatted(nodes, nodes);
        }
        return """
                The Business Cockpit writes to MongoDB with the write concern tag '%s'. Which nodes \
                that tag stands for is written in the configuration of the replica set, so the \
                cockpit cannot tell whether it is a majority. Below a majority a write is lost when \
                the primary steps down, and the user task the cockpit has already confirmed to the \
                BPMS adapter never appears.""".formatted(w);

    }

    /**
     * The trap this whole check came out of: a value which asks for a journal and leaves the number
     * of nodes open. MongoDB calls it 'journaled', and it is what the database migration used to
     * set on the cockpit's template. Naming it matters, because from where the reader sits the
     * property is set and the journal is asked for.
     */
    private static String whatTheConfigurationAsksFor(
            final WriteConcern configured) {

        if (configured == null
                || configured.getWObject() != null
                || !Boolean.TRUE.equals(configured.getJournal())) {
            return "";
        }

        return """


                '%s' asks for a journal and leaves the number of nodes open, which is what MongoDB \
                calls 'journaled'. A template which checks the result of every write cannot keep \
                such a value and drops the journal with it. Write the number of nodes here, and \
                ask for the journal in '%s'."""
                .formatted(MONGODB_WRITE_CONCERN, MONGODB_WRITE_CONCERN_JOURNAL);

    }

    /**
     * The one place people write a write concern into first, and the one place it does not work.
     * Worth naming, because from where they sit the value is configured.
     */
    private static String whatTheConnectionAsksFor(
            final WriteConcern applied,
            final WriteConcern ofTheConnection) {

        if (ofTheConnection == null
                || ofTheConnection.isServerDefault()
                || ofTheConnection.equals(applied)) {
            return "";
        }

        return """


                The connection asks for 'w: %s', and the cockpit does not write with it. A \
                MongoTemplate which checks the result of every write, and this one does, replaces \
                the write concern of the connection with an acknowledged write. So set '%s' rather \
                than writing the concern into the connection string."""
                .formatted(ofTheConnection.getWObject(), MONGODB_WRITE_CONCERN);

    }

    /**
     * Azure Cosmos DB for MongoDB speaks the MongoDB protocol but is not a replica set, so the
     * paragraph above describes a server the installation does not have. What it does have is said
     * here, and what could not be decided is left to the documentation rather than guessed.
     */
    private static String whatAzureCosmosMakesOfIt(
            final Mode mode) {

        if (mode != Mode.AZURE_COSMOS_MONGO_4_2) {
            return "";
        }

        return """


                This cockpit talks to Azure Cosmos DB for MongoDB. Cosmos DB replicates on its own \
                terms, and how durable a write is comes from the consistency level of the account \
                rather than from the write concern the driver sends. Read the paragraph above as \
                being about MongoDB, and check the consistency level of the account.""";

    }

    private static WritesAreNotAcknowledgedException writesAreNotAcknowledged(
            final WriteConcern applied) {

        return new WritesAreNotAcknowledgedException("""
                The Business Cockpit cannot start. It would write to MongoDB with 'w: %s', so \
                nobody acknowledges a write and nothing says whether it happened. The cockpit reads \
                the result of every write to notice that somebody else changed the same user task, \
                workflow, user or notification lease first. An unacknowledged write carries no such \
                result, so saving any of those documents ends in an error."""
                .formatted(applied.getWObject()),
                """
                Set a write concern which waits for at least one node:

                %s

                Then start again.""".formatted(WHAT_TO_SET));

    }

}
