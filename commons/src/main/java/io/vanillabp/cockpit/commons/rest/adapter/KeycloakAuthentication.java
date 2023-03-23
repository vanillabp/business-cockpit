package io.vanillabp.cockpit.commons.rest.adapter;

public class KeycloakAuthentication extends Client {

    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
