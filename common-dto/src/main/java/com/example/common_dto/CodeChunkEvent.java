package com.example.common_dto;

public record CodeChunkEvent(
        String chunkId,
        String namespace,
        String filePath,
        int startLine,
        int endLine,
        String chunkType,
        String content
) {
    public String prefixedContent() {
        return "File: " + filePath +
                " | Type: " + chunkType +
                " | Lines: " + startLine + "-" + endLine +
                "\n\n" + content;
    }
}
