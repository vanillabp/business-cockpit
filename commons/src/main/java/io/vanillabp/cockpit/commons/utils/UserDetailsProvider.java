package io.vanillabp.cockpit.commons.utils;

public interface UserDetailsProvider {

    UserDetails getUserDetails(Object principal);
    
}
