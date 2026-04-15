package com.distributed_systems.module_3_kafka_service_a;

import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/lab32")
public class Lab32Controller {
  private KafkaTemplate<String, String> kafkaTemplate;
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private PlatformTransactionManager transactionManager;


  public Lab32Controller(KafkaTemplate<String, String> kafkaTemplate, JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }

  @GetMapping("/{userId}/events/{event}")
  public String produce(@PathVariable("userId") int userId, @PathVariable("event") String event){
    String jsonStr = new Gson().toJson(Map.of("userId", userId, "event", event));
    int partition = (int)(Math.random() * 3);
    kafkaTemplate.send("user-events", String.valueOf(userId), jsonStr).thenAccept(result -> {
      System.out.printf("Sent to partition %d, offset %d%n",
        result.getRecordMetadata().partition(),
        result.getRecordMetadata().offset());
    });
    return "Done";
  }

  @GetMapping("/state/{userId}")
  public String status(@PathVariable("userId") int userId){
    return jdbcTemplate.query(
      "SELECT * FROM events WHERE userId = ? LIMIT 1;",
      (result, rowNum) -> "Event: " + result.getInt("userId") + " - " + result.getString("event"),
      userId
    ).getFirst();
  }

  @GetMapping("/state/all")
  public List<String> allState() {
    return jdbcTemplate.query(
      "SELECT * FROM events;",
      (result, rowNum) -> "Event: " + result.getInt("userId") + " - " + result.getString("event"));
  }

  @KafkaListener(topics = "user-events", id = "user-event-consumer-2")
  public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) throws InterruptedException {
    String data = record.value();
    int partition = record.partition();
    long offset = record.offset();
    String dedupeKey = partition + "-" + offset;
    Map<String, Object> result = new Gson().fromJson(data, Map.class);
    int userId = ((Number) result.get("userId")).intValue();
    String event = (String) result.get("event");


    Thread.sleep((long)(Math.random() * 100));

    boolean isNotProcessed = jdbcTemplate.query("SELECT dedup_key from processed_offsets WHERE dedup_key = ? LIMIT 1",
      (rs, rn) -> rs.getString("dedup_key"), dedupeKey).isEmpty();

    System.out.printf("Consumed %s, for %s, %s\n", event, userId, isNotProcessed);
    if(!isNotProcessed) {
      ack.acknowledge();
      return;
    }

    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.execute(status -> {
      jdbcTemplate.update("INSERT INTO events (userId, event) VALUES (?, ?)", userId, event);
      jdbcTemplate.update("INSERT INTO processed_offsets (dedup_key) VALUES (?)", dedupeKey);
      return null;
    });

    if (event.equals("UPDATED") && userId == 1) {
      System.out.println("SIMULATING CRASH — killing before offset commit");
      throw new RuntimeException();
    }


    ack.acknowledge();
  }
}

