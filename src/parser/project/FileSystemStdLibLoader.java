package parser.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/// Loads stdlib Nova modules from a configurable filesystem root.
public class FileSystemStdLibLoader implements StdLibLoader {

    private final Path stdlibRoot;

    public FileSystemStdLibLoader(Path stdlibRoot) {
        this.stdlibRoot = stdlibRoot;
    }

    @Override
    public List<Path> loadStdLibFiles() throws IOException {

        if (stdlibRoot == null || !Files.exists(stdlibRoot)) return List.of();
        try (var stream = Files.walk(stdlibRoot)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".nv"))
                .sorted(Comparator.naturalOrder())
                .toList();
        }
    }
}
