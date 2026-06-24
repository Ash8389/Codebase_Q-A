package com.example.embedding_service.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QdrantCollectionInitializer implements ApplicationRunner {
    private final QdrantClient qdrantClient;
    private static final String COLLECTION_NAME = "code_chunks";
    private static final int VECTOR_SIZE = 1024;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean exists = qdrantClient
                .listCollectionsAsync()
                .get()
                .stream()
                .anyMatch(c -> c.equals(COLLECTION_NAME));

        if(!exists) {
            qdrantClient.createCollectionAsync(
                    COLLECTION_NAME,
                    Collections.VectorParams.newBuilder()
                            .setSize(VECTOR_SIZE)
                            .setDistance(Collections.Distance.Cosine)
                            .build()
            ).get();

            qdrantClient.createPayloadIndexAsync(
                    COLLECTION_NAME, "namespace",
                    Collections.PayloadSchemaType.Keyword, null, true, null, null
            ).get();

            qdrantClient.createPayloadIndexAsync(
                    COLLECTION_NAME, "chunkType",
                    Collections.PayloadSchemaType.Keyword, null, true, null, null
            ).get();
        }
    }
}
