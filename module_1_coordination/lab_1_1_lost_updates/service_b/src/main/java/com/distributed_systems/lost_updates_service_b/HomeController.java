package com.distributed_systems.lost_updates_service_b;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


@RestController
class HomeController {

  private final DataSource dataSource;
  private final MeterRegistry meterRegistry;

  public HomeController(DataSource dataSource, MeterRegistry meterRegistry){
    this.dataSource = dataSource;
    this.meterRegistry = meterRegistry;
  }

  @GetMapping("/increment")
  public String increment() throws SQLException{
    meterRegistry
      .counter("increment.attempt", "service", "service_b")
      .increment();
    int current = 0;
    try(Connection connection = dataSource.getConnection()){

      try(PreparedStatement write = connection.prepareStatement(
        "UPDATE counters SET value = value + ? WHERE id = ? RETURNING value"
      )) {
        write.setInt(1, 1);
        write.setInt(2, 1);
        ResultSet rs = write.executeQuery();
        rs.next();
        current = rs.getInt("value");
      }
    }

    return "Current Value " + current;
  }
}
