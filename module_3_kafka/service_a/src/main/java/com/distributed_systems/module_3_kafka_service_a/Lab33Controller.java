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
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/lab33")
public class Lab33Controller {
  private KafkaTemplate<String, String> kafkaTemplate;
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private PlatformTransactionManager transactionManager;

  AtomicInteger produced = new AtomicInteger();
  AtomicInteger consumed = new AtomicInteger();


  public Lab33Controller(KafkaTemplate<String, String> kafkaTemplate, JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }

  @GetMapping("/burst/{count}")
  public String produce(@PathVariable("count") int count){
    for (int i = 1; i <= count; i++) {
      String key = String.valueOf(produced.incrementAndGet());
      kafkaTemplate.send("backpressure-test", key, key);
    }
    return "Done";
  }

  @GetMapping("/lag")
  public Map<String, Integer> allState() {
    return Map.of("produced", produced.get(), "consumed", consumed.get());
  }

  @KafkaListener(topics = "backpressure-test", id = "consumer-1")
  public void consume(String data) throws InterruptedException {
    Thread.sleep(2_000);
    System.out.printf("Received count %s \n", consumed.incrementAndGet());
  }
}

