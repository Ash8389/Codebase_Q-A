package com.example.ingest_service.configuration;

import com.example.common_dto.CodeChunkEvent;
import com.example.ingest_service.dtos.CodeChunk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, CodeChunkEvent> kafkaTemplate(
            ProducerFactory<String, CodeChunkEvent> producerFactory
    ){
        return new KafkaTemplate<>(producerFactory);
    }
}
