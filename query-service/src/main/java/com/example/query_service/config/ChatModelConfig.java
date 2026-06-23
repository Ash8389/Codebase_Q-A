package com.example.query_service.config;

import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class ChatModelConfig {

    private final RedisChatMemory redisChatMemory;

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${groq.baseurl}")
            String url,
            @Value("${groq.api.key}")
            String api,
            @Value("${groq.model}")
            String model
    ) {

        return OpenAiChatModel.builder()
                .baseUrl(url)
                .apiKey(api)
                .modelName(model)
                .maxTokens(600)
                .temperature(0.2)
                .build();
    }

    @Bean
    public ChatAssistant chatAssistant(
            ChatLanguageModel chatLanguageModel
    ){
        return AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(memoryID ->
                        TokenWindowChatMemory
                                .builder()
                                .id(memoryID)
                                .chatMemoryStore(redisChatMemory)
                                .maxTokens(2000, new OpenAiTokenizer())
                                .build()
                )
                .build();
    }
}
