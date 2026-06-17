package com.example.query_service.controller;

import com.example.query_service.dtos.QueryResponse;
import com.example.query_service.dtos.RequestQuery;
import com.example.query_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@RestController
@RequestMapping("chat/")
public class ChatController {

    private final ChatService  chatService;

    @PostMapping
    public ResponseEntity<QueryResponse> chat(@RequestBody RequestQuery requestQuery,
                                              @RequestHeader("X-Session-Id") String sessionID) throws ExecutionException, InterruptedException {
        return  ResponseEntity.ok(chatService.chat(sessionID ,requestQuery.question(), requestQuery.namespace()));
    }
}
