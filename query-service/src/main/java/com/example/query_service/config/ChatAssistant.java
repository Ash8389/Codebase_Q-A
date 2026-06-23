package com.example.query_service.config;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public interface ChatAssistant {
    String answer(@MemoryId String sessionID,@UserMessage String question);
}
