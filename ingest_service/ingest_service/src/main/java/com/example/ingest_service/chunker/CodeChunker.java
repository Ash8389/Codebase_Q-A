package com.example.ingest_service.chunker;

import com.example.ingest_service.dtos.CodeChunk;

import java.util.List;

public interface CodeChunker {
    List<CodeChunk> chunk(String content, String filePath, String nameSpace);
}
