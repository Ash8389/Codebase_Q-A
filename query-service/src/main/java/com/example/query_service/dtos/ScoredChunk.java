package com.example.query_service.dtos;

public record ScoredChunk(
        String filePath,
        int    startLine,
        int    endLine,
        String chunkType,
        String content,
        double score
) {}
