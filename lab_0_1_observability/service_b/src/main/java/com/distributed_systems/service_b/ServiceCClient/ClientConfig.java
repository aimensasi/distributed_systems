package com.distributed_systems.service_b.ServiceCClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.micrometer.observation.ObservationRegistry;

@Configuration
public class ClientConfig {
    
    @Value("${services.c.url}")
    private String serviceUrl;

    @Bean
    public RestClient.Builder restClientBuilder(ObservationRegistry register){
        return RestClient.builder()
            .observationRegistry(register);
    }

    @Bean
    public ServiceCClient serviceCClient(RestClient.Builder builder){
        RestClient restClient = builder.baseUrl(serviceUrl).build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient))
        .build();

        return factory.createClient(ServiceCClient.class);
    }
}
