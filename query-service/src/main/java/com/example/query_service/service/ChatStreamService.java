package com.example.query_service.service;

import com.example.query_service.config.StreamingChatAssistant;
import com.example.query_service.dtos.ScoredChunk;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.el.parser.Token;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatStreamService {

    private final StreamingChatAssistant streamingChatAssistant;
    private final CodeChunkRetriever chunkRetriever;

    private final int MAX_TOKEN = 2500;

    public TokenStream chatStream(String sessionID, String question, String namespace) throws ExecutionException, InterruptedException {

        List<ScoredChunk> chunks = chunkRetriever.retriever(question, namespace);

        TokenCountEstimator tokenizer = new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4);
        StringBuilder context = new StringBuilder();
        int currentTokenCount = 0;
        for(int i = 0; i< chunks.size(); i++) {
            ScoredChunk c = chunks.get(i);

//            log.debug("Score: {}---Context: {}", c.score(), c.content());

            StringBuilder currentChunk = new StringBuilder();
            currentChunk.append("[CHUNK ").append(i + 1).append("]\n")
                    .append("File: ").append(c.filePath())
                    .append(" | Lines: ").append(c.startLine())
                    .append("-").append(c.endLine()).append("\n")
                    .append(c.content())
                    .append("\n\n");

            int chunkToken = tokenizer.estimateTokenCountInText(String.valueOf(currentChunk));
            if(currentTokenCount + chunkToken > MAX_TOKEN) {
                break;
            }

            currentTokenCount += chunkToken;

            context.append(currentChunk);
        }


        String prompt = """ 
            You are a code assistant.
            Use chat history when the user refers to previous messages.
            Use retrieved code chunks only for repository questions.
            
            - Use proper formatting.
            - Explain code in plain English.
            - Use code blocks for code reference.
            After your answer, on a new line write:
            CITATIONS: [list the CHUNK numbers you used, e.g. 1,3]
            
            CODE CHUNKS:
            %s

            QUESTION: %s
            """.formatted(context.toString(), question);

        TokenStream stream = streamingChatAssistant.answer(sessionID, prompt);
        return stream;
    }
}
