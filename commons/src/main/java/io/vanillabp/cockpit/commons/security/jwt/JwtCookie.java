package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.boot.web.server.Cookie.SameSite;

public class JwtCookie {

    private String name = "bc";
    
    private String domain;
    
    private String path = "/";
    
    private SameSite sameSite;
    
    private boolean secure;
    
    private String expiresDuration = "PT12H";

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
    
}
