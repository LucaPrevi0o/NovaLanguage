package semantic;

import parser.parser.util.Diagnostic;

import java.util.List;

/// Thrown when semantic analysis finds one or more diagnostics.
public class SemanticAnalysisException extends RuntimeException {

    private final List<Diagnostic> diagnostics;

    public SemanticAnalysisException(List<Diagnostic> diagnostics) {
        super(buildMessage(diagnostics));
        this.diagnostics = List.copyOf(diagnostics);
    }

    public List<Diagnostic> getDiagnostics() { return diagnostics; }

    private static String buildMessage(List<Diagnostic> diagnostics) {
        var sb = new StringBuilder("Semantic analysis failed with ").append(diagnostics.size()).append(" issue(s):\n");
        for (var diagnostic : diagnostics) sb.append("  • ").append(diagnostic.toPrettyString()).append("\n");
        return sb.toString().stripTrailing();
    }
}
