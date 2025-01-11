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
        private String defaultBrowser;
        private String edgePath;
        private boolean autoCloseCookieBanner;
        private boolean headless;
    }

    @Data
    public static class UiConfig {
        private String homeUrl;
        private boolean enableJavascriptDebug;
        private boolean enableJavascriptConsole;
    }

    @Data
    public static class LoginConfig {
        private String url0;
        private String url1;
        private String url2;
        private String url3;
        private String postUrl;
        private String user;
        private String pin;
    }
}
