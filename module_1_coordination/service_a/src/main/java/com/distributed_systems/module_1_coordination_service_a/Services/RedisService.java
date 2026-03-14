package com.distributed_systems.module_1_coordination_service_a.Services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;



@Service
public class RedisService {

  private final StringRedisTemplate stringRedisTemplate;

  public RedisService(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public Lock acquireLock(String key, Duration duration){
    return acquireLock(key, null, duration, duration);
  }

  public Lock acquireLock(String key, String value, Duration duration){
    return acquireLock(key, value, duration, duration);
  }

  public Lock acquireLock(String key, Duration duration, Duration timeout){
    return acquireLock(key, null, duration, timeout);
  }

  public Lock acquireLock(String key, String value, Duration duration, Duration timeout){
    Long fencingToken = stringRedisTemplate.opsForValue().increment("fence:" + key);
    String keyValue = value == null ? UUID.randomUUID().toString() : value;
    boolean lockAcquired = false;
    Instant start = Instant.now();


    while (!lockAcquired && Duration.between(start, Instant.now()).compareTo(timeout) <= 0){
      lockAcquired = stringRedisTemplate.opsForValue()
        .setIfAbsent(key, keyValue, duration);

      if(!lockAcquired){
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return null;
        }
      }
    }

    if(lockAcquired) {
      return new Lock(keyValue, fencingToken);
    }

    return null;
  }

  public boolean isLeader(String key, String ticket){
    String value = stringRedisTemplate.opsForValue().get(key);
    if(Objects.equals(value, ticket)){
      return true;
    }

    return false;
  }

  public boolean renewLock(String key, String ticket, Duration ttl){
    String value = stringRedisTemplate.opsForValue().get(key);
    if(Objects.equals(value, ticket)){
      stringRedisTemplate.expire(key, ttl);
      return true;
    }

    return false;
  }

  public void releaseLock(String key, String ticket){
    if(Objects.equals(stringRedisTemplate.opsForValue().get(key), ticket)){
      stringRedisTemplate.delete(key);
    }
  }
}