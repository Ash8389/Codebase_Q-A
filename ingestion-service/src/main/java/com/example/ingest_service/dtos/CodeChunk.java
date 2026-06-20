package com.example.ingest_service.dtos;


import lombok.Builder;

import java.util.UUID;

@Builder
public record CodeChunk(
        String chunkId,
        String namespace,
        String filePath,
        int startLine,
        int endLine,
        String chunkType,
        String content
) { }
