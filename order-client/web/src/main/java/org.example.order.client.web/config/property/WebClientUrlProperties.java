package org.example.order.client.web.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("web-client")
public class WebClientUrlProperties {
    private Client client;

    public Client getClient() {
        return client;
    }

    @Getter
    @Setter
    public static class Client {
        private String clientId;
        private Url url;
    }

    @Getter
    @Setter
    public static class Url {
        String order;
        String user;

        public String getWithPathVariable(String url, Object path) {
            return String.format("%s/%s", url, path);
        }
    }
}
