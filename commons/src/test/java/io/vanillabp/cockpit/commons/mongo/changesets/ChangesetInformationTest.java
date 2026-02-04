package io.vanillabp.cockpit.commons.mongo.changesets;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChangesetInformationTest {

    @Test
    void collectionName_hasExpectedValue() {
        assertThat(ChangesetInformation.COLLECTION_NAME).isEqualTo("ChangesetInformation");
    }

    @Test
    void defaultValues_areNull() {
        ChangesetInformation info = new ChangesetInformation();

        assertThat(info.getId()).isNull();
        assertThat(info.getAuthor()).isNull();
        assertThat(info.getTimestamp()).isNull();
        assertThat(info.getOrder()).isZero();
        assertThat(info.getRollbackScripts()).isNull();
        assertThat(info.getVersion()).isZero();
    }

    @Test
    void setId_updatesValue() {
        ChangesetInformation info = new ChangesetInformation();
        info.setId("changeset-001");

        assertThat(info.getId()).isEqualTo("changeset-001");
    }

    @Test
    void setAuthor_updatesValue() {
        ChangesetInformation info = new ChangesetInformation();
        info.setAuthor("developer@example.com");

        assertThat(info.getAuthor()).isEqualTo("developer@example.com");
    }

    @Test
    void setTimestamp_updatesValue() {
        ChangesetInformation info = new ChangesetInformation();
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        info.setTimestamp(timestamp);

        assertThat(info.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void setOrder_updatesValue() {
        ChangesetInformation info = new ChangesetInformation();
        info.setOrder(5);

        assertThat(info.getOrder()).isEqualTo(5);
    }

    @Test
    void setRollbackScripts_updatesValue() {
        ChangesetInformation info = new ChangesetInformation();
        List<String> scripts = List.of("DROP INDEX idx_1", "DROP TABLE temp_table");
        info.setRollbackScripts(scripts);

        assertThat(info.getRollbackScripts()).isEqualTo(scripts);
    }

    @Test
    void setVersion_updatesValue() {
        ChangesetInformation info = new ChangesetInformation();
        info.setVersion(10L);

        assertThat(info.getVersion()).isEqualTo(10L);
    }

    @Test
    void equals_withSameId_returnsTrue() {
        ChangesetInformation info1 = new ChangesetInformation();
        info1.setId("changeset-001");

        ChangesetInformation info2 = new ChangesetInformation();
        info2.setId("changeset-001");

        assertThat(info1.equals(info2)).isTrue();
    }

    @Test
    void equals_withDifferentId_returnsFalse() {
        ChangesetInformation info1 = new ChangesetInformation();
        info1.setId("changeset-001");

        ChangesetInformation info2 = new ChangesetInformation();
        info2.setId("changeset-002");

        assertThat(info1.equals(info2)).isFalse();
    }

    @Test
    void equals_withNonChangesetInformation_returnsFalse() {
        ChangesetInformation info = new ChangesetInformation();
        info.setId("changeset-001");

        assertThat(info.equals("changeset-001")).isFalse();
    }

    @Test
    void equals_withNull_returnsFalse() {
        ChangesetInformation info = new ChangesetInformation();
        info.setId("changeset-001");

        assertThat(info.equals(null)).isFalse();
    }

    @Test
    void hashCode_basedOnId() {
        ChangesetInformation info = new ChangesetInformation();
        info.setId("changeset-001");

        assertThat(info.hashCode()).isEqualTo("changeset-001".hashCode());
    }

    @Test
    void fullConfiguration_worksCorrectly() {
        ChangesetInformation info = new ChangesetInformation();
        info.setId("V1_001_add_users_table");
        info.setAuthor("admin");
        info.setTimestamp(OffsetDateTime.of(2024, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC));
        info.setOrder(1);
        info.setVersion(1L);
        info.setRollbackScripts(List.of("DROP TABLE users"));

        assertThat(info.getId()).isEqualTo("V1_001_add_users_table");
        assertThat(info.getAuthor()).isEqualTo("admin");
        assertThat(info.getTimestamp()).isNotNull();
        assertThat(info.getOrder()).isEqualTo(1);
        assertThat(info.getVersion()).isEqualTo(1L);
        assertThat(info.getRollbackScripts()).containsExactly("DROP TABLE users");
    }
}
