package com.distributed_systems.module_1_coordination_service_a.Services;

public record Lock(String ticket, long fencingToken) {
}
