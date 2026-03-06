package com.distributed_systems.service_b.ServiceCClient;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface  ServiceCClient {

    @GetExchange("/process")
    public String process();

    @GetExchange("/slow")
    public String slowProcess();
}
