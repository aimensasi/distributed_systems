package com.distributed_systems.service_a;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("https://localhost:8081")
public interface ServiceBClient {

  @GetExchange("/process")
  String process();
}
