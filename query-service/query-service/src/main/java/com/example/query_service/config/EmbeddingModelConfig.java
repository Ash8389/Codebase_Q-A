package com.example.query_service.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingModelConfig {

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${jina.baseurl}") String url,
            @Value("${jina.api.key}") String apiKey,
            @Value("${jina.model}") String modelName
    ){
        return OpenAiEmbeddingModel.builder()
                .baseUrl(url)
                .apiKey(apiKey)
                .modelName(modelName)
                .dimensions(1024)
                .build();
    }
}
