package error.diagnostic;

/// Describes how serious a compiler diagnostic is.
public enum DiagnosticSeverity {

    /// The diagnostic is informational and does not indicate a problem.
    INFO,

    /// The diagnostic indicates a potential problem that may not prevent compilation but should be addressed.
    WARNING,

    /// The diagnostic indicates a serious problem that prevents compilation and must be fixed.
    ERROR
}
