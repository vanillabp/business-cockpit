package io.vanillabp.cockpit.config.web.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.User;

import io.vanillabp.cockpit.commons.utils.UserDetails;

public class BasicUserDetails implements UserDetails {

    public BasicUserDetails(User user) {
        
        this.user = user;
        
    }

    private User user;
    
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
    public List<String> getRoles() {
        
        return user
                .getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
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
