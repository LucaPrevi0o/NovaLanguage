package error.diagnostic;

import lexer.Token;

import java.util.ArrayList;
import java.util.List;

/// Mutable collection of diagnostics for one compiler run.
public final class DiagnosticBag {

    private final List<Diagnostic> diagnostics = new ArrayList<>();

    /// Adds a diagnostic to the collection.
    /// @param diagnostic The diagnostic to add.
    public void report(Diagnostic diagnostic) {
        if (diagnostic != null) diagnostics.add(diagnostic);
    }

    /// Adds a diagnostic error to the collection with the given compiler phase and diagnostics message.
    /// @param phase The phase of the compiler in which the diagnostic was generated.
    /// @param message The diagnostic message.
    public void reportError(DiagnosticPhase phase, String message) {

        var diagnostic = Diagnostic.error(phase, message);
        report(diagnostic);
    }

    /// Adds a diagnostic error to the collection with the given compiler phase, diagnostics message, and source token.
    /// @param phase The phase of the compiler in which the diagnostic was generated.
    /// @param message The diagnostic message.
    /// @param token The source token associated with the diagnostic.
    /// @return The diagnostic that was added to the collection.
    public Diagnostic reportError(DiagnosticPhase phase, String message, Token token) {

        var diagnostic = Diagnostic.error(phase, message, token);
        report(diagnostic);
        return diagnostic;
    }

    /// Returns the list of diagnostics in the collection.
    /// > NOTE: The returned list should not be modified directly, although it is not strictly immutable.
    /// > Use the `report` method to add diagnostics instead.
    /// @return The list of diagnostics in the collection.
    public List<Diagnostic> getDiagnostics() { return diagnostics; }

    /// Checks if the collection has any diagnostics.
    /// @return `true` if the collection has any diagnostics, `false` otherwise.
    public boolean hasDiagnostics() { return !diagnostics.isEmpty(); }

    /// Checks if the collection has any error diagnostics.
    /// @return `true` if the collection has any diagnostics with severity ERROR, `false` otherwise.
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == DiagnosticSeverity.ERROR);
    }

    /// Returns the number of diagnostics in the collection.
    /// @return The number of diagnostics in the collection.
    public int size() { return diagnostics.size(); }

    /// Clears all diagnostics from the collection.
    public void clear() { diagnostics.clear(); }
}
