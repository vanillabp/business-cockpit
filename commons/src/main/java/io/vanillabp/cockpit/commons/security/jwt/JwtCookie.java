package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.boot.web.server.Cookie.SameSite;

public class JwtCookie {

    private String name = "bc";
    
    private String domain;
    
    private String path = "/";
    
    private SameSite sameSite;
    
    private boolean secure;
    
    /**
     * How long a token lives after its last renewal. A request made in the second half of that time
     * gets a new token, see {@link JwtRenewalFilter}.
     */
    private String expiresDuration = "PT12H";

    /**
     * How long a login lasts at most, however often its token is renewed. It counts from the
     * login, so a stolen cookie does not stay valid forever.
     */
    private String maxLoginDuration = "P7D";

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public SameSite getSameSite() {
        return sameSite;
    }

    public void setSameSite(SameSite sameSite) {
        this.sameSite = sameSite;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getExpiresDuration() {
        return expiresDuration;
    }
    
    public void setExpiresDuration(String expiresDuration) {
        this.expiresDuration = expiresDuration;
    }

    public String getMaxLoginDuration() {
        return maxLoginDuration;
    }

    public void setMaxLoginDuration(String maxLoginDuration) {
        this.maxLoginDuration = maxLoginDuration;
    }
    
}
