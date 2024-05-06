package io.vanillabp.cockpit.util.candidates;

import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetails;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public abstract class CandidatesAware {

    protected abstract List<String> getGroupIds();

    protected abstract List<String> getUserIds();

    public Collection<String> getTargetGroups() {

        final var result = new HashSet<String>();
        if (getGroupIds() != null) {
            result.addAll(getGroupIds());
        }
        if (getUserIds() != null) {
            getUserIds()
                    .forEach(user -> result.add(JwtUserDetails.USER_AUTHORITY_PREFIX + user));
        }
        if (result.isEmpty()) {
            return null; // means visible to everyone
        }
        return result;

    }

    public boolean hasOneOfTargetGroups(
            final String... groups) {

        if ((groups == null)
                || (groups.length == 0)) {
            return true;
        }

        final var targetGroups = getTargetGroups();
        return Arrays
                .stream(groups)
                .anyMatch(targetGroups::contains);

    }

    public boolean hasOneOfTargetGroups(
            final Collection<String> groups) {

        if ((groups == null)
                || groups.isEmpty()) {
            return true;
        }

        final var targetGroups = getTargetGroups();
        return groups
                .stream()
                .anyMatch(targetGroups::contains);

    }

}
