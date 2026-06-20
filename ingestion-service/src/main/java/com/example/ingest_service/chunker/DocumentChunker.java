package com.example.ingest_service.chunker;

import com.example.ingest_service.dtos.CodeChunk;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class DocumentChunker {
    private static final int CHUNK_WORD_SIZE = 400;
    private static final int OVERLAP_SIZE = 30;

    public List<CodeChunk> splitter(String content, String filename, String namespace) {
        String[] words = content.split("\\s+");

        List<CodeChunk> chunks = new ArrayList<>();

        int lineCounter = 1;
        int i = 0;

        while(i < words.length) {
            int end = Math.min(words.length, i+CHUNK_WORD_SIZE);
            String chunkText = String.join(" ", Arrays.copyOfRange(words, i, end));

            int startLine = lineCounter;
            int totalLines = (int) chunkText.chars().filter(c -> c ==  '\n' ).count();
            int endLine = startLine+totalLines;

            String chunkId = uuid(chunkText+filename+namespace);

            chunks.add(
                    CodeChunk.builder()
                            .chunkId(chunkId)
                            .namespace(namespace)
                            .filePath(filename)
                            .startLine(startLine)
                            .endLine(endLine)
                            .chunkType(filename.substring(filename.lastIndexOf('.')))
                            .content(content)
                            .build()
            );

            lineCounter = endLine;
            i = i + CHUNK_WORD_SIZE - OVERLAP_SIZE;
        }

        return chunks;
    }

    private String uuid(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
