package com.distributed_systems.module_2_data_scaling_service_a.Sharding;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@Profile("sharding")
@RequestMapping("/lab24/orders")
public class OrderController {

  private ShardRouter shardRouter;
  private Executor queryExecutorPool;

  public OrderController(ShardRouter shardRouter, @Qualifier("queryExecutorPool") Executor queryExecutorPool){
    this.shardRouter = shardRouter;
    this.queryExecutorPool = queryExecutorPool;
  }


  @PostMapping("/{userId}/{amount}")
  public String store(@PathVariable("userId") int userId, @PathVariable("amount") double amount){
    List<JdbcTemplate> targets = shardRouter.getShardsForWrite(userId);
    targets.forEach((datasource) -> datasource.update("INSERT INTO orders (user_id, amount) VALUES (?,?)", userId, amount));

    return "Done";
  }

  @GetMapping("/{userId}")
  public List<String> get(@PathVariable("userId") int userId){
    JdbcTemplate datasource = shardRouter.getShardForRead(userId);

    return datasource.query(
      "SELECT id, user_id, amount, created_at FROM orders WHERE user_id = ?",
      (rs, rowNum) -> "Order " + rs.getInt("id") + " - $" + rs.getBigDecimal("amount"),
      userId
    );
  }

  @GetMapping("/all")
  public List<String> getAll() {
    List<JdbcTemplate> shards = shardRouter.getAllShards();
    List<String> result = new ArrayList<>();
    int amountLimit = 50;

     for (JdbcTemplate shard: shards) {
       List<String> shardResult = shard.query(
         "SELECT id, user_id, amount, created_at FROM orders WHERE amount > ?",
         (rs, rowNum) -> "Order " + rs.getInt("id") + " - $" + rs.getBigDecimal("amount"),
         amountLimit
       );

       result.addAll(shardResult);
     }

     return result;
  }

  @GetMapping("/all/async")
  public List<String> getAllAsync() {
    List<JdbcTemplate> shards = shardRouter.getAllShards();
    int amountLimit = 50;

    List<CompletableFuture<List<String>>> futures = shards.stream()
      .map((JdbcTemplate shard) -> {
        return CompletableFuture.supplyAsync(() -> {
          return shard.query(
            "SELECT id, user_id, amount, created_at FROM orders WHERE amount > ?",
            (rs, rowNum) -> "Order " + rs.getInt("id") + " - $" + rs.getBigDecimal("amount"),
            amountLimit
          );
        }, queryExecutorPool);
      }).toList();


    return futures.stream()
      .map(CompletableFuture::join)
      .flatMap(List::stream)
      .toList();
  }

  @GetMapping("/backfill")
  public String backfill(){

    List<JdbcTemplate> shards = shardRouter.getAllShards();

    for (JdbcTemplate shard: shards){
      int lastId = 0;
      while (true){
        List<Integer> ids = shard.query(
          "SELECT DISTINCT user_id from orders WHERE user_id > ? ORDER BY user_id LIMIT 1000",
          (res, row) -> res.getInt("user_id"),
          lastId
          );

        if(ids.isEmpty()) break;
        lastId = ids.getLast();

        System.out.printf("Batch to Process Before Check  %s\n", ids.toString());
        List<Integer> impacted = ids.stream().filter((id) -> shardRouter.isImpacted(id)).toList();

        if(!impacted.isEmpty()) {
          processBatch(impacted, shard);
        }
      }
    }

    return "Done";
  }

  private void processBatch(List<Integer> ids, JdbcTemplate sourceShard) {
    JdbcTemplate targetShard = shardRouter.getShardByName("shard3");


    for (int userId : ids) {
      List<Map<String, Object>> rows = sourceShard.queryForList(
        "SELECT user_id, amount, created_at FROM orders WHERE user_id = ?", userId);

      for (Map<String, Object> row : rows) {
        Integer exists = targetShard.queryForObject(
          "SELECT count(*) FROM orders WHERE user_id = ? AND created_at = ?",
          Integer.class,
          row.get("user_id"),
          row.get("created_at")
        );

        if (exists == 0) {
          targetShard.update(
            "INSERT INTO orders (user_id, amount, created_at) VALUES (?, ?, ?)",
            row.get("user_id"),
            row.get("amount"),
            row.get("created_at")
          );
        }
      }
    }
  }
}
