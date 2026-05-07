package parser.parser.util;

/// Represents a source position range (1-based line/column start and end).
public record SourceSpan(int line, int startColumn, int endColumn) {

    /// Returns a compact caret-style indicator for the span.
    public String toCaretLine() {

        var start = Math.max(1, startColumn);
        var end = Math.max(start, endColumn);
        var sb = new StringBuilder();
        sb.append(" ".repeat(start - 1));
        sb.append("^");
        if (end > start) sb.append("~".repeat(end - start));
        return sb.toString();
    }
}
