package com.gangku.be.config.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AppProps.class, AssetPolicyProps.class})
public class ImageConfig { }
