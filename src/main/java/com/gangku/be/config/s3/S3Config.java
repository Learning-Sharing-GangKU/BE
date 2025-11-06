package com.gangku.be.config.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    S3Client s3Client(AppProps props) {
        return S3Client.builder()
                .region(Region.of(props.getS3().getRegion()))
                .build();
    }

    @Bean
    S3Presigner s3Presigner(AppProps props) {
        return S3Presigner.builder()
                .region(Region.of(props.getS3().getRegion()))
                .build();
    }
}
