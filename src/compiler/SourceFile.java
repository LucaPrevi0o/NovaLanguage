package compiler;

import java.nio.file.Path;
import java.util.Objects;

/// Immutable identity and text for one Nova source file.
///
/// A source file owns only input identity, text, and origin metadata. Tokens,
/// AST nodes, diagnostics, declarations, and scopes belong to later pipeline
/// objects.
public record SourceFile(String identity, String source, SourceOrigin origin) {

    /// Creates a source file from explicit identity, text, and origin metadata.
    /// @param identity Stable source identity used in diagnostics and project maps.
    /// @param source The full source text.
    /// @param origin The source origin category.
    public SourceFile {

        if (identity == null || identity.isBlank())
            throw new IllegalArgumentException("Source file identity must not be blank");
        source = Objects.requireNonNullElse(source, "");
        origin = origin != null ? origin : SourceOrigin.MEMORY;
    }

    /// Creates an in-memory source file, typically for tests and tooling.
    /// @param identity Stable display identity.
    /// @param source The full source text.
    /// @return A memory-backed source file.
    public static SourceFile inMemory(String identity, String source) {
        return new SourceFile(identity, source, SourceOrigin.MEMORY);
    }

    /// Creates a disk-backed source file from a path and text.
    /// @param path The source path.
    /// @param source The full source text.
    /// @return A disk-backed source file.
    public static SourceFile fromPath(Path path, String source) {
        Objects.requireNonNull(path, "Source path must not be null");
        return new SourceFile(path.normalize().toString(), source, SourceOrigin.DISK);
    }
}
