package com.distributed_systems.module_2_data_scaling_service_a.ConnectionPool;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lab22/fixed")
@Profile({"pool", "docker"})
public class ConnectionPoolFixed {

  private final JdbcTemplate primaryDataSource;
  private final JdbcTemplate reportDataSource;

  public ConnectionPoolFixed(
    @Qualifier("primaryDataSourceTemplate") JdbcTemplate primaryDataSource,
    @Qualifier("reportDataSourceTemplate") JdbcTemplate reportDataSource
  ){
    this.primaryDataSource = primaryDataSource;
    this.reportDataSource = reportDataSource;
  }

  @GetMapping("/fast")
  public String fast(){
    primaryDataSource.execute("SELECT pg_sleep(0.5)");
    return "Done";
  }

  @GetMapping("/slow")
  public String slow(){
    reportDataSource.execute("SELECT pg_sleep(5)");
    return "Done";
  }
}
