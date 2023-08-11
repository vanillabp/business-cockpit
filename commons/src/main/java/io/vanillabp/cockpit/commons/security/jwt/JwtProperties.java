package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.util.StringUtils;

import java.util.Base64;

import javax.crypto.KeyGenerator;

public class JwtProperties {

    private JwtCookie cookie = new JwtCookie();
    
    private String hmacSHA256Base64;

    public JwtCookie getCookie() {
        return cookie;
    }

    public void setCookie(JwtCookie cookie) {
        this.cookie = cookie;
    }
    
    public String getHmacSHA256Base64() {
        if (!StringUtils.hasText(hmacSHA256Base64)) {
            final String hmacKey;
            try {
                final var key = KeyGenerator
                        .getInstance("HmacSha256")
                        .generateKey();
                hmacKey = Base64
                        .getEncoder()
                        .encodeToString(key.getEncoded());
            } catch (Exception e) {
                throw new RuntimeException(
                        "No property 'business-cockpit.jwt.hmacSHA256-base64' set! "
                        + "Additionally, on generating a key to be used an error occured.", e);
            }
            throw new RuntimeException(
                    "No property 'business-cockpit.jwt.hmacSHA256-base64' set! "
                    + "One could use this generated key: '"
                    + hmacKey
                    + "'.");
        }
        return hmacSHA256Base64;
    }
    
    public void setHmacSHA256Base64(String hmacSHA256Base64) {
        this.hmacSHA256Base64 = hmacSHA256Base64;
    }
    
    public byte[] getHmacSHA256() {
        
        return Base64.getDecoder().decode(getHmacSHA256Base64());
        
    }
    
}
