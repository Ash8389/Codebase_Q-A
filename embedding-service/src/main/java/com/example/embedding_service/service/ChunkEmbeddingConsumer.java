package com.example.embedding_service.service;

import com.example.common_dto.CodeChunkEvent;
import com.example.embedding_service.config.QdrantCollectionInitializer;
import com.google.common.collect.Lists;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Service
public class ChunkEmbeddingConsumer {

    private final EmbeddingModel embeddingModel;
    private final QdrantClient qdrantClient;

    private static final String COLLECTION = "code_chunks";
    private static final int BATCH_SIZE = 20;

    @KafkaListener(
            topics = "code-chunks",
            groupId = "embedding-service",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consume(List<CodeChunkEvent> chunkEvents) {

        System.out.println("consumed-chunks size : " + chunkEvents.size());

        Lists.partition(chunkEvents, BATCH_SIZE)
            .forEach(this::qDrantEmbedAndStore);
    }


    private void qDrantEmbedAndStore(List<CodeChunkEvent> chunkEventBatch) {

        deleteNamespaceExist(chunkEventBatch.getFirst().namespace());

        List<String> text = chunkEventBatch.stream()
                .map(CodeChunkEvent::prefixedContent)
                .toList();

        List<Embedding> embeddings = embeddingModel.embedAll(
                text.stream()
                        .map(TextSegment::from)
                        .toList()
        ).content();

        List<Points.PointStruct> points = new ArrayList<>();
        for(int i = 0; i< chunkEventBatch.size(); i++){
            CodeChunkEvent chunk = chunkEventBatch.get(i);
            List<Float> vec = embeddings.get(i).vectorAsList();

            Map<String, JsonWithInt.Value> payload = Map.of(
                    "namespace",  value(chunk.namespace()),
                    "filePath",   value(chunk.filePath()),
                    "startLine",  value(chunk.startLine()),
                    "endLine",    value(chunk.endLine()),
                    "chunkType",  value(chunk.chunkType()),
                    "content",    value(chunk.content())
            );

            points.add(
                    Points.PointStruct.newBuilder().setId(
                                    Points.PointId.newBuilder()
                                            .setUuid(chunk.chunkId())
                                            .build())
                            .setVectors(Points.Vectors.newBuilder()
                                    .setVector(Points.Vector.newBuilder()
                                            .addAllData(vec)
                                            .build()
                                    ).build()
                            ).putAllPayload(payload)
                            .build()
            );
        }

        try{
            qdrantClient.upsertAsync(COLLECTION, points);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonWithInt.Value value(String s){
        return JsonWithInt.Value.newBuilder().setStringValue(s).build();
    }
    private JsonWithInt.Value value(int n){
        return JsonWithInt.Value.newBuilder().setIntegerValue(n).build();
    }

    public void deleteNamespaceExist(String namespace){

        try{
            qdrantClient.deleteAsync(COLLECTION,
                    Points.Filter.newBuilder()
                            .addMust(
                                    Points.Condition.newBuilder()
                                            .setField(
                                                    Points.FieldCondition.newBuilder()
                                                            .setKey("namespace")
                                                            .setMatch(
                                                                    Points.Match.newBuilder()
                                                                            .setKeyword(namespace)
                                                                            .build()
                                                            )
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            ).get();
        } catch (RuntimeException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
