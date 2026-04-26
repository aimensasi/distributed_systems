package com.distributed_systems.module_3_kafka_service_b;

import com.google.gson.Gson;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentService {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final JdbcTemplate jdbcTemplate;

  public PaymentService(KafkaTemplate<String, String> kafkaTemplate, JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }


  @KafkaListener(topics = "INVENTORY_RESERVED", id = "payment-manager")
  public void create(String eventPayload) {
    Map<String, Object> result = new Gson().fromJson(eventPayload, Map.class);
    int orderId = ((Number) result.get("orderId")).intValue();
    int productId = ((Number) result.get("productId")).intValue();
    int quantity = ((Number) result.get("quantity")).intValue();

    System.out.printf("Processing Order %s, for %s with quantity of \n", orderId, productId, quantity);
    double productPrice = 9.99;


    if(quantity == 5) {
      kafkaTemplate.send("PAYMENT_FAILED", String.valueOf(orderId), eventPayload);
    } else {
    jdbcTemplate.update(
      "INSERT INTO payments (orderId, amount, status) VALUES (?, ?, ?)",
      orderId,
      quantity * productPrice,
      "CHARGED"
    );
      kafkaTemplate.send("PAYMENT_COMPLETED", String.valueOf(orderId), eventPayload);
    }

  }
}
