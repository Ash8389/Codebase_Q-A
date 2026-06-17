package com.example.ingest_service;

import com.example.ingest_service.chunker.JavaAstChunker;
import com.example.ingest_service.services.IngestionService;
import com.example.ingest_service.services.PullRepoService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class IngestServiceApplication {

	public static void main(String[] args) throws GitAPIException, IOException {
		SpringApplication.run(IngestServiceApplication.class, args);
	}

}
