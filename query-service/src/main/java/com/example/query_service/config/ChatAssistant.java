package com.example.query_service.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ChatAssistant {
    @SystemMessage("""
        You are a code assistant. You will be given code snippets as context.
    """)
    Response<AiMessage> answer(@MemoryId String sessionID, @UserMessage String question);
}
