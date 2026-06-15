package com.cogna.matriculaprocess.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient cicloRestClient(@Value("${app.ciclo-api.url}") String baseUrl,
                                      @Value("${app.ciclo-api.connect-timeout-ms:2000}") long connectTimeout,
                                      @Value("${app.ciclo-api.read-timeout-ms:5000}") long readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(connectTimeout))
                .withReadTimeout(Duration.ofMillis(readTimeout));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
