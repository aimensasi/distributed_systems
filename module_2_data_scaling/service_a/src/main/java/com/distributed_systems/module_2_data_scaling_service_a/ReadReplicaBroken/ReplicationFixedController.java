package com.distributed_systems.module_2_data_scaling_service_a.ReadReplicaBroken;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

@RestController
@RequestMapping("/fixed")
class ReplicationFixedController {
  private static final String WAL_KEY = "wal:key";
  private JdbcTemplate primary;
  private JdbcTemplate replica;
  private StringRedisTemplate redisTemplate;

  public ReplicationFixedController(@Qualifier("primaryJdbcTemplate") JdbcTemplate primary,
                                    @Qualifier("replicaJdbcTemplate") JdbcTemplate replica,
                                    StringRedisTemplate redisTemplate){
    this.primary = primary;
    this.replica = replica;
    this.redisTemplate = redisTemplate;
  }

  @GetMapping("/kv/{key}")
  public String process(@PathVariable("key") String key){
    try(Connection conn = replica.getDataSource().getConnection()){
      boolean currentWalPosition = canUseReplica(conn, key);

      if(currentWalPosition) {
        return getValue(conn, key);
      }
      return getValueFromPrimary(key);
    } catch (SQLException e) {
      return "Something went wrong";
    }
  }

  private String getValueFromPrimary(String key) throws SQLException {
    try(Connection conn = primary.getDataSource().getConnection()){
      return getValue(conn, key);
    }
  }

  private String getValue(Connection conn, String key) throws SQLException {
    PreparedStatement stm = conn.prepareStatement("SELECT value from key_value where key = ?");
    stm.setString(1, key);
    ResultSet rs = stm.executeQuery();
    if(!rs.next()){
      return "Unable to get the data";
    }

    return "The Value is " + rs.getInt("value");
  }

  private boolean canUseReplica(Connection conn, String key) throws SQLException {
    String redisKey = WAL_KEY + ":" + key;
    String walPosition = redisTemplate.opsForValue().get(redisKey);

    if(walPosition == null) return true;

    PreparedStatement stm = conn.prepareStatement("SELECT pg_last_wal_replay_lsn() >= ?::pg_lsn");
    stm.setString(1, walPosition);
    stm.executeQuery();
    ResultSet rs = stm.getResultSet();
    rs.next();

    if(rs.getBoolean(1)){
      redisTemplate.delete(redisKey);
      return true;
    }

    return false;
  }

  @PutMapping("/kv/{key}")
  public String update(@PathVariable("key") String key, @RequestBody() int value){
    try(Connection conn = primary.getDataSource().getConnection()){
      boolean success = storeKeyValue(key, value, conn);
      if (!success) return "Something broken about this";
      updateWalPosition(conn, key);

      return "Key stored";
    } catch (SQLException e) {
      return "Something went wrong: " + e.getMessage();
    }
  }

  private void updateWalPosition(Connection conn, String key) throws SQLException {
    PreparedStatement stm = conn.prepareStatement("SELECT pg_current_wal_lsn()");
    ResultSet rs = stm.executeQuery();
    rs.next();

    String position = rs.getString(1);
    redisTemplate.opsForValue().set(WAL_KEY + ":" + key, position, Duration.ofSeconds(5));
  }

  private boolean storeKeyValue(String key, int value, Connection conn) throws SQLException {
    PreparedStatement stm = conn.prepareStatement("INSERT INTO key_value (key, value) VALUES (?, ?)\n" +
      "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value");
    stm.setString(1, key);
    stm.setInt(2, value);

    return stm.executeUpdate() > 0;
  }
}