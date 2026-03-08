package com.ose.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Jwt jwt = new Jwt();
    private DefaultAdmin defaultAdmin = new DefaultAdmin();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expireMinutes;
    }

    @Getter
    @Setter
    public static class DefaultAdmin {
        private String username;
        private String password;
        private String displayName;
    }
}
