package com.distributed_systems.module_3_kafka_service_a;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/lab31")
public class Lab31Controller {
  private KafkaTemplate<String, String> kafkaTemplate;
  ConcurrentHashMap<Integer, String> state = new ConcurrentHashMap<>();

  public Lab31Controller(KafkaTemplate<String, String> kafkaTemplate) {
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
    String current = state.get(userId);

    System.out.printf("Current State %s, for %s\n", current, userId);

    return current;
  }

  @GetMapping("/state/all")
  public Map<Integer, String> allState() {
    return state;
  }

  @KafkaListener(topics = "user-events", id = "user-event-consumer")
  public String consume(String data) throws InterruptedException {
    Map<String, Object> result = new Gson().fromJson(data, Map.class);
    int userId = ((Number) result.get("userId")).intValue();
    String event = (String) result.get("event");
    System.out.printf("Consumed %s, for %s\n", event, userId);

    Thread.sleep((long)(Math.random() * 100));
    state.put(userId, event);
    return "Done";
  }
}
