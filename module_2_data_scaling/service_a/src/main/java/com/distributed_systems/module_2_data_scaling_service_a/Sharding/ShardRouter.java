package com.distributed_systems.module_2_data_scaling_service_a.Sharding;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Component
@Profile("sharding")
public class ShardRouter {

  private final int NODE_SIZE = 3;
  private final Map<Integer, JdbcTemplate> shards;

  public ShardRouter(@Qualifier("shard0Template") JdbcTemplate shard0Template,
                     @Qualifier("shard1Template") JdbcTemplate shard1Template,
                     @Qualifier("shard2Template") JdbcTemplate shard2Template){
    shards = new HashMap<>();
    shards.put(0, shard0Template);
    shards.put(1, shard1Template);
    shards.put(2, shard2Template);
  }

  public JdbcTemplate getShard(int userId) {
    int targetShard = userId % NODE_SIZE;

    return shards.get(targetShard);
  }

  public List<JdbcTemplate> getAllShards(){
    return shards.values().stream().toList();
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
