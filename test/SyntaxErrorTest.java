import error.syntax.UnrecognizedTokenError;
import error.syntax.UnexpectedTokenError;
import lexer.Token;
import lexer.token.family.Delimiter;
import lexer.token.family.Keyword;
import lexer.token.family.Special;
import lexer.token.family.literal.IdentifierLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests for syntax error message formatting.
public class SyntaxErrorTest {

    @Test
    void unexpectedTokenWithLocationIncludesActualToken() {

        var error = new UnexpectedTokenError(Keyword.IF, 3, 7);
        assertEquals("[line 3, col 7] Unexpected token: 'if'", error.getMessage());
    }

    @Test
    void unexpectedTokenWithExpectedTokenIncludesBothTokens() {

        var actual = new Token(Keyword.IF, 2, 4);
        var error = new UnexpectedTokenError(Delimiter.RPAREN, actual);
        assertEquals("[line 2, col 4] Unexpected token: expected ')' but found 'if'", error.getMessage());
    }

    @Test
    void unexpectedPlaceholderLiteralUsesClassNameFallback() {

        var error = new UnexpectedTokenError(new IdentifierLiteral());
        assertEquals("Unexpected token: 'IdentifierLiteral'", error.getMessage());
    }

    @Test
    void unrecognizedTokenWithLocationIncludesRawValue() {

        var error = new UnrecognizedTokenError("@", 4, 12);
        assertEquals("[line 4, col 12] Unrecognized token: '@'", error.getMessage());
    }

    @Test
    void unrecognizedTokenWithoutLocationOmitsLocationPrefix() {

        var error = new UnrecognizedTokenError("@");
        assertEquals("Unrecognized token: '@'", error.getMessage());
    }

    @Test
    void unrecognizedTokenCanBeBuiltFromLexerToken() {

        var token = new Token(Special.UNKNOWN, 5, 9, "@");
        var error = new UnrecognizedTokenError(token);
        assertEquals("[line 5, col 9] Unrecognized token: '@'", error.getMessage());
    }
}
