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
    private Intro intro = new Intro();
    private TextFilter textFilter = new TextFilter();
    private Recommendations recommendations = new Recommendations();
    private RefreshClustering refreshClustering = new RefreshClustering();
    private RefreshPopularity refreshPopularity = new RefreshPopularity();

    @Getter @Setter
    public static class Intro {
        private String v1;
    }

    @Getter @Setter
    public static class TextFilter {
        private String v1;
        private String v2;
    }

    @Getter @Setter
    public static class Recommendations {
        private String v1;
        private String v2;
    }

    @Getter @Setter
    public static class RefreshClustering {
        private String v1;
        private String v2;
    }

    @Getter @Setter
    public static class RefreshPopularity {
        private String v1;
        private String v2;
    }
}
