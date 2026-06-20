package com.example.ingest_service.services;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


@Service
public class PullRepoService {

    public Path cloneRepo(String uri) throws GitAPIException, IOException {
        Path repoPath = Files.createTempDirectory("coderag-");

        CloneCommand clone = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(repoPath.toFile())
                .setDepth(1);

        try(Git git = clone.call()){

            System.out.println("Repo Cloned!!");
        };

        return repoPath;
    }

    public List<Path> supportedFiles(Path relevantPath) throws IOException {
        List<Path> supportedFiles = Files.walk(relevantPath)
                .filter(Files::isRegularFile)
                .filter(p -> isSupportedExtension(p))
                .filter(p -> !isIgnoredPath(p))
                .toList();

        return supportedFiles;
    }

    private boolean isSupportedExtension(Path p){
        String name = p.toString();

        return name.endsWith(".java") || name.endsWith(".md");
    }

    private boolean isIgnoredPath(Path p){
        String name = p.toString();
        return name.contains("/.git/")  || name.contains("/target/")
                || name.contains("/node_modules/") || name.contains("/.idea/");
    }
}
