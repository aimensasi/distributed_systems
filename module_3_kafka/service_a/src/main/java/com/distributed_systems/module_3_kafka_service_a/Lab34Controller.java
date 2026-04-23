package com.distributed_systems.module_3_kafka_service_a;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/lab34")
public class Lab34Controller {
  private KafkaTemplate<String, String> kafkaTemplate;
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private PlatformTransactionManager transactionManager;
  AtomicInteger processedMessage = new AtomicInteger();

  public Lab34Controller(KafkaTemplate<String, String> kafkaTemplate, JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }

  @GetMapping("/send")
  public String produce(@RequestParam("message") String message){
    kafkaTemplate.send("processing-message", message);
    return "Done";
  }

  @GetMapping("status")
  public int status(){
    return processedMessage.get();
  }


  @KafkaListener(topics = "processing-message", id = "processing-message-consumer-1")
  public void consume(String data) throws InterruptedException {
    Map<String, Object> payload = new Gson().fromJson(data, Map.class);
    String value = (String) payload.get("value");
    processedMessage.incrementAndGet();
  }
}

