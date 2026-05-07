package parser.parser.util;

/// Structured diagnostic payload used by parser and later pipeline stages.
public record Diagnostic(DiagnosticCode code, String message, SourceSpan span, String near) {

    public String toPrettyString() {

        var sb = new StringBuilder();
        sb.append(code).append(" at line ").append(span.line())
          .append(", column ").append(span.startColumn())
          .append(": ").append(message);
        if (near != null) sb.append(" (near '").append(near).append("')");
        sb.append("\n  ").append(span.toCaretLine());
        return sb.toString();
    }
}
