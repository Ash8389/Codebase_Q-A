package com.example.ingest_service.dtos;

public record ServiceResponse(
        String msg,
        boolean ingested
) {
}
