import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import error.diagnostic.DiagnosticSeverity;
import error.syntax.UnrecognizedTokenError;
import lexer.Token;
import lexer.token.family.Special;
import org.junit.jupiter.api.Test;
import parser.parser.util.ParseException;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for structured compiler diagnostics.
public class DiagnosticTest {

    @Test
    void diagnosticFromTokenStoresLocationAndTokenContext() {

        var token = new Token(Special.UNKNOWN, 2, 5, "@");
        var diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, "Bad token", token);

        assertEquals(DiagnosticSeverity.ERROR, diagnostic.getSeverity());
        assertEquals(DiagnosticPhase.PARSER, diagnostic.getPhase());
        assertEquals("Bad token", diagnostic.getMessage());
        assertEquals(2, diagnostic.getLine());
        assertEquals(5, diagnostic.getColumn());
        assertEquals(1, diagnostic.getSpanLength());
        assertEquals(Special.UNKNOWN, diagnostic.getActualToken());
        assertEquals("@", diagnostic.getLexeme());
        assertTrue(diagnostic.hasLocation());
        assertTrue(diagnostic.hasSpan());
    }

    @Test
    void diagnosticBagCollectsDiagnosticsAndExposesImmutableView() {

        var bag = new DiagnosticBag();
        bag.reportError(DiagnosticPhase.PARSER, "Expected expression");

        assertEquals(1, bag.size());
        assertTrue(bag.hasDiagnostics());
        assertTrue(bag.hasErrors());
        assertThrows(UnsupportedOperationException.class,
                () -> bag.getDiagnostics().add(Diagnostic.error(DiagnosticPhase.PARSER, "another")));
    }

    @Test
    void diagnosticBagCanWrapLegacyErrorWithTokenContext() {

        var token = new Token(Special.UNKNOWN, 4, 7, "$");
        var error = new UnrecognizedTokenError(token);
        var bag = new DiagnosticBag();

        var diagnostic = bag.report(error, DiagnosticPhase.PARSER, token);

        assertEquals(1, bag.size());
        assertTrue(diagnostic.getMessage().contains("Unrecognized token"));
        assertEquals(4, diagnostic.getLine());
        assertEquals(7, diagnostic.getColumn());
        assertEquals("$", diagnostic.getLexeme());
    }

    @Test
    void parseExceptionExposesStructuredDiagnostic() {

        var token = new Token(Special.UNKNOWN, 3, 9, "@");
        var exception = new ParseException("Unrecognized token", token);
        var diagnostic = exception.getDiagnostic();

        assertEquals(DiagnosticPhase.PARSER, diagnostic.getPhase());
        assertEquals(DiagnosticSeverity.ERROR, diagnostic.getSeverity());
        assertEquals("Unrecognized token", diagnostic.getMessage());
        assertEquals(Special.UNKNOWN, diagnostic.getActualToken());
        assertEquals("@", diagnostic.getLexeme());
    }

    @Test
    void parseExceptionAcceptsPrebuiltDiagnostic() {

        var token = new Token(Special.UNKNOWN, 6, 2, "@");
        var diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, "Bad token", token);
        var exception = new ParseException(diagnostic, token);

        assertSame(diagnostic, exception.getDiagnostic());
        assertSame(token, exception.getToken());
        assertTrue(exception.getMessage().contains("line 6"));
        assertTrue(exception.getMessage().contains("Bad token"));
    }
}
