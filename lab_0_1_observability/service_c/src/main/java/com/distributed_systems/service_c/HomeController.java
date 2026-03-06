package com.distributed_systems.service_c;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HomeController {

  @RequestMapping("/process")
  public String home(){
    return "Service WHAT C";
  }

  @GetMapping("/slow")
  public String slowProcess(){
    try {
      Thread.sleep(10_000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "Service C is slow";
  }
}
