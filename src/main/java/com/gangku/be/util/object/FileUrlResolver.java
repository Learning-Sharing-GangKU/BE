package com.gangku.be.util.object;

import com.gangku.be.config.aws.AwsAppProps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileUrlResolver {

    private final AwsAppProps awsAppProps;

    public String toPublicUrl(String key) {
        String cdn = awsAppProps.getCdn().getBaseUrl();
        if (cdn != null && !cdn.isBlank()) {
            return (cdn.endsWith("/") ? cdn : cdn + "/") + key;
        }
        return "https://"
                + awsAppProps.getS3().getBucket()
                + ".s3." + awsAppProps.getS3().getRegion()
                + ".amazonaws.com/"
                + key;
    }
}
