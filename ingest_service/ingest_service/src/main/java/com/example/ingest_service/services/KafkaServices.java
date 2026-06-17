package com.example.ingest_service.services;

import com.example.common_dto.CodeChunkEvent;
import com.example.ingest_service.dtos.CodeChunk;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaServices {

    private final KafkaTemplate<String, CodeChunkEvent> kafkaTemplate;
    public KafkaServices(KafkaTemplate<String, CodeChunkEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void producer(CodeChunkEvent chunk) {
//        System.out.println("Chunk-sended : " + chunk.chunkId());
        kafkaTemplate.send("code-chunks", chunk.chunkId(), chunk);
    }
}
