package com.example.ingest_service.controller;

import com.example.ingest_service.services.IngestionService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("ingest/")
public class IngestController {
    IngestionService ingestionService;
    public IngestController(IngestionService ingestionService){
        this.ingestionService = ingestionService;
    }

    @PostMapping("uri")
    public void ingest(@RequestParam("q") String uri) throws GitAPIException, IOException {
        ingestionService.ingest(uri);
    }
}
