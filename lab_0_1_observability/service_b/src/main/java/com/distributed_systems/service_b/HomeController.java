package com.distributed_systems.servce_b;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
class HomeController {

  @RequestMapping("/")
  public String home(){
    return RestClient.create()
      .get()
      .uri("http://localhost:8082")
      .retrieve()
      .body(String.class);
  }
}
