package com.distributed_systems.module_1_coordination_service_b;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class RateLimiting {
  private final AtomicInteger counter = new AtomicInteger(0);
  private static final int LIMIT = 10;
  private final StringRedisTemplate stringRedisTemplate;

  public RateLimiting(StringRedisTemplate stringRedisTemplate){
    this.stringRedisTemplate = stringRedisTemplate;
    Executors.newSingleThreadScheduledExecutor()
      .scheduleAtFixedRate(() -> counter.set(0), 1, 1, TimeUnit.MINUTES);
  }

  @GetMapping("api/data-broken")
  public ResponseEntity<String> processBroken(){
    if(counter.getAndIncrement() >= LIMIT){
      return ResponseEntity.status(429).body("Rate Limit Reached");
    }


    return ResponseEntity.ok("Done");
  }

  @GetMapping("api/data-fixed")
  public ResponseEntity<String> processFixed(){
    String luaScript = """
      local count = redis.call('INCR', KEYS[1])
      if count == 1 then
          redis.call('EXPIRE', KEYS[1], ARGV[1])
      end
      return count
      """;

    Long count = stringRedisTemplate.execute(
      new DefaultRedisScript<>(luaScript, Long.class),
      List.of("rate:limit"),
      "60"
    );

    if(count >= LIMIT){
      return ResponseEntity.status(429).body("Rate Limit Reached");
    }


    return ResponseEntity.ok("Done");
  }
}
