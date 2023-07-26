package io.vanillabp.cockpit.commons.rest.adapter;

import java.time.Duration;

public class Retry {

    private boolean enabled = false;
    private int maxAttempts = 5;
    private Duration period = Duration.ofMillis(100);
    private Duration maxPeriod = Duration.ofSeconds(1);
    
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public int getMaxAttempts() {
        return maxAttempts;
    }
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    public Duration getPeriod() {
        return period;
    }
    public void setPeriod(Duration period) {
        this.period = period;
    }
    public Duration getMaxPeriod() {
        return maxPeriod;
    }
    public void setMaxPeriod(Duration maxPeriod) {
        this.maxPeriod = maxPeriod;
    }
    
}
