package de.maxhenkel.audioplayer.webserver;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class StaticFileCache {

    private final Map<String, byte[]> cache;

    public StaticFileCache(Map<String, byte[]> cache) {
        this.cache = cache;
    }

    @Nullable
    public byte[] get(String path) {
        return cache.get(path);
    }

    public static StaticFileCache of(String resourceFolder) throws IOException, URISyntaxException {
        Map<String, byte[]> cache = new HashMap<>();

        URL url = StaticFileCache.class.getClassLoader().getResource(resourceFolder);
        if (url == null) {
            throw new IOException("Resource not found: %s".formatted(resourceFolder));
        }
        Path root = Paths.get(url.toURI());

        List<Path> resources = getRecursive(root);

        for (Path path : resources) {
            cache.put(pathToString(root.relativize(path)), Files.readAllBytes(path));
        }

        return new StaticFileCache(cache);
    }

    private static String pathToString(Path path) {
        StringBuilder sb = new StringBuilder();
        for (Path p : path) {
            sb.append("/");
            sb.append(p);
        }
        if (sb.isEmpty()) {
            sb.append("/");
        }
        return sb.toString();
    }

    private static List<Path> getRecursive(Path path) throws IOException {
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        if (!Files.isDirectory(path)) {
            return List.of(path);
        }
        try (Stream<Path> stream = Files.list(path)) {
            List<Path> contents = stream.toList();
            List<Path> paths = new ArrayList<>();
            for (Path file : contents) {
                paths.addAll(getRecursive(file));
            }
            return paths;
        }
    }

}
