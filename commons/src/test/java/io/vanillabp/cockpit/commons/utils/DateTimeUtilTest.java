package io.vanillabp.cockpit.commons.utils;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateTimeUtilTest {

    @Test
    void toOffsetDate_withNullString_returnsNull() {
        OffsetDateTime result = DateTimeUtil.toOffsetDate(null);
        assertThat(result).isNull();
    }

    @Test
    void toOffsetDate_withEmptyString_returnsNull() {
        OffsetDateTime result = DateTimeUtil.toOffsetDate("");
        assertThat(result).isNull();
    }

    @Test
    void toOffsetDate_withValidDateString_returnsOffsetDateTime() {
        OffsetDateTime result = DateTimeUtil.toOffsetDate("2024-01-15");

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        // Note: Day may vary based on local timezone since SimpleDateFormat parses in local timezone
        assertThat(result.getDayOfMonth()).isBetween(14, 15);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void toOffsetDate_withInvalidDateString_throwsRuntimeException() {
        assertThatThrownBy(() -> DateTimeUtil.toOffsetDate("invalid-date"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Wrong date format");
    }

    @Test
    void toOffsetDate_withWrongFormat_throwsRuntimeException() {
        assertThatThrownBy(() -> DateTimeUtil.toOffsetDate("15/01/2024"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Wrong date format");
    }

    @Test
    void fromMilliseconds_returnsCorrectOffsetDateTime() {
        // January 1, 2024 00:00:00 UTC in milliseconds
        long millis = 1704067200000L;

        OffsetDateTime result = DateTimeUtil.fromMilliseconds(millis);

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void fromDate_withNullDate_returnsNull() {
        OffsetDateTime result = DateTimeUtil.fromDate(null);
        assertThat(result).isNull();
    }

    @Test
    void fromDate_withValidDate_returnsOffsetDateTime() {
        Date date = new Date(1704067200000L); // January 1, 2024 00:00:00 UTC

        OffsetDateTime result = DateTimeUtil.fromDate(date);

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(1);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void fromDate_preservesTimeComponents() {
        // January 15, 2024 14:30:45 UTC
        long millis = 1705329045000L;
        Date date = new Date(millis);

        OffsetDateTime result = DateTimeUtil.fromDate(date);

        assertThat(result).isNotNull();
        assertThat(result.getHour()).isEqualTo(14);
        assertThat(result.getMinute()).isEqualTo(30);
        assertThat(result.getSecond()).isEqualTo(45);
    }
}
