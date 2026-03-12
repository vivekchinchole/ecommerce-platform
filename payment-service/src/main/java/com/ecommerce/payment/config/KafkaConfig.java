package com.ecommerce.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic paymentTopic() {
        return new NewTopic("payment.completed", 3, (short) 1);
    }
}

//@Configuration
//public class KafkaConfig {
//
//    @Bean
//    public NewTopic paymentTopic() {
//        return TopicBuilder.name("payment-events")
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//}