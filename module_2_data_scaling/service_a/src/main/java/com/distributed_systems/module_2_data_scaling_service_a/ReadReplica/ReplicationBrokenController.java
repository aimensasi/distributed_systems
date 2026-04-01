package com.distributed_systems.module_2_data_scaling_service_a.ReadReplica;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RestController()
@RequestMapping("/broken")
@Profile("local")
class ReplicationBrokenController {
  private JdbcTemplate primary;
  private JdbcTemplate replica;

  public ReplicationBrokenController(@Qualifier("primaryJdbcTemplate") JdbcTemplate primary,
                               @Qualifier("replicaJdbcTemplate") JdbcTemplate replica){
    this.primary = primary;
    this.replica = replica;
  }

  @GetMapping("/kv/{key}")
  public String process(@PathVariable("key") String key){
    try(Connection conn = replica.getDataSource().getConnection()){
      PreparedStatement stm = conn.prepareStatement("SELECT value from key_value where key = ?");
      stm.setString(1, key);
      ResultSet rs = stm.executeQuery();
      if(!rs.next()){
        return "Unable to get the data";
      }

    return "The Value is " + rs.getInt("value");
    } catch (SQLException e) {
      return "Something went wrong";
    }
  }

  @PutMapping("/kv/{key}")
  public String update(@PathVariable("key") String key, @RequestBody() int value){
    try(Connection conn = primary.getDataSource().getConnection()){
      PreparedStatement stm = conn.prepareStatement("INSERT INTO key_value (key, value) VALUES (?, ?)\n" +
        "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value");
      stm.setString(1, key);
      stm.setInt(2, value);

      if(stm.executeUpdate() > 0){
        return "Key Stored";
      }

      return "Something broken about this";
    } catch (SQLException e) {
      return "Something went wrong: " + e.getMessage();
    }
  }
}