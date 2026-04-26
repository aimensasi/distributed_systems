package com.distributed_systems.module_3_kafka_service_b;

import com.google.gson.Gson;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

@Component
public class InventoryService {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final JdbcTemplate jdbcTemplate;

  public InventoryService(KafkaTemplate<String, String> kafkaTemplate, JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }


  @KafkaListener(topics = "ORDER_CREATED", id = "inventory-manager-order-created")
  public void create(String eventPayload) {
    System.out.printf("Processing Order %s \n", eventPayload);
    Map<String, Object> result = new Gson().fromJson(eventPayload, Map.class);
    int orderId = ((Number) result.get("orderId")).intValue();
    int productId = ((Number) result.get("productId")).intValue();
    int quantity = ((Number) result.get("quantity")).intValue();

    System.out.printf("Processing Order %s, for %s with quantity of \n", orderId, productId, quantity);

    int updated = jdbcTemplate.update(
      "UPDATE inventory SET stock = stock - ? WHERE productId = ? AND stock >= ?",
      quantity,
      productId,
      quantity
    );

    if(updated == 0) {
      kafkaTemplate.send("INVENTORY_OUT_OF_STOCK", String.valueOf(orderId), eventPayload);
    } else {
      kafkaTemplate.send("INVENTORY_RESERVED", String.valueOf(orderId), eventPayload);
    }
  }

  @KafkaListener(topics = "PAYMENT_FAILED", id = "inventory-manager-payment-failed")
  public void onPaymentFailed(String eventPayload) {
    System.out.printf("Processing Order %s \n", eventPayload);
    Map<String, Object> result = new Gson().fromJson(eventPayload, Map.class);
    int orderId = ((Number) result.get("orderId")).intValue();
    int productId = ((Number) result.get("productId")).intValue();
    int quantity = ((Number) result.get("quantity")).intValue();

    System.out.printf("Processing Order %s, for %s with quantity of \n", orderId, productId, quantity);

    int updated = jdbcTemplate.update(
      "UPDATE inventory SET stock = stock + ? WHERE productId = ?",
      quantity,
      productId
    );

    kafkaTemplate.send("INVENTORY_RELEASED", String.valueOf(orderId), eventPayload);
  }
}
