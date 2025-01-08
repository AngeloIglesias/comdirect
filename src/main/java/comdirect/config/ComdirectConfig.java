package comdirect.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "comdirect.login")
@Getter
@Setter
public class ComdirectConfig {

    private String url0;
    private String url1;
    private String url2;
    private String user;
    private String pin;
    private String postUrl;
}
