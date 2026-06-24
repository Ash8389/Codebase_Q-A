package com.example.query_service.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@RequiredArgsConstructor
@Configuration
public class StreamChatModel {
    private final RedisChatMemory redisChatMemory;

    @Bean
    public OpenAiStreamingChatModel streamLanguageModel(
            @Value("${groq.baseurl}")
            String url,
            @Value("${groq.api.key}")
            String api,
            @Value("${groq.model}")
            String model
    ) {

        return OpenAiStreamingChatModel.builder()
                .baseUrl(url)
                .apiKey(api)
                .modelName(model)
                .logRequests(true)
                .logResponses(true)
                .maxTokens(600)
                .temperature(0.5)
                .build();
    }

    @Bean
    public StreamingChatAssistant streamChatAssistant(
            OpenAiStreamingChatModel chatLanguageModel
    ){
        return AiServices.builder(StreamingChatAssistant.class)
                .streamingChatModel(chatLanguageModel)
                .chatMemoryProvider(memoryID ->
                        MessageWindowChatMemory
                                .builder()
                                .id(memoryID)
                                .chatMemoryStore(redisChatMemory)
                                .maxMessages(5)
                                .build()
                )
                .build();
    }
}
