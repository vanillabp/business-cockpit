package io.vanillabp.cockpit.gui.api.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

record TestSource(){}

class GuiEventTest {
    private static final List<String> TARGET_GROUPS = List.of("A", "B");
    private static final List<String> MATCHING_GROUPS = Collections.singletonList("B");
    private static final List<String> OTHER_GROUPS = Collections.singletonList("C");

    private GuiEvent event;

    @BeforeEach
    void setUp() {
	event = new GuiEvent(new TestSource(), TARGET_GROUPS, null);
    }

    @Test
    void eventIsRelevantForCollectionContainingGroupTest() {
	var isRelevant = event.relevantForTargetGroups(MATCHING_GROUPS);

	assertTrue(isRelevant);
    }

    @Test
    void eventIsRelevantWhenNoGroupsAreSet() {
	var eventWithoutGroups = new GuiEvent(new TestSource(), null, null);

	var isRelevant = eventWithoutGroups.relevantForTargetGroups(MATCHING_GROUPS);

	assertTrue(isRelevant);
    }

    @Test
    void eventIsNotRelevantWhenNoGroupsAreMatching() {
	var isRelevant = event.relevantForTargetGroups(OTHER_GROUPS);

	assertFalse(isRelevant);
    }
}