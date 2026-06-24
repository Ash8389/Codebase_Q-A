package com.example.ingest_service.chunker;

import com.example.ingest_service.dtos.CodeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class JavaAstChunker implements CodeChunker{

    private static Set<String> CHUNK_NODE_TYPES = Set.of(
            "method_declaration",
            "constructor_declaration"
    );

    private static Set<String> FALLBACK_NODE_TYPES = Set.of(
            "class_declaration",
            "interface_declaration",
            "enum_declaration"
    );

    private final DocumentChunker documentChunker;

    @Override
    public List<CodeChunk> chunk(String content, String filePath, String nameSpace) {

        byte[] sourceBytes = content.getBytes(StandardCharsets.UTF_8);

        TSParser parser = new TSParser();
        parser.setLanguage(new TreeSitterJava());

        TSTree tree = parser.parseString(null, content);
        TSNode rootNode = tree.getRootNode();

        List<CodeChunk> chunks = new ArrayList<>();
        collectChunks(rootNode, sourceBytes, filePath, nameSpace, chunks, CHUNK_NODE_TYPES, "METHOD");

        collectChunks(rootNode, sourceBytes, filePath, nameSpace, chunks, FALLBACK_NODE_TYPES, "CLASS");

        if(chunks.isEmpty()) {
            System.out.println(filePath + " : " + content);
            return fallbackToSlidingWindow(content, filePath, nameSpace);
        }

        return chunks;
    }

    private List<CodeChunk> fallbackToSlidingWindow(String content, String filePath, String nameSpace) {

        return documentChunker.splitter(content, filePath, nameSpace);

    }

    private void collectChunks(TSNode node, byte[] sourceBytes,
                               String filePath, String namespace,
                               List<CodeChunk> result,
                               Set<String> targetTypes, String chunkType) {

        if(targetTypes.contains(node.getType())) {

            int start = node.getStartByte();
            int end = node.getEndByte();

            if (start < 0 || end > sourceBytes.length || start >= end) {
                log.info(
                        "Invalid node: {} start={} end={} sourceLength={} file={}",
                        node.getType(),
                        start,
                        end,
                        sourceBytes.length,
                        filePath
                );
            }

            String chunkContent = new String(
                    sourceBytes,
                    node.getStartByte(),
                    node.getEndByte()- node.getStartByte(),
                    StandardCharsets.UTF_8
            );

            int startLine = node.getStartPoint().getRow() + 1;
            int endLine = node.getEndPoint().getRow() + 1;

            if(endLine - startLine < 2) return;

            String chunkId = uuid(namespace + filePath + startLine);

            result.add(
                    CodeChunk.builder()
                            .chunkId(chunkId)
                            .namespace(namespace)
                            .filePath(filePath)
                            .startLine(startLine)
                            .endLine(endLine)
                            .chunkType(chunkType)
                            .content(chunkContent)
                            .build()
            );

            return;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectChunks(node.getChild(i), sourceBytes, filePath,
                    namespace, result, targetTypes, chunkType);
        }
    }

    private String uuid(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
