package com.distributed_systems.module_2_data_scaling_service_a.Configuration;


import java.util.*;

public class HashRing {
  private final int VIRTUAL_NODES_COUNT = 120;
  private final TreeMap<Integer, String> nodes = new TreeMap<>();
  private final Map<String, List<Integer>> virtualNodes = new HashMap<>();

  public void addNode(String node) {
    List<Integer> positions = new ArrayList<>();

    for (int i = 1; i <= VIRTUAL_NODES_COUNT; i++) {
      int key = getKey(node + "-" + i);
      nodes.put(key, node);
      positions.add(key);
    }

    virtualNodes.put(node, positions);
  }

  public void removeNode(String node){
    List<Integer> positions = virtualNodes.get(node);

    for (Integer position: positions) {
      nodes.remove(position);
    }

    virtualNodes.remove(node);
  }

  public String getNode(String id){
    int key = getKey(id);
    Integer foundKey = nodes.ceilingKey(key);

    if(foundKey == null) {
      return nodes.firstEntry().getValue();
    }

    return nodes.get(foundKey);
  }


  private int getKey(String nodeId){
    int h = Objects.hash(nodeId);

    // 2. Apply a 32-bit mixer to "scatter" the bits
    h ^= h >>> 16;
    h *= 0x85ebca6b;
    h ^= h >>> 13;
    h *= 0xc2b2ae35;
    h ^= h >>> 16;

    return h;
  }
}
