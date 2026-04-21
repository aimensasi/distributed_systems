package com.distributed_systems.module_3_kafka_service_a;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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

	@Bean()
	public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate){
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
		return new DefaultErrorHandler(recoverer, new FixedBackOff(1000, 2));
	}
}
