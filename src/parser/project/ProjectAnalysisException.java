package parser.project;

import parser.parser.util.Diagnostic;

import java.util.List;

/// Thrown when semantic analysis fails on a parsed project.
public class ProjectAnalysisException extends RuntimeException {

    private final List<Diagnostic> diagnostics;

    public ProjectAnalysisException(List<Diagnostic> diagnostics) {
        super(buildMessage(diagnostics));
        this.diagnostics = List.copyOf(diagnostics);
    }

    public List<Diagnostic> getDiagnostics() { return diagnostics; }

    private static String buildMessage(List<Diagnostic> diagnostics) {
        var sb = new StringBuilder("Project semantic analysis failed with ").append(diagnostics.size()).append(" issue(s):\n");
        for (var diagnostic : diagnostics) sb.append("  • ").append(diagnostic.toPrettyString()).append('\n');
        return sb.toString().stripTrailing();
    }
}
