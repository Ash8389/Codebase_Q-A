package com.example.query_service.dtos;

import java.util.List;

public record QueryResponse(
        String answer,
        List<Citation> citations,
        boolean fromCache,
        long latencyMs
) {
}
