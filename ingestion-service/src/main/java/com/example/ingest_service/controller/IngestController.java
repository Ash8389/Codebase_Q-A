package com.example.ingest_service.controller;

import com.example.ingest_service.services.IngestionService;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("ingest/")
public class IngestController {
    IngestionService ingestionService;
    public IngestController(IngestionService ingestionService){
        this.ingestionService = ingestionService;
    }

    @GetMapping
    public void get(@RequestParam("q") String uri) throws GitAPIException, IOException {
        ingestionService.ingest(uri);
    }
}
