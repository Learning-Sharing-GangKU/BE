package com.gangku.be.config.aws;

import lombok.Getter; import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@ConfigurationProperties(prefix = "assets")
public class AssetPolicyProps {
    private List<Category> categories = new ArrayList<>();

    @Getter @Setter
    public static class Category {
        private String type;
        private String prefix;
        private List<String> allowedContentTypes;
    }
}
