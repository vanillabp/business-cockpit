package io.vanillabp.cockpit.commons.mongo.changestreams;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationTypeTest {

    @Test
    void enumValues_hasExpectedCount() {
        OperationType[] values = OperationType.values();

        assertThat(values).hasSize(4);
    }

    @Test
    void enumValues_containsExpectedValues() {
        assertThat(OperationType.values())
                .contains(OperationType.ANY, OperationType.INSERT, OperationType.UPDATE, OperationType.DELETE);
    }

    @Test
    void any_containsAllMongoTypes() {
        List<String> mongoTypes = OperationType.ANY.getMongoTypes();

        assertThat(mongoTypes).containsExactlyInAnyOrder("insert", "update", "replace", "delete");
    }

    @Test
    void insert_containsOnlyInsert() {
        List<String> mongoTypes = OperationType.INSERT.getMongoTypes();

        assertThat(mongoTypes).containsExactly("insert");
    }

    @Test
    void update_containsUpdateAndReplace() {
        List<String> mongoTypes = OperationType.UPDATE.getMongoTypes();

        assertThat(mongoTypes).containsExactlyInAnyOrder("update", "replace");
    }

    @Test
    void delete_containsOnlyDelete() {
        List<String> mongoTypes = OperationType.DELETE.getMongoTypes();

        assertThat(mongoTypes).containsExactly("delete");
    }

    @Test
    void byMongoType_insert_returnsInsert() {
        OperationType result = OperationType.byMongoType("insert");

        assertThat(result).isEqualTo(OperationType.INSERT);
    }

    @Test
    void byMongoType_update_returnsUpdate() {
        OperationType result = OperationType.byMongoType("update");

        assertThat(result).isEqualTo(OperationType.UPDATE);
    }

    @Test
    void byMongoType_replace_returnsUpdate() {
        OperationType result = OperationType.byMongoType("replace");

        assertThat(result).isEqualTo(OperationType.UPDATE);
    }

    @Test
    void byMongoType_delete_returnsDelete() {
        OperationType result = OperationType.byMongoType("delete");

        assertThat(result).isEqualTo(OperationType.DELETE);
    }

    @Test
    void byMongoType_unknownType_throwsRuntimeException() {
        assertThatThrownBy(() -> OperationType.byMongoType("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unsupported change-stream operation-type 'unknown'!");
    }

    @Test
    void byMongoType_invalidate_throwsRuntimeException() {
        assertThatThrownBy(() -> OperationType.byMongoType("invalidate"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unsupported change-stream operation-type");
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(OperationType.valueOf("ANY")).isEqualTo(OperationType.ANY);
        assertThat(OperationType.valueOf("INSERT")).isEqualTo(OperationType.INSERT);
        assertThat(OperationType.valueOf("UPDATE")).isEqualTo(OperationType.UPDATE);
        assertThat(OperationType.valueOf("DELETE")).isEqualTo(OperationType.DELETE);
    }
}
