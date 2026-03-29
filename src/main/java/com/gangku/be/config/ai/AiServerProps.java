package com.gangku.be.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.server")
public class AiServerProps {

    private String baseUrl;
    private int connectTimeoutMillis;
    private int responseTimeoutMillis;
    private String introPath;
    private String textFilterPath;
    private String recommendationsPath;
    private String refreshClusteringPath;
    private String refreshPopularityPath;
}
