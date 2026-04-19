package com.distributed_systems.module_3_kafka_service_a;

import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/lab32b")
@EnableScheduling
public class Lab32bController {
  @Autowired
  private PlatformTransactionManager transactionManager;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final JdbcTemplate jdbcTemplate;

  public Lab32bController(KafkaTemplate<String, String> kafkaTemplate, JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }

  @GetMapping("orders/{userId}/amount/{amount}")
  public String produce(@PathVariable("userId") int userId, @PathVariable("amount") int amount){
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.execute(status -> {
      KeyHolder keyHolder = new GeneratedKeyHolder();
      jdbcTemplate.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO orders (userId, amount) VALUES (?, ?)",
          new String[]{"id"}
        );
        ps.setInt(1, userId);
        ps.setInt(2, amount);
        return ps;
      }, keyHolder);

      int orderId = keyHolder.getKey().intValue();

      String payload = new Gson().toJson(Map.of(
        "userId", userId,
        "amount", amount,
        "orderId", orderId,
        "uuid", UUID.randomUUID().toString()
      ));
      jdbcTemplate.update(
        "INSERT INTO outbox (topic, partition_key, payload) VALUES (?, ?, ?)",
        "order-received", String.valueOf(userId), payload
      );

      return null;
    });


    return "Done";
  }

  @Scheduled(fixedDelay = 500)
  public void publishOutboxEvents(){
    List<Map<String, String>> result = jdbcTemplate.query(
      "SELECT * FROM outbox WHERE published = false", (rs, rowNum) -> {
        return Map.of(
            "id",  rs.getString("id"),
          "topic", rs.getString("topic"),
          "key", rs.getString("partition_key"),
          "payload", rs.getString("payload")
        );
      }
    );

//    System.out.printf("Publishing %s events\n", result.size());

    for (Map<String, String> event : result){
      int outboxId = Integer.parseInt(event.get("id"));
      kafkaTemplate.send(event.get("topic"), event.get("partition_key"), event.get("payload"));

//      if(outboxId == 5) {
//        throw new RuntimeException();
//      }

      jdbcTemplate.update("UPDATE outbox SET published = true where id = ?", outboxId);
    }

  }


  @GetMapping("/state/all")
  public List<String> allState() {
    return jdbcTemplate.query(
      "SELECT o.id, o.userId, o.amount, n.id AS notif_id FROM orders o LEFT JOIN order_notifications n ON o.id = n.orderId",
      (rs, rowNum) -> "Order: " + rs.getInt("id") +
        " user=" + rs.getInt("userId") +
        " amount=" + rs.getInt("amount") +
        " notified=" + (rs.getObject("notif_id") != null ? "yes" : "NO")
    );
  }

  @KafkaListener(topics = "order-received", id = "orders-completed")
  public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) throws InterruptedException {
    String data = record.value();
    Map<String, Object> result = new Gson().fromJson(data, Map.class);
    int userId = ((Number) result.get("userId")).intValue();
    int orderId = ((Number) result.get("orderId")).intValue();
    int amount = ((Number) result.get("amount")).intValue();
    String dedupeKey = result.get("uuid").toString();


    boolean isNotProcessed = jdbcTemplate.query("SELECT dedup_key from processed_offsets WHERE dedup_key = ? LIMIT 1",
      (rs, rn) -> rs.getString("dedup_key"), dedupeKey).isEmpty();

    System.out.printf("Consumed %s, for %s, %s\n", orderId, userId, isNotProcessed);
    if(!isNotProcessed) {
      ack.acknowledge();
      return;
    }

    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.execute(status -> {
      jdbcTemplate.update("INSERT INTO order_notifications (userId, orderId, amount) VALUES (?, ?, ?)", userId, orderId, amount);
      jdbcTemplate.update("INSERT INTO processed_offsets (dedup_key) VALUES (?)", dedupeKey);
      return null;
    });

    ack.acknowledge();
  }
}

