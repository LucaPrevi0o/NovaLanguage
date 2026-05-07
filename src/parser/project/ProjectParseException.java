package parser.project;

import parser.parser.util.ParseException;

import java.util.List;

/// Thrown when one or more files in a project parse fail.
public class ProjectParseException extends RuntimeException {

    private final List<ParseException> errors;

    public ProjectParseException(List<ParseException> errors) {

        super(buildMessage(errors));
        this.errors = List.copyOf(errors);
    }

    public List<ParseException> getErrors() { return errors; }

    private static String buildMessage(List<ParseException> errors) {

        var sb = new StringBuilder("Project parse failed with ").append(errors.size()).append(" error(s):\n");
        for (var error : errors) sb.append("  • ").append(error.getDiagnostic().toPrettyString()).append("\n");
        return sb.toString().stripTrailing();
    }
}
