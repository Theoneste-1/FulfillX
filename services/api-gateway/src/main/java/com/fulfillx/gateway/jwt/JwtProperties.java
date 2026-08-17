package com.fulfillx.gateway.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fulfillx.security.jwt")
public class JwtProperties {
    private String secret = "fulfillx-demo-jwt-secret-change-me-32bytes!!";
    private String issuer = "fulfillx-auth";
    private String audience = "fulfillx-api";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}
