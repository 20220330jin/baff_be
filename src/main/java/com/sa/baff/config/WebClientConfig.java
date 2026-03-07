package com.sa.baff.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.InputStream;

@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @Qualifier("tossWebClient")
    @ConditionalOnProperty(name = "toss.api.url")
    public WebClient tossWebClient(
            @Value("${toss.api.url}") String tossApiUrl,
            @Value("${toss.cert-path}") Resource clientCert,
            @Value("${toss.key-path}") Resource clientKey) {
        try {
            InputStream certInputStream = clientCert.getInputStream();
            InputStream keyInputStream = clientKey.getInputStream();

            SslContext sslContext = SslContextBuilder.forClient()
                    .keyManager(certInputStream, keyInputStream)
                    .build();

            HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

            return WebClient.builder()
                    .baseUrl(tossApiUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create tossWebClient: {}", e.getMessage());
            throw new RuntimeException("Failed to create tossWebClient: " + e.getMessage());
        }
    }
}
