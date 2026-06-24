package com.example.query_service.config;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface StreamingChatAssistant {
    TokenStream answer(@MemoryId String sessionID, @UserMessage String question);
}
