package io.vanillabp.cockpit.util.candidates;

import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetails;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CandidatesAware}.
 */
class CandidatesAwareTest {

    // --- getTargetGroups tests ---

    @Test
    void getTargetGroups_withNoGroupsOrUsers_returnsNull() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(null, null);

        // Act
        Collection<String> result = candidate.getTargetGroups();

        // Assert - null means visible to everyone
        assertThat(result).isNull();
    }

    @Test
    void getTargetGroups_withEmptyGroupsAndUsers_returnsNull() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of(), List.of());

        // Act
        Collection<String> result = candidate.getTargetGroups();

        // Assert - empty means visible to everyone
        assertThat(result).isNull();
    }

    @Test
    void getTargetGroups_withOnlyGroups_returnsGroupIds() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1", "group2"), null);

        // Act
        Collection<String> result = candidate.getTargetGroups();

        // Assert
        assertThat(result).containsExactlyInAnyOrder("group1", "group2");
    }

    @Test
    void getTargetGroups_withOnlyUsers_returnsUserIdsWithPrefix() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(null, List.of("user1", "user2"));

        // Act
        Collection<String> result = candidate.getTargetGroups();

        // Assert - users should have USER_AUTHORITY_PREFIX
        assertThat(result).containsExactlyInAnyOrder(
                JwtUserDetails.USER_AUTHORITY_PREFIX + "user1",
                JwtUserDetails.USER_AUTHORITY_PREFIX + "user2"
        );
    }

    @Test
    void getTargetGroups_withBothGroupsAndUsers_returnsBoth() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(
                List.of("group1"),
                List.of("user1")
        );

        // Act
        Collection<String> result = candidate.getTargetGroups();

        // Assert
        assertThat(result).containsExactlyInAnyOrder(
                "group1",
                JwtUserDetails.USER_AUTHORITY_PREFIX + "user1"
        );
    }

    // --- hasOneOfTargetGroups (varargs) tests ---

    @Test
    void hasOneOfTargetGroups_withNullGroups_returnsTrue() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1"), null);

        // Act
        boolean result = candidate.hasOneOfTargetGroups((String[]) null);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasOneOfTargetGroups_withEmptyGroups_returnsTrue() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1"), null);

        // Act
        boolean result = candidate.hasOneOfTargetGroups(new String[0]);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasOneOfTargetGroups_withMatchingGroup_returnsTrue() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1", "group2"), null);

        // Act
        boolean result = candidate.hasOneOfTargetGroups("group1", "group3");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasOneOfTargetGroups_withNoMatchingGroup_returnsFalse() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1", "group2"), null);

        // Act
        boolean result = candidate.hasOneOfTargetGroups("group3", "group4");

        // Assert
        assertThat(result).isFalse();
    }

    // --- hasOneOfTargetGroups (Collection) tests ---

    @Test
    void hasOneOfTargetGroups_collection_withNullGroups_returnsTrue() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1"), null);

        // Act
        boolean result = candidate.hasOneOfTargetGroups((Collection<String>) null);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasOneOfTargetGroups_collection_withEmptyGroups_returnsTrue() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1"), null);

        // Act
        boolean result = candidate.hasOneOfTargetGroups(List.of());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasOneOfTargetGroups_collection_withMatchingGroup_returnsTrue() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1", "group2"), null);

        // Act
        boolean result = candidate.hasOneOfTargetGroups(Arrays.asList("group1", "group3"));

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasOneOfTargetGroups_collection_withNoMatchingGroup_returnsFalse() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(List.of("group1", "group2"), null);

        // Act
        boolean result = candidate.hasOneOfTargetGroups(Arrays.asList("group3", "group4"));

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void hasOneOfTargetGroups_withMatchingUser_returnsTrue() {
        // Arrange
        CandidatesAware candidate = new TestCandidatesAware(null, List.of("user1"));

        // Act - check with the prefixed user ID
        boolean result = candidate.hasOneOfTargetGroups(JwtUserDetails.USER_AUTHORITY_PREFIX + "user1");

        // Assert
        assertThat(result).isTrue();
    }

    // --- Test implementation of abstract class ---

    private static class TestCandidatesAware extends CandidatesAware {
        private final List<String> groupIds;
        private final List<String> userIds;

        TestCandidatesAware(List<String> groupIds, List<String> userIds) {
            this.groupIds = groupIds;
            this.userIds = userIds;
        }

        @Override
        protected List<String> getGroupIds() {
            return groupIds;
        }

        @Override
        protected List<String> getUserIds() {
            return userIds;
        }
    }
}
