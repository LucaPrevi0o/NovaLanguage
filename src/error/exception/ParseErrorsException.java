package error.exception;

import error.Error;

import java.util.List;

/// Thrown by {@link parser.Parser} when one or more parse errors are collected during a parse run.
///
/// <p>Rather than aborting at the first error, the parser attempts to recover and continue so
/// that as many problems as possible are reported in a single run.  All collected errors are
/// carried inside this exception and rendered together in the message.</p>
public class ParseErrorsException extends RuntimeException {

    private final List<Error> errors;

    /// Constructs a new ParseErrorsException from the given list of parse errors.
    /// @param errors One or more {@link Error}s collected during parsing.
    public ParseErrorsException(List<Error> errors) {

        super(buildMessage(errors));
        this.errors = List.copyOf(errors);
    }

    /// Returns the individual errors that were collected during parsing.
    /// @return An unmodifiable list of {@link Error}s.
    public List<Error> getErrors() { return errors; }

    private static String buildMessage(List<Error> errors) {

        var sb = new StringBuilder();
        sb.append("\n\n === PARSING ERROR ===\n\nParse failed with ").append(errors.size()).append(" error").append(errors.size() == 1 ? "" : "s").append(".\n");
        //sb.append(errors.size()).append(" parse error").append(errors.size() == 1 ? "" : "s").append(":\n");
        for (var e : errors) sb.append("  • ").append(e.getMessage()).append("\n");
        return sb.toString().stripTrailing();
    }
}
