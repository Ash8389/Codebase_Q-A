package com.example.query_service.dtos;

public record Citation(
        String path,
        int startLine,
        int endLine,
        double score
) { }
