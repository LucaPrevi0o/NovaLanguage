package error.diagnostic;

/// Identifies the compiler phase that produced a diagnostic.
public enum DiagnosticPhase {

    LEXER,
    PARSER,
    SEMANTIC,
    LOWERING,
    COMPILER
}
