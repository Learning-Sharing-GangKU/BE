package com.gangku.be.config.aws;

import lombok.Getter; import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app")
public class AwsAppProps {
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
