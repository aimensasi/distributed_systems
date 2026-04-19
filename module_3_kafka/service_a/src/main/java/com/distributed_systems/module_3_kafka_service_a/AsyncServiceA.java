package com.distributed_systems.module_3_kafka_service_a;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@SpringBootApplication
public class AsyncServiceA {

	public static void main(String[] args) {
		SpringApplication.run(AsyncServiceA.class, args);
	}

	@Bean()
	public NewTopic userEventTopic(){
		return TopicBuilder.name("user-events")
			.partitions(3)
			.replicas(1)
			.build();
	}

	@Bean()
	public NewTopic backpressureTopic(){
		return TopicBuilder.name("backpressure-test")
			.partitions(3)
			.replicas(1)
			.build();
	}
}
