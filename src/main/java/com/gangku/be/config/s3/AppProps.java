package com.gangku.be.config.s3;

import lombok.Getter; import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app")
public class AppProps {
    private S3Props s3 = new S3Props();
    private CdnProps cdn = new CdnProps();

    @Getter @Setter
    public static class S3Props {
        private String bucket;
        private String region;
        private String basePrefix;
        private String envPrefix;
        private int ttlSeconds;
    }
    @Getter @Setter
    public static class CdnProps {
        private String baseUrl;
    }
}
