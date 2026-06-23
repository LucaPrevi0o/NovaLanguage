package error.diagnostic;

import lexer.Token;

import java.util.ArrayList;
import java.util.List;

/// Mutable collection of diagnostics for one compiler run.
public final class DiagnosticBag {

    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public void report(Diagnostic diagnostic) {

        if (diagnostic != null) diagnostics.add(diagnostic);
    }

    public Diagnostic reportError(DiagnosticPhase phase, String message) {

        var diagnostic = Diagnostic.error(phase, message);
        report(diagnostic);
        return diagnostic;
    }

    public Diagnostic reportError(DiagnosticPhase phase, String message, Token token) {

        var diagnostic = Diagnostic.error(phase, message, token);
        report(diagnostic);
        return diagnostic;
    }

    public Diagnostic report(error.Error error, DiagnosticPhase phase) {

        var diagnostic = Diagnostic.fromError(error, phase);
        report(diagnostic);
        return diagnostic;
    }

    public Diagnostic report(error.Error error, DiagnosticPhase phase, Token token) {

        var diagnostic = Diagnostic.fromError(error, phase, token);
        report(diagnostic);
        return diagnostic;
    }

    public List<Diagnostic> getDiagnostics() { return List.copyOf(diagnostics); }

    public boolean hasDiagnostics() { return !diagnostics.isEmpty(); }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.getSeverity() == DiagnosticSeverity.ERROR);
    }

    public int size() { return diagnostics.size(); }

    public void clear() { diagnostics.clear(); }
}
