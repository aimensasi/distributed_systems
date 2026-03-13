package com.distributed_systems.module_1_coordination_service_b;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@RestController
public class IdempotencyController {

  private final DataSource dataSource;

  public IdempotencyController(DataSource dataSource){
    this.dataSource = dataSource;
  }

  @GetMapping("/payment")
  public String process(@RequestHeader("Idempotency-Key") String idempotencyKey){
    try(Connection con = dataSource.getConnection()){
      PreparedStatement stm = con.prepareStatement("INSERT INTO transactions (amount, transaction_key) VALUES (?, ?)");
      stm.setInt(1, 300);
      stm.setString(2, idempotencyKey);

      if(stm.executeUpdate() > 0) {
        return "Payment Successful";
      }

    } catch (SQLException e) {
      if(e.getSQLState().startsWith("23")){
        return "Payment already processed";
      }
    }

    return "Payment Failed";
  }
}
