package com.distributed_systems.module_1_coordination_service_a.Services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RedisService {

  private final StringRedisTemplate stringRedisTemplate;

  public RedisService(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public String acquireLock(String key, Duration duration){
    String lockKey = UUID.randomUUID().toString();
    boolean lockAcquired = false;
    Instant start = Instant.now();

    while (!lockAcquired && Duration.between(start, Instant.now()).compareTo(duration) >= 0){
      lockAcquired = stringRedisTemplate.opsForValue()
        .setIfAbsent("lockForTransferAToB", lockKey, Duration.ofSeconds(2));
    }

    if(lockAcquired) {
      return lockKey;
    }

    return null;
  }

  public void releaseLock(String key, String ticket){
    if(stringRedisTemplate.opsForValue().get(key).equals(ticket)){
      stringRedisTemplate.delete(key);
    }
  }
}
