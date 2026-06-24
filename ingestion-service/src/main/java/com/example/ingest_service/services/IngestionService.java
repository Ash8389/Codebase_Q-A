package com.example.ingest_service.services;

import com.example.common_dto.CodeChunkEvent;
import com.example.ingest_service.chunker.JavaAstChunker;
import com.example.ingest_service.dtos.CodeChunk;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class IngestionService {

    private final PullRepoService pullRepoService;
    private final JavaAstChunker javaAstChunker;
    private final KafkaServices kafkaServices;

    public IngestionService(PullRepoService pullRepoService,
                            JavaAstChunker javaAstChunker,
                            KafkaServices kafkaServices) {
        this.pullRepoService = pullRepoService;
        this.javaAstChunker = javaAstChunker;
        this.kafkaServices = kafkaServices;
    }

    public String ingest(String uri) throws GitAPIException, IOException {
        if(!uri.contains(".git")) {
            uri = uri + ".git";
        }

        Path path = pullRepoService.cloneRepo(uri);
        List<Path> supportedFiles = pullRepoService.supportedFiles(path);

        for(Path filePath : supportedFiles) {
            String contents = Files.readString(filePath, StandardCharsets.UTF_8);
            String relativePath = path.relativize(filePath).toString();

            String nameSpace = uri.substring(uri.lastIndexOf('/') + 1, uri.lastIndexOf('.'));

            List<CodeChunk> chunks = javaAstChunker.chunk(contents, relativePath, nameSpace);

            List<CodeChunkEvent> chunkEvents = chunks.stream().map(
                    c -> new CodeChunkEvent(
                            c.chunkId(),
                            c.namespace(),
                            c.filePath(),
                            c.startLine(),
                            c.endLine(),
                            c.chunkType(),
                            c.content()
                    )
            ).toList();


            log.info("{}---{}", filePath.getFileName(), chunks.size());
            chunkEvents.forEach(kafkaServices::producer);

        }

        FileSystemUtils.deleteRecursively(path.toFile());

        return "Ingestion Done";
    }
}



//            for (CodeChunk chunk : chunks) {
//
//                System.out.println("=================================");
//                System.out.println("File: " + chunk.filePath());
//                System.out.println("Type: " + chunk.chunkType());
//                System.out.println("Lines: " +
//                        chunk.startLine() + "-" +
//                        chunk.endLine());
//
////                System.out.println(
////                        chunk.content()
////                );
//
//                System.out.println("=================================");
//            }
