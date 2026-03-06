package com.distributed_systems.service_a;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.distributed_systems.service_a.ServiceBClient.ServiceBClient;

@RestController
public class HomeController {

  private final DataSource dataSource;
  private final ServiceBClient serviceBClient;
  // private static final List<byte[]> leakedMemory = new ArrayList<>();

  public HomeController(DataSource dataSource, ServiceBClient serviceBClient){
    this.dataSource = dataSource;
    this.serviceBClient = serviceBClient;
  }

  @RequestMapping("/process")
  public String home(){
    // leakedMemory.add(new byte[500 * 1024]);
    return serviceBClient.process();
  }

  @GetMapping("/slow")
  public String slowProcess(){
    return serviceBClient.slowProcess();
  }

  @GetMapping("/leak")
  public String leak() throws SQLException{
    try(Connection connection = dataSource.getConnection()){

    }
    
    return "Service A is leaking memory Completed";
  }
}