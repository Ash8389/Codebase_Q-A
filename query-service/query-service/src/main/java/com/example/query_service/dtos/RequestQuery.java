package com.example.query_service.dtos;

public record RequestQuery(
        String question,
        String namespace
) {
}
