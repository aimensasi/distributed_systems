package com.distributed_systems.module_1_coordination_service_b;

import com.distributed_systems.module_1_coordination_service_b.Services.Lock;
import com.distributed_systems.module_1_coordination_service_b.Services.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.logging.Logger;

@EnableScheduling
@RestController
public class LeaderElection {

  private final RedisService redisService;

  @Value("${server.port}")
  private int serverPort;

  public LeaderElection(RedisService redisService){
    this.redisService = redisService;
  }

  @Scheduled(fixedDelay = 5000)
  public void runBroken(){
    System.out.println("[SERVICE_B] Running broken scheduled job");
  }

  @Scheduled(fixedDelay = 5000)
  public void runFixed() throws UnknownHostException {
    String instanceId = InetAddress.getLocalHost().getHostName() + ":" + serverPort;
    String lockKey = "leader:election";
    boolean isCurrentLeader = redisService.isLeader(lockKey, instanceId);

    if(isCurrentLeader) {
      redisService.renewLock(lockKey, instanceId, Duration.ofSeconds(10));
    } else {
      Lock lock = redisService.acquireLock(
        lockKey,
        instanceId,
        Duration.ofSeconds(10),
        Duration.ofSeconds(1)
      );

      if(lock == null) return;
    }


    System.out.println("[SERVICE_B] Running fixed scheduled job");
  }

}
