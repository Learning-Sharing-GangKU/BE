package com.gangku.be.config.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmailVerificationProps.class)
public class VerificationConfig { }
