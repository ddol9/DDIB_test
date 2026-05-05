package com.ddib.monolith.support.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenValiditySeconds;
    private long refreshTokenValiditySeconds;
    private String issuer = "ddib-monolith";
    private String audience = "api";
    private long clockSkewSeconds = 60;
}

