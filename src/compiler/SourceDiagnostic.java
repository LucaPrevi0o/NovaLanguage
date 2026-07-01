package compiler;

import error.diagnostic.Diagnostic;

import java.util.Objects;

/// File-aware diagnostic produced at the project pipeline boundary.
///
/// Existing compiler phases can continue reporting plain diagnostics. The
/// project pipeline wraps those diagnostics with source-file identity when it
/// aggregates results across multiple files.
public record SourceDiagnostic(SourceFile sourceFile, Diagnostic diagnostic) {

    /// Creates a file-aware diagnostic.
    /// @param sourceFile The source file associated with the diagnostic, or `null` for project-level diagnostics.
    /// @param diagnostic The underlying compiler diagnostic.
    public SourceDiagnostic {
        Objects.requireNonNull(diagnostic, "Diagnostic must not be null");
    }

    /// Creates a diagnostic associated with a concrete source file.
    /// @param sourceFile The source file.
    /// @param diagnostic The diagnostic.
    /// @return A file-aware diagnostic.
    public static SourceDiagnostic from(SourceFile sourceFile, Diagnostic diagnostic) {
        return new SourceDiagnostic(Objects.requireNonNull(sourceFile, "Source file must not be null"), diagnostic);
    }

    /// Creates a project-level diagnostic that is not tied to one file.
    /// @param diagnostic The diagnostic.
    /// @return A project-level diagnostic wrapper.
    public static SourceDiagnostic project(Diagnostic diagnostic) {
        return new SourceDiagnostic(null, diagnostic);
    }

    /// Returns whether this diagnostic is associated with one source file.
    /// @return `true` when a source file is present.
    public boolean hasSourceFile() { return sourceFile != null; }
}
