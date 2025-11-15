package com.auzienko.observability.corebackend.app.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean(name = "loadTestRestTemplate")
    public RestTemplate loadTestRestTemplate() {
        PoolingHttpClientConnectionManager connectionManager = createConnectionManager();
        HttpClient httpClient = createHttpClient(connectionManager);
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }

    private PoolingHttpClientConnectionManager createConnectionManager() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(500);
        connectionManager.setDefaultMaxPerRoute(100);
        return connectionManager;
    }

    private HttpClient createHttpClient(PoolingHttpClientConnectionManager connectionManager) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(5000))
                .setResponseTimeout(Timeout.ofMilliseconds(10000))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(3000))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

}
