package com.example.query_service.dtos;

import java.util.List;

public record LlmServiceResponse(
        String answer,
        List<Citation> citations
) {
}
