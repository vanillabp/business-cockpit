package io.vanillabp.cockpit.commons.rest.adapter.versioning;

/**
 * Something which knows the version of the API it talks to or offers.
 */
public interface ApiVersionAware {

    /**
     * The version of the API.
     *
     * @return The version
     */
    String getApiVersion();
    
}
