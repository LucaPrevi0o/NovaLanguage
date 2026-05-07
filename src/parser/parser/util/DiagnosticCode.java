package parser.parser.util;

/// Stable diagnostic codes that classify parser/semantic/backend issues.
public enum DiagnosticCode {
    SYNTAX_ERROR,
    UNDEFINED_SYMBOL,
    DUPLICATE_SYMBOL,
    TYPE_ERROR,
    SEMANTIC_ERROR,
    BACKEND_ERROR
}
