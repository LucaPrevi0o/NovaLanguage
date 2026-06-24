package error.diagnostic;

import java.util.List;

/// Thrown by {@link parser.Parser} when one or more parse errors are collected during a parse run.
///
/// <p>Rather than aborting at the first error, the parser attempts to recover and continue so
/// that as many problems as possible are reported in a single run.  All collected errors are
/// carried inside this exception and rendered together in the message.</p>
public class ParseErrorsException extends RuntimeException {

    private final List<ParseException> errors;

    /// Constructs a new ParseErrorsException from the given list of parse errors.
    /// @param errors One or more {@link ParseException}s collected during parsing.
    public ParseErrorsException(List<ParseException> errors) {

        super(buildMessage(errors));
        this.errors = List.copyOf(errors);
    }

    /// Returns the individual errors that were collected during parsing.
    /// @return An unmodifiable list of {@link ParseException}s.
    public List<ParseException> getErrors() { return errors; }

    /// Returns the structured diagnostics represented by the collected parse errors.
    /// @return An unmodifiable list of diagnostics.
    public List<Diagnostic> getDiagnostics() { return errors.stream().map(ParseException::getDiagnostic).toList(); }

    /// Builds a message string from the given list of parse errors.
    /// @param errors The list of parse errors to build the message from.
    /// @return A string message summarizing the parse errors.
    private static String buildMessage(List<ParseException> errors) {

        var sb = new StringBuilder();
        sb.append(errors.size()).append(" parse error").append(errors.size() == 1 ? "" : "s").append(":\n");
        for (var e : errors) sb.append("  • ").append(e.getMessage()).append("\n");
        return sb.toString().stripTrailing();
    }
}
