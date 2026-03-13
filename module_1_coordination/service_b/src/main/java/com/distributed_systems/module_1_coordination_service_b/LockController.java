package com.distributed_systems.module_1_coordination_service_a;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RestController
public class LockController {
  private final DataSource dataSource;

  public LockController(DataSource dataSource){
    this.dataSource = dataSource;
  }

  @GetMapping("/transfer")
  public String transfer(){
    int amount = 100;
    try {
      Connection connection = dataSource.getConnection();
      int accountABalance = getAccountABalance(connection);

      if(accountABalance < amount) {
        return "Transfer Failed, Insufficient Fund";
      }

      debitAccountA(connection, amount);
      creditAccountB(connection, amount);

      return "Transfer Completed";
    } catch (SQLException e) {

      return "Transfer Failed";
    }
  }



  private void debitAccountA(Connection connection, int amount) throws SQLException {
    PreparedStatement write = connection.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE id = 1");
    write.setInt(1, amount);
    write.executeUpdate();
  }

  private void creditAccountB(Connection connection, int amount) throws SQLException {
    PreparedStatement write = connection.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id = 2");
    write.setInt(1, amount);
    write.executeUpdate();
  }

  private static int getAccountABalance(Connection connection) throws SQLException {
    PreparedStatement read = connection.prepareStatement("SELECT balance from accounts WHERE id = ? LIMIT 1");
    read.setInt(1, 1);
    ResultSet accountABalanceResult = read.executeQuery();
    accountABalanceResult.next();
    return accountABalanceResult.getInt("balance");
  }
}
