package com.example.query_service.service;

import com.example.query_service.dtos.LlmServiceResponse;
import com.example.query_service.dtos.QueryResponse;
import com.example.query_service.dtos.ScoredChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final RedisTemplate<String, LlmServiceResponse> redisTemplate;
    private final LlmService llmService;
    private final CodeChunkRetriever chunkRetriever;

    private final static long REDIS_CACHE_TTL = 30;

    public QueryResponse chat(String sessionID, String question, String namespace) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();

        String cacheKey = buildCacheKey(question, namespace);

        LlmServiceResponse cached = redisTemplate.opsForValue().get(cacheKey);

        if(cached != null) {

            return new QueryResponse(
                    cached.answer(),
                    cached.citations(),
                    true,
                    System.currentTimeMillis()-startTime
            );
        }

        List<ScoredChunk> chunks = chunkRetriever.retriever(question, namespace);

        log.debug("Chunks is size: {}", chunks.size());

        LlmServiceResponse llmServiceResponse = llmService.llmCall(sessionID, question, chunks);

        redisTemplate.opsForValue().set(cacheKey, llmServiceResponse, Duration.ofMinutes(REDIS_CACHE_TTL));

        return new QueryResponse(
                llmServiceResponse.answer(),
                llmServiceResponse.citations(),
                false,
                System.currentTimeMillis() - startTime
        );
    }


    private String buildCacheKey(String question, String namespace){
        String raw = namespace + ":" + question.toLowerCase().trim();

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            return "query:" + HexFormat.of().formatHex(hash).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }
}
