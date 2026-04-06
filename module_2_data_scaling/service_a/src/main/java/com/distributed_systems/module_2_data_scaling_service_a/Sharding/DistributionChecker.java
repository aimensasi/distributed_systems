package com.distributed_systems.module_2_data_scaling_service_a.Sharding;

import com.distributed_systems.module_2_data_scaling_service_a.Configuration.HashRing;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Profile("sharding")
@RequestMapping("/lab25")
public class DistributionChecker {

  @GetMapping("/distribution-test")
  public Map<String, Object> distributionTest() {
    int totalKeys = 1000;
    Map<String, Object> result = new HashMap<>();

    // Modulo routing — before (3 nodes)
    Map<String, Integer> moduloBefore = new HashMap<>();
    for (int i = 0; i < totalKeys; i++) {
      String shard = "shard" + (i % 3);
      moduloBefore.merge(shard, 1, Integer::sum);
    }

    // Modulo routing — after (4 nodes)
    Map<String, Integer> moduloAfter = new HashMap<>();
    for (int i = 0; i < totalKeys; i++) {
      String shard = "shard" + (i % 4);
      moduloAfter.merge(shard, 1, Integer::sum);
    }

    // Count remapped keys with modulo
    int moduloRemapped = 0;
    for (int i = 0; i < totalKeys; i++) {
      if ((i % 3) != (i % 4)) moduloRemapped++;
    }

    // Consistent hashing — before (3 nodes)
    HashRing ringBefore = new HashRing();
    ringBefore.addNode("shard0");
    ringBefore.addNode("shard1");
    ringBefore.addNode("shard2");

    // Consistent hashing — after (4 nodes)
    HashRing ringAfter = new HashRing();
    ringAfter.addNode("shard0");
    ringAfter.addNode("shard1");
    ringAfter.addNode("shard2");
    ringAfter.addNode("shard3");

    // Count remapped keys with consistent hashing
    int hashRemapped = 0;
    Map<String, Integer> hashBefore = new HashMap<>();
    Map<String, Integer> hashAfter = new HashMap<>();

    for (int i = 0; i < totalKeys; i++) {
      String before = ringBefore.getNode(String.valueOf(i));
      String after = ringAfter.getNode(String.valueOf(i));
      if (!before.equals(after)) hashRemapped++;
      hashBefore.merge(before, 1, Integer::sum);
      hashAfter.merge(after, 1, Integer::sum);
    }

    result.put("modulo_remapped", moduloRemapped + " / " + totalKeys + " (" + (moduloRemapped * 100 / totalKeys) + "%)");
    result.put("consistent_hash_remapped", hashRemapped + " / " + totalKeys + " (" + (hashRemapped * 100 / totalKeys) + "%)");
    result.put("modulo_distribution_before", moduloBefore);
    result.put("modulo_distribution_after", moduloAfter);
    result.put("hash_distribution_before", hashBefore);
    result.put("hash_distribution_after", hashAfter);

    return result;
  }
}
