package com.distributed_systems.service_b;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.distributed_systems.service_b.ServiceCClient.ServiceCClient;

@RestController
class HomeController {

  private final ServiceCClient serviceCClient;
  // private static final List<byte[]> leakedMemory = new ArrayList<>();

  public HomeController(ServiceCClient serviceCClient){
    this.serviceCClient = serviceCClient;
  }

  @RequestMapping("/process")
  public String home(){
    // leakedMemory.add(new byte[500 * 1024]);
    return serviceCClient.process();
  }

  @GetMapping("/slow")
  public String slowProcess(){
    return serviceCClient.slowProcess();
  }
}
