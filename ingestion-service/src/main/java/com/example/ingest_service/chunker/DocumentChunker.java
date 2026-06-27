package com.example.ingest_service.chunker;

import com.example.ingest_service.dtos.CodeChunk;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DocumentChunker {
    private static final int CHUNK_WORD_SIZE = 400;
    private static final int OVERLAP_SIZE = 25;

    public List<CodeChunk> splitter(String content, String filename, String namespace) {
        String[] lines= content.split("\\R");

        List<CodeChunk> chunks = new ArrayList<>();

        int startLine = 1;
        int wordCount = 0;
        StringBuilder chunkText= new StringBuilder();

        for(int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int wordsInLine = line.isBlank() ? 0 : line.trim().split("\\s+").length;

            if(wordCount + wordsInLine > CHUNK_WORD_SIZE && !chunkText.isEmpty()) {
                int endLine = i;
                String chunkId = uuid(chunkText+filename+namespace);

                chunks.add(
                        CodeChunk.builder()
                                .chunkId(chunkId)
                                .namespace(namespace)
                                .filePath(filename)
                                .startLine(startLine)
                                .endLine(endLine)
                                .chunkType(filename.substring(filename.lastIndexOf('.')))
                                .content(chunkText.toString())
                                .build()
                );


                chunkText.setLength(0);
                startLine = i+1;
                wordCount = 0;
            }

            wordCount += wordsInLine;
            chunkText.append(line).append("\n");
        }

        if(!chunkText.isEmpty()) {
            int endLine = lines.length;
            String chunkId = uuid(chunkText+filename+namespace);
            chunks.add(
                    CodeChunk.builder()
                            .chunkId(chunkId)
                            .namespace(namespace)
                            .filePath(filename)
                            .startLine(startLine)
                            .endLine(endLine)
                            .chunkType(filename.substring(filename.lastIndexOf('.')))
                            .content(chunkText.toString())
                            .build()
            );

        }

        return chunks;
    }

    private String uuid(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }

}
