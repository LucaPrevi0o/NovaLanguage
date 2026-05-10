package error;

/// Represents a general error in the system, providing a method to retrieve an error message describing the issue.
///
/// This error can happen both in the lexing/parsing phase (e.g., syntax errors) and in the semantic analysis phase
/// (e.g., type errors, undefined variable errors).
/// It can be used to raise an appropriate exception in the parser or to report errors during semantic analysis,
/// allowing for a unified error handling approach across different stages of processing the source code.
public interface Error {

    /// Returns a human-readable message describing the error.
    /// @return A string message that provides details about the error, which can be used for debugging and user feedback purposes.
    String getMessage();
}
