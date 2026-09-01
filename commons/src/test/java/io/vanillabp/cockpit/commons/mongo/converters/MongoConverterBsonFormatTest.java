package io.vanillabp.cockpit.commons.mongo.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * The BSON types the cockpit stores, pinned down for the Spring Data MongoDB 5 upgrade (T18).
 *
 * <p>{@code BigDecimal} and {@code OffsetDateTime} have no canonical BSON representation, so the cockpit
 * brings four converters of its own: {@code BigDecimal} goes to {@code Decimal128}, {@code OffsetDateTime}
 * to a BSON date, with {@code OffsetDateTime.MAX} mapped to {@code Long.MAX_VALUE} as an "open end"
 * sentinel. Spring Data ships defaults for both types too, and a default that wins over a custom converter
 * would change the stored type without any error - documents written before the upgrade would then be
 * unreadable, or new ones unreadable for older cockpit versions.
 *
 * <p>The test therefore does not call the converters directly. It builds a {@link MappingMongoConverter}
 * the way Spring Data does, registers the four converters through {@link MongoCustomConversions}, and
 * checks what actually ends up in the {@link Document} - which is the only thing the driver sends.
 */
class MongoConverterBsonFormatTest {

    public static class Sample {

        public String id;

        public BigDecimal amount;

        public OffsetDateTime dueDate;

        public OffsetDateTime openEnd;

    }

    private final CustomConversions customConversions = new MongoCustomConversions(List.of(
            new BigDecimalReadConverter(),
            new BigDecimalWriteConverter(),
            new OffsetDateTimeReadConverter(),
            new OffsetDateTimeWriteConverter()));

    private MappingMongoConverter converter() {

        final var mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();

        final var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        converter.setCustomConversions(customConversions);
        converter.afterPropertiesSet();
        return converter;

    }

    private Sample sample() {

        final var sample = new Sample();
        sample.id = "task-1";
        sample.amount = new BigDecimal("1234.50");
        sample.dueDate = OffsetDateTime.of(2026, 7, 31, 12, 34, 56, 0, ZoneOffset.ofHours(2));
        sample.openEnd = OffsetDateTime.MAX;
        return sample;

    }

    private Document written() {

        final var document = new Document();
        converter().write(sample(), document);
        return document;

    }

    @Test
    void bigDecimalIsStoredAsDecimal128() {

        final var amount = written().get("amount");

        assertThat(amount)
                .as("a BigDecimal stored as Double would lose precision, as a String it would stop being "
                        + "comparable in queries")
                .isInstanceOf(Decimal128.class);
        assertThat(amount).hasToString("1234.50");

    }

    @Test
    void offsetDateTimeIsStoredAsBsonDate() {

        final var dueDate = written().get("dueDate");

        assertThat(dueDate).isInstanceOf(Date.class);
        // the offset is not stored - the instant is, normalised to UTC
        assertThat(((Date) dueDate).toInstant()).isEqualTo(sample().dueDate.toInstant());

    }

    /**
     * {@code OffsetDateTime.MAX} is the cockpit's "no end" marker, e.g. for follow-up dates. It cannot be
     * expressed as a date, so it is stored as {@code Long.MAX_VALUE} milliseconds. Any change here silently
     * turns "open end" into a date in the year 292278994.
     */
    @Test
    void theOpenEndSentinelIsStoredAsMaxMillis() {

        assertThat(((Date) written().get("openEnd")).getTime()).isEqualTo(Long.MAX_VALUE);

    }

    @Test
    void theStoredDocumentRoundTrips() {

        final var readBack = converter().read(Sample.class, written());

        assertThat(readBack.id).isEqualTo("task-1");
        assertThat(readBack.amount).isEqualByComparingTo("1234.50");
        assertThat(readBack.dueDate.toInstant()).isEqualTo(sample().dueDate.toInstant());
        assertThat(readBack.openEnd).isEqualTo(OffsetDateTime.MAX);

    }

    /**
     * Reading is the direction that matters for backwards compatibility: documents written by earlier
     * cockpit versions carry exactly these two BSON types, and they have to keep mapping onto the same Java
     * values. The document below is assembled by hand rather than by the converter, so this is a real
     * "old data" test and not a round-trip.
     */
    @Test
    void documentsWrittenByEarlierVersionsAreStillRead() {

        final var stored = new Document()
                .append("id", "task-1")
                .append("amount", new Decimal128(new BigDecimal("1234.50")))
                .append("dueDate", new Date(1785494096000L))
                .append("openEnd", new Date(Long.MAX_VALUE));

        final var readBack = converter().read(Sample.class, stored);

        assertThat(readBack.amount).isEqualByComparingTo("1234.50");
        assertThat(readBack.dueDate.toInstant().toEpochMilli()).isEqualTo(1785494096000L);
        assertThat(readBack.openEnd).isEqualTo(OffsetDateTime.MAX);

    }

    /**
     * Without the four converters Spring Data falls back to its own handling, and that produces different
     * BSON. Asserting the difference is what gives the tests above their meaning: they show the custom
     * converters win, not that the types happen to match by luck.
     */
    @Test
    void withoutTheCustomConvertersTheBsonTypesDiffer() {

        final var mappingContext = new MongoMappingContext();
        final var plainConversions = new MongoCustomConversions(List.of());
        mappingContext.setSimpleTypeHolder(plainConversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();

        final var plainConverter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        plainConverter.setCustomConversions(plainConversions);
        plainConverter.afterPropertiesSet();

        final var document = new Document();
        plainConverter.write(sample(), document);

        assertThat(document.get("amount"))
                .as("Spring Data's own BigDecimal handling differs from Decimal128 - if this ever becomes "
                        + "Decimal128, the custom converters could be dropped")
                .isNotInstanceOf(Decimal128.class);

    }

}
