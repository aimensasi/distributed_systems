package com.distributed_systems.module_3_kafka_saga_orchestrator;

import com.google.gson.Gson;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

@RestController
@RequestMapping("lab36/orders")
public class SagaOrchestratorController {


  private final KafkaTemplate<String, String> kafkaTemplate;
  private final JdbcTemplate jdbcTemplate;

  public SagaOrchestratorController(KafkaTemplate<String, String> kafkaTemplate, JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.kafkaTemplate = kafkaTemplate;
  }

  @GetMapping("{userId}/{productId}/{quantity}")
  public String create(
    @PathVariable("userId") int userId,
    @PathVariable("productId") int productId,
    @PathVariable("quantity") int quantity)
  {
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO orders (userId, productId, quantity) VALUES (?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS // Inform JDBC to return keys
      );
      ps.setLong(1, userId);
      ps.setLong(2, productId);
      ps.setInt(3, quantity);
      return ps;
    }, keyHolder);

    int newOrderId = (int) keyHolder.getKeys().get("id");
    updateTracker(newOrderId, "ORDER_CREATED", 1);

    String payload = new Gson().toJson(
      Map.of("orderId", newOrderId, "productId", productId, "quantity", quantity, "userId", userId)
    );
    kafkaTemplate.send("RESERVE_INVENTORY", String.valueOf(newOrderId), payload);
    return "Done";
  }


  @KafkaListener(topics = "INVENTORY_RESERVED_2", id = "saga_orc_inventory_handler")
  public void inventoryHandler(String data){
    Map<String, Object> result = new Gson().fromJson(data, Map.class);
    int orderId = ((Number) result.get("orderId")).intValue();
    int productId = ((Number) result.get("productId")).intValue();
    int quantity = ((Number) result.get("quantity")).intValue();

    System.out.printf("Inventory reserved for %s, for %s with quantity of \n", orderId, productId);

    updateTracker(orderId, "INVENTORY_RESERVED", 2);

    kafkaTemplate.send("CHARGE_CUSTOMER", String.valueOf(orderId), data);
  }

  @KafkaListener(topics = "INVENTORY_FAILED_2", id = "saga_orc_inventory_failed_handler")
  public void inventoryFailedHandler(String data){
    Map<String, Object> result = new Gson().fromJson(data, Map.class);
    int orderId = ((Number) result.get("orderId")).intValue();
    int productId = ((Number) result.get("productId")).intValue();

    System.out.printf("Inventory failed for %s, for %s with quantity of \n", orderId, productId);

    updateTracker(orderId, "INVENTORY_FAILED", 2);

    jdbcTemplate.update("UPDATE orders SET status = ? WHERE orderId = ?", "Failed", orderId);
  }

  @KafkaListener(topics = "CUSTOMER_CHARGED_2", id = "saga_orc_payment_charged")
  public void paymentSuccessHandler(String data){
    Map<String, Object> result = new Gson().fromJson(data, Map.class);
    int orderId = ((Number) result.get("orderId")).intValue();
    int productId = ((Number) result.get("productId")).intValue();
    int quantity = ((Number) result.get("quantity")).intValue();

    System.out.printf("Payment Charged for %s, for %s with quantity of \n", orderId, productId);

    updateTracker(orderId, "PAYMENT_CHARGED", 3);

    jdbcTemplate.update("UPDATE orders SET status = ? WHERE orderId = ?", "Completed", orderId);
  }

  @KafkaListener(topics = "CUSTOMER_DIDNOT_CHARGED_2", id = "saga_orc_payment_failed_charged")
  public void paymentFailedHandler(String data){
    Map<String, Object> result = new Gson().fromJson(data, Map.class);
    int orderId = ((Number) result.get("orderId")).intValue();
    int productId = ((Number) result.get("productId")).intValue();
    int quantity = ((Number) result.get("quantity")).intValue();

    System.out.printf("Payment Did not Charged for %s, for %s with quantity of \n", orderId, productId);

    updateTracker(orderId, "PAYMENT_FAILED", 3);

    jdbcTemplate.update("UPDATE orders SET status = ? WHERE orderId = ?", "FAILED", orderId);
    kafkaTemplate.send("RELEASE_INVENTORY_2", String.valueOf(orderId), data);
  }

  private void updateTracker(int orderId, String status, int step) {
    jdbcTemplate.update("INSERT INTO saga_state (orderId, status, step) VALUES (?, ?, ?)", orderId, status, step);
  }
}
