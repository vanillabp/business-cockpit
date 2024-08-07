package io.vanillabp.cockpit.commons.security.usercontext;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public interface UserDetails {

    String getId();

    String getEmail();

    String getDisplay();

    String getDisplayShort();

    List<String> getAuthorities();

    default boolean hasAuthority(
            final String authority) {

        return getAuthorities()
                .stream()
                .anyMatch(given -> Objects.equals(authority, given));

    }

    default boolean hasOneAuthorityOf(
            final Collection<String> authorities) {

        return getAuthorities()
                .stream()
                .anyMatch(authorities::contains);

    }

    default boolean hasAllAuthoritiesOf(
            final Collection<String> authorities) {

        final var given = getAuthorities();
        return authorities
                .stream()
                .allMatch(given::contains);

    }

}
