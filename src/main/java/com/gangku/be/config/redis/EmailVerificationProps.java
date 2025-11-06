package com.gangku.be.config.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.email.verification")
public class EmailVerificationProps {
    private long tokenTtlMinutes;
    private long sessionTtlMinutes;
}