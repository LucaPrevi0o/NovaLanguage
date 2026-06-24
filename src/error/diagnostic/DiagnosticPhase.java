package error.diagnostic;

/// Identifies the compiler phase that produced a diagnostic.
public enum DiagnosticPhase {

    /// The lexical analysis phase of the compiler.
    LEXER,

    /// The parsing phase of the compiler.
    PARSER,

    /// The semantic analysis phase of the compiler.
    SEMANTIC,

    /// The lowering phase of the compiler, in which high-level constructs are transformed into lower-level constructs.
    LOWERING,

    /// The code generation phase of the compiler, in which intermediate representations are transformed into target code.
    COMPILER
}
