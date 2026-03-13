package com.distributed_systems.module_1_coordination_service_a;

import com.distributed_systems.module_1_coordination_service_a.Services.Lock;
import com.distributed_systems.module_1_coordination_service_a.Services.RedisService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

@RestController
public class LockController {
  private final DataSource dataSource;
  private final RedisService redisService;

  public LockController(DataSource dataSource, RedisService redisService){
    this.dataSource = dataSource;
    this.redisService = redisService;
  }

  @GetMapping("/transfer")
  public String transfer(){
    String key = "fromAtoB";
    Lock lock = redisService.acquireLock(
      key,
      Duration.ofMillis(100),
      Duration.ofSeconds(10)
    );

    if(lock == null) {
      return "Transfer Failed b";
    }

    int amount = 100;
    try {
      try(Connection connection = dataSource.getConnection()){
        int accountABalance = getAccountABalance(connection);

        if(accountABalance < amount) {
          return "Transfer Failed, Insufficient Fund";
        }

        boolean debitResult = debitAccountA(connection, amount, lock.fencingToken());
        boolean creditResult = creditAccountB(connection, amount, lock.fencingToken());

        if(!debitResult || !creditResult){
          return "Transfer Failed, Insufficient Fund";
        }
        return "Transfer Completed";
      }
    } catch (SQLException e) {

      return "Transfer Failed";
    } finally {
      redisService.releaseLock(key, lock.ticket());
    }
  }


  private boolean debitAccountA(Connection connection, int amount, long fencingToken) throws SQLException {
    PreparedStatement write = connection.prepareStatement("UPDATE accounts SET balance = balance - ?, version = ? WHERE id = 1 AND version < ?");
    write.setInt(1, amount);
    write.setLong(2, fencingToken);
    write.setLong(3, fencingToken);
    return write.executeUpdate() > 0;
  }

  private boolean creditAccountB(Connection connection, int amount, long fencingToken) throws SQLException {
    PreparedStatement write = connection.prepareStatement("UPDATE accounts SET balance = balance + ?, version = ? WHERE id = 2 AND version < ?");
    write.setInt(1, amount);
    write.setLong(2, fencingToken);
    write.setLong(3, fencingToken);
    return write.executeUpdate() > 0;
  }

  private static int getAccountABalance(Connection connection) throws SQLException {
    PreparedStatement read = connection.prepareStatement("SELECT balance from accounts WHERE id = ? LIMIT 1");
    read.setInt(1, 1);
    ResultSet accountABalanceResult = read.executeQuery();
    accountABalanceResult.next();
    return accountABalanceResult.getInt("balance");
  }
}
