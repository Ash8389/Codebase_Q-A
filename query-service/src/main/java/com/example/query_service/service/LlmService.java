package com.example.query_service.service;

import com.example.query_service.config.ChatAssistant;
import com.example.query_service.dtos.Citation;
import com.example.query_service.dtos.LlmServiceResponse;
import com.example.query_service.dtos.ScoredChunk;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LlmService {
    private final ChatAssistant chatAssistant;

    private final int MAX_TOKEN = 2500;

    public LlmServiceResponse llmCall(String sessionID, String question, List<ScoredChunk> chunks) {

         TokenCountEstimator tokenizer = new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4);
        StringBuilder context = new StringBuilder();
        int currentTokenCount = 0;

        log.info( "CHUNK SIZE: {}",chunks.size());

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
            log.info("CURRENT CHUNK: {}", currentChunk);

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
                
                            Instructions:
                            - Use proper Markdown formatting.
                            - Explain code in plain English.
                            - When referring to repository code, only use the provided code chunks.
                            - At the very end of your response, output exactly one line in this format:
                
                            CITATIONS: <comma-separated chunk numbers>
                
                            Rules:
                            - This must be the final line of the response.
                            - Do not include brackets, periods, or extra text.
                            - Example: CITATIONS: 2,5,8
                            - If no code chunks were used, output: CITATIONS:
                
                            CODE CHUNKS:
                            %s
                
                            QUESTION:
                            %s
            """.formatted(context.toString(), question);

//        log.debug("#################################\nPROMPT: {}\n#################################", prompt);

        String result = chatAssistant.answer(sessionID ,prompt);

        return parseCitation(result, chunks);
    }

    private LlmServiceResponse parseCitation(String result, List<ScoredChunk> chunks) {
        int citationIndex = result.lastIndexOf("CITATIONS:");

//        log.debug("------------RESULT: -----------------{}----------------------------------", result);

        if(citationIndex == -1) {
            return new LlmServiceResponse(result, List.of());
        }
        String answer = result.substring(0, citationIndex);
        List<Integer> usedChunkIndex = new ArrayList<>();

        if(citationIndex != -1) {
            String indexLine = result.substring(citationIndex+10).trim();

            Arrays.stream(indexLine.split(","))
                    .map(String::trim)
                    .filter(s -> s.matches("\\d+"))
                    .map(Integer::parseInt)
                    .filter(n -> n >= 1 && n <= chunks.size())
                    .forEach(n -> usedChunkIndex.add(n - 1));
        }

        List<Citation> citations = usedChunkIndex.stream()
                .map(i -> new Citation(
                        chunks.get(i).filePath(),
                        chunks.get(i).startLine(),
                        chunks.get(i).endLine(),
                        chunks.get(i).score()
                        )
                ).toList();

        return new LlmServiceResponse(answer, citations);
    }
}
