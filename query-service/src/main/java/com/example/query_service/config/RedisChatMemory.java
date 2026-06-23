package com.example.query_service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<ChatMessage> getMessages(Object memoryID) {

        String redisKey = memoryKey((String) memoryID);
        String stored = redisTemplate.opsForValue().get(redisKey);

        log.info("Loading memory: {}", memoryID);
        log.info("Stored value: {}", stored);

        if(stored == null || stored.isBlank()){
            log.info("Returning EMPTY memory");
            return new ArrayList<>();
        }

        try {
            List<Map<String, String>> map = objectMapper.readValue(stored, new TypeReference<>() {});

            List<ChatMessage> messages = map.stream()
                    .map(this::toMessage)
                    .filter(Objects::nonNull)
                    .toList();

            return messages;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMessages(Object memoryID, List<ChatMessage> list) {
        String redisKey = memoryKey((String) memoryID);

        List<Map<String, String>> map = list.stream()
                .map(this::fromMessage)
                .toList();
        try {
            String json = objectMapper.writeValueAsString(map);
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofMinutes(30));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteMessages(Object o) {
        String redisKey = memoryKey((String) o);

        redisTemplate.delete(redisKey);
    }

    private ChatMessage toMessage(Map<String, String> map) {
        String type = map.get("type");
        String text = map.get("text");

        return switch (type) {
            case "USER" -> UserMessage.from(text);
            case "AI" -> AiMessage.from(text);
            case "SYSTEM" -> SystemMessage.from(text);
            default -> null;
        };
    }

    private Map<String, String> fromMessage(ChatMessage message) {
        String type = message.type().name();
        String text = message.text();

        return Map.of(
                "type" , type,
                "text" , text
        );
    }

    private String memoryKey(String memoryID) {

        try{
            java.security.MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(memoryID.getBytes(StandardCharsets.UTF_8));
            return "memory:"+HexFormat.of().formatHex(hash).substring(0, 16);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
