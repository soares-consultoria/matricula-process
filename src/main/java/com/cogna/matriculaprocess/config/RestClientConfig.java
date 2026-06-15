package com.cogna.matriculaprocess.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuração do cliente REST usado para consultar a API de ciclos.
 *
 * <p>A URL base vem da propriedade {@code app.ciclo-api.url} (alimentada pela
 * variável de ambiente {@code CICLO_API_URL}), permitindo apontar para o mock
 * (WireMock) em desenvolvimento ou para o serviço real em produção sem alterar
 * o código.</p>
 *
 * @author Equipe matricula-process
 * @see com.cogna.matriculaprocess.adapter.out.rest.CicloApiClient
 */
@Configuration
public class RestClientConfig {

    /**
     * Cria o {@link RestClient} da API de ciclos com URL base e timeouts.
     *
     * @param baseUrl        URL base da API ({@code CICLO_API_URL})
     * @param connectTimeout timeout de conexão, em milissegundos
     * @param readTimeout    timeout de leitura, em milissegundos
     * @return cliente REST configurado
     */
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
