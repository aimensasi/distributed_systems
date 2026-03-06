package com.distributed_systems.service_a.ServiceBClient;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface ServiceBClient {

  @GetExchange("/process")
  String process();

  @GetExchange("/slow")
  String slowProcess();
}
