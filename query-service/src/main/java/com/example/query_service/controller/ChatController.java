package com.example.query_service.controller;

import com.example.query_service.dtos.QueryResponse;
import com.example.query_service.dtos.RequestQuery;
import com.example.query_service.service.ChatService;
import com.example.query_service.service.ChatStreamService;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@RestController
@RequestMapping("chat/")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatService  chatService;
    private final ChatStreamService chatStreamService;

    @PostMapping("query")
    public ResponseEntity<QueryResponse> chat(@RequestBody RequestQuery requestQuery,
                                              @RequestHeader("X-Session-Id") String sessionID) throws ExecutionException, InterruptedException {
        return  ResponseEntity.ok(chatService.chat(sessionID ,requestQuery.question(), requestQuery.namespace()));
    }

    @GetMapping(value="stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody RequestQuery requestQuery,
                                 @RequestHeader("X-Session-Id") String sessionID) throws ExecutionException, InterruptedException {

        SseEmitter emitter = new SseEmitter();
        TokenStream stream = chatStreamService.chatStream(sessionID, requestQuery.question(), requestQuery.namespace());
        StringBuilder answer = new StringBuilder();

        stream.onPartialResponse(token -> {
            answer.append(token);

            try {
                emitter.send(
                        SseEmitter.event()
                                .name("token")
                                .data(token)
                );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        stream.onCompleteResponse(response -> {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("done")
                                .data("DONE")
                );

                log.info("Final answer: {}", answer);

                emitter.complete();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        stream.onError(emitter::completeWithError);

        stream.start();

        return emitter;
    }
}
