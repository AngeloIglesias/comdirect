package comdirect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "comdirect")
public class ComdirectConfig {

    private BrowserConfig browser;
    private UiConfig ui;
    private LoginConfig login;

    @Data
    public static class BrowserConfig {
        private boolean autoCloseCookieBanner;
        private boolean headless;
    }

    @Data
    public static class UiConfig {
        private boolean enableJavascriptDebug;
        private boolean enableJavascriptConsole;
    }

    @Data
    public static class LoginConfig {
        private String url0;
        private String url1;
        private String url2;
        private String postUrl;
        private String user;
        private String pin;
    }
}
