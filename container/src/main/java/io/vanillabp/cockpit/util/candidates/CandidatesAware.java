package io.vanillabp.cockpit.util.candidates;

import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public abstract class CandidatesAware {

    protected abstract List<String> getGroups();

    protected abstract List<String> getUsers();

    public Collection<String> getTargetRoles() {

        final var result = new HashSet<String>();
        if (getGroups() != null) {
            result.addAll(getGroups());
        }
        if (getUsers() != null) {
            getUsers()
                    .forEach(user -> result.add(JwtUserDetails.USER_AUTHORITY_PREFIX + user));
        }
        if (result.isEmpty()) {
            return null; // means visible to everyone
        }
        return result;

    }

    public boolean hasOneOfTargetRoles(
            final String... roles) {

        if ((roles == null)
                || (roles.length == 0)) {
            return true;
        }

        final var targetRoles = getTargetRoles();
        return Arrays
                .stream(roles)
                .anyMatch(targetRoles::contains);

    }

    public boolean hasOneOfTargetRoles(
            final Collection<String> roles) {

        if ((roles == null)
                || roles.isEmpty()) {
            return true;
        }

        final var targetRoles = getTargetRoles();
        return roles
                .stream()
                .anyMatch(targetRoles::contains);

    }

}
