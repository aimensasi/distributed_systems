package com.distributed_systems.module_2_data_scaling_service_a.Sharding;

import com.distributed_systems.module_2_data_scaling_service_a.Configuration.HashRing;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Component
@Profile("sharding")
public class ShardRouter {

  private final int NODE_SIZE = 3;
  private final Map<String, JdbcTemplate> shards;
  private final HashRing stableRing = new HashRing();
  private final HashRing hashRing = new HashRing();

  @Value("${migration.state:STABLE}")
  private MigrationState migrationState;

  public ShardRouter(@Qualifier("shard0Template") JdbcTemplate shard0Template,
                     @Qualifier("shard1Template") JdbcTemplate shard1Template,
                     @Qualifier("shard2Template") JdbcTemplate shard2Template,
                     @Qualifier("shard3Template") JdbcTemplate shard3Template){
    shards = new HashMap<>();
    shards.put("shard0", shard0Template);
    shards.put("shard1", shard1Template);
    shards.put("shard2", shard2Template);
    shards.put("shard3", shard3Template);
  }

  @PostConstruct
  private void initializeHashRing(){
    System.out.printf("Migrating State %s\n", migrationState);

    if (migrationState == MigrationState.MIGRATING) {
      List.of("shard0", "shard1", "shard2").forEach(stableRing::addNode);
    }

    List.of("shard0", "shard1", "shard2", "shard3").forEach(hashRing::addNode);
  }

  public JdbcTemplate getShardForRead(int userId) {
    String targetShard = hashRing.getNode(String.valueOf(userId));

    if(migrationState == MigrationState.MIGRATING) {
      targetShard = stableRing.getNode(String.valueOf(userId));
    }

    return shards.get(targetShard);
  }

  public List<JdbcTemplate> getShardsForWrite(int userId) {
    JdbcTemplate node = shards.get(hashRing.getNode(String.valueOf(userId)));
    if(migrationState != MigrationState.MIGRATING) {
      return List.of(node);
    }

    JdbcTemplate stableNode = shards.get(stableRing.getNode(String.valueOf(userId)));

    if(node.equals(stableNode)) {
      // the shard did not move, only 25% is impacted by the migration
      return List.of(node);
    } else {
      // did move, return new and old
      return List.of(node, stableNode);
    }
  }

  public List<JdbcTemplate> getAllShards(){
    if(migrationState == MigrationState.MIGRATING) {
      return List.of(shards.get("shard0"), shards.get("shard1"), shards.get("shard2"));
    }

    return shards.values().stream().toList();
  }

  public boolean isImpacted(int userId){
    if(migrationState != MigrationState.MIGRATING) return false;

    String stableNode = stableRing.getNode(String.valueOf(userId));
    String newNode = hashRing.getNode(String.valueOf(userId));

    return !stableNode.equals(newNode);
  }

  public JdbcTemplate getShardByName(String name){
    return shards.get(name);
  }

  @Bean("queryExecutorPool")
  public Executor queryExecutorPool() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(shards.size() * 2);
    executor.setThreadNamePrefix("query-executor-");
    executor.initialize();
    return executor;
  }
}
