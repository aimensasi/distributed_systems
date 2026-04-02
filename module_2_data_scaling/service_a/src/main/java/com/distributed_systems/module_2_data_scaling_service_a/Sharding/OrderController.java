package com.distributed_systems.module_2_data_scaling_service_a.Sharding;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    JdbcTemplate datasource = shardRouter.getShard(userId);
    datasource.update("INSERT INTO orders (user_id, amount) VALUES (?,?)", userId, amount);

    return "Done";
  }

  @GetMapping("/{userId}")
  public List<String> get(@PathVariable("userId") int userId){
    JdbcTemplate datasource = shardRouter.getShard(userId);

    return datasource.query(
      "SELECT id, user_id, amount, created_at FROM orders WHERE amount = ?",
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
}
