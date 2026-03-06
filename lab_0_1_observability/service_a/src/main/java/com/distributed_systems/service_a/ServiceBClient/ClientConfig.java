package com.distributed_systems.service_a.ServiceBClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.micrometer.observation.ObservationRegistry;

@Configuration
public class ClientConfig {

  @Value("${services.b.url}")
  public String serviceUrl;

  @Bean
  public RestClient.Builder restClientBuilder(ObservationRegistry registry) {
    return RestClient.builder()
      .observationRegistry(registry);
  }

  @Bean
  public ServiceBClient serviceBClient(RestClient.Builder builder) {
    RestClient restClient = builder.baseUrl(serviceUrl).build();

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
      .builderFor(RestClientAdapter.create(restClient))
      .build();

    return factory.createClient(ServiceBClient.class);
  }
}
