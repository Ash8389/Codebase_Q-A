package com.example.embedding_service.config;


import com.example.common_dto.CodeChunkEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CodeChunkEvent>
            batchKafkaListenerContainerFactory(ConsumerFactory<String, CodeChunkEvent> cf) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, CodeChunkEvent>();
        factory.setConsumerFactory(cf);
        factory.setBatchListener(true);

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.BATCH);

        return factory;

    }
}
