package com.company.app.ordermanager.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeilisearchConfig {
    @Value("${meilisearch.host}")
    private String meilisearchHost;

    @Value("${meilisearch.port}")
    private int meilisearchPort;

    @Value("${meilisearch.api-key}")
    private String meilisearchApiKey;

    @Bean
    public Client meilisearchClient() {
        String hostUrl = String.format("http://%s:%d", this.meilisearchHost, this.meilisearchPort);

        Config config = new Config(hostUrl, meilisearchApiKey);
        return new Client(config);
    }
}
