package com.pharmaai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "whatsapp.api")
public class WhatsAppConfig {

    private String token;
    private String phoneNumberId;
    private String verifyToken;
    private String url;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
