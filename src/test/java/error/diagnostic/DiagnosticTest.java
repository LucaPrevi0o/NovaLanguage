package error.diagnostic;

import lexer.Token;
import lexer.token.family.Special;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for structured compiler diagnostics.
public class DiagnosticTest {

    @Test
    void diagnosticFromTokenStoresLocationAndTokenContext() {

        var token = new Token(Special.UNKNOWN, 2, 5, "@");
        var diagnostic = Diagnostic.error(DiagnosticPhase.PARSER, "Bad token", token);

        assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
        assertEquals(DiagnosticPhase.PARSER, diagnostic.phase());
        assertEquals("Bad token", diagnostic.message());
        assertEquals(2, diagnostic.line());
        assertEquals(5, diagnostic.column());
        assertEquals(1, diagnostic.spanLength());
        assertEquals(Special.UNKNOWN, diagnostic.actualToken());
        assertEquals("@", diagnostic.lexeme());
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
    void diagnosticBagReportsTokenDiagnosticsDirectly() {

        var token = new Token(Special.UNKNOWN, 4, 7, "$");
        var bag = new DiagnosticBag();

        var diagnostic = bag.reportError(DiagnosticPhase.PARSER, "Unrecognized token: '$'", token);

        assertEquals(1, bag.size());
        assertTrue(diagnostic.message().contains("Unrecognized token"));
        assertEquals(4, diagnostic.line());
        assertEquals(7, diagnostic.column());
        assertEquals("$", diagnostic.lexeme());
    }

    @Test
    void parseExceptionExposesStructuredDiagnostic() {

        var token = new Token(Special.UNKNOWN, 3, 9, "@");
        var exception = new ParseException("Unrecognized token", token);
        var diagnostic = exception.getDiagnostic();

        assertEquals(DiagnosticPhase.PARSER, diagnostic.phase());
        assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
        assertEquals("Unrecognized token", diagnostic.message());
        assertEquals(Special.UNKNOWN, diagnostic.actualToken());
        assertEquals("@", diagnostic.lexeme());
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
