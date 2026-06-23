package com.example.query_service.service;

import com.example.query_service.dtos.ScoredChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Component
public class CodeChunkRetriever {

    private final EmbeddingModel embeddingModel;
    private final QdrantClient qdrantClient;

    private static final int TOP_K = 5;
    public List<ScoredChunk> retriever(String question,String namespace) throws ExecutionException, InterruptedException {

        Embedding embedding = embeddingModel.embed(question).content();
        float[] vec = embedding.vector();

        Points.Filter filter = Points.Filter.newBuilder()
                .addMust(Points.Condition.newBuilder()
                                .setField(Points.FieldCondition.newBuilder()
                                        .setKey("namespace")
                                        .setMatch(Points.Match.newBuilder()
                                                .setKeyword(namespace)
                                                .build()
                                        ).build()

                                ).build()
                ).build();

        List<Points.ScoredPoint> points = qdrantClient.searchAsync(
                        Points.SearchPoints.newBuilder()
                                .setCollectionName("code_chunks")
                                .setFilter(filter)
                                .setScoreThreshold((float)0.5)
                                .setLimit(TOP_K)
                                .addAllVector(toFloatList(vec))
                                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                                .build()
                ).get();

        return points.stream()
                .map(this::toChodChunk)
                .toList();
    }

    private ScoredChunk toChodChunk(Points.ScoredPoint point) {

        Map<String, JsonWithInt.Value> p = point.getPayloadMap();

        return new ScoredChunk(
                p.get("filePath").getStringValue(),
                (int) p.get("startLine").getIntegerValue(),
                (int) p.get("endLine").getIntegerValue(),
                p.get("chunkType").getStringValue(),
                p.get("content").getStringValue(),
                point.getScore()
        );
    }

    private Iterable<Float> toFloatList(float[] vec) {
        List<Float> lst = new ArrayList<>();

        for (float v: vec) {
            lst.add(v);
        }

        return lst;
    }

}
