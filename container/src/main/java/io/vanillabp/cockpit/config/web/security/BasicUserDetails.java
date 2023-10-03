package io.vanillabp.cockpit.config.web.security;

import io.vanillabp.cockpit.commons.utils.UserDetails;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

public class BasicUserDetails implements UserDetails {

    public BasicUserDetails(org.springframework.security.core.userdetails.UserDetails user) {
        
        this.user = user;
        
    }

    private org.springframework.security.core.userdetails.UserDetails user;
    
    @Override
    public String getId() {
        
        return user.getUsername();
        
    }
    
    @Override
    public String getEmail() {
        
        return null;
        
    }
    
    @Override
    public String getFirstName() {
        
        return null;
        
    }
    
    @Override
    public String getLastName() {
        
        return null;
        
    }
    
    @Override
    public List<String> getAuthorities() {
        
        return user
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
    }
    
    @Override
    public boolean isActive() {
        
        return true;
        
    }
    
    @Override
    public Boolean isFemale() {
        
        return null;
        
    }
    
}
