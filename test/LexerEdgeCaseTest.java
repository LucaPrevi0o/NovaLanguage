import lexer.token.family.literal.CharLiteral;
import lexer.token.family.literal.IdentifierLiteral;
import lexer.token.family.literal.NumberLiteral;
import lexer.token.family.literal.StringLiteral;
import org.junit.jupiter.api.Test;
import lexer.Lexer;
import lexer.token.type.LiteralToken;
import lexer.token.family.Special;

import static org.junit.jupiter.api.Assertions.*;

/// Edge-case tests for the Lexer that exercise less common paths:
/// multiple decimal points, empty/unterminated char literals,
/// type suffixes on number literals, escape sequences, etc.
public class LexerEdgeCaseTest {

    // ─── Number literals ──────────────────────────────────────────────────────

    @Test
    void testIntegerLiteral() {

        var tokens = new Lexer("123").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
        assertEquals("123", ((LiteralToken) tokens.getFirst()).getValue());
    }

    @Test
    void testFloatLiteral() {

        var tokens = new Lexer("3.14").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
        assertEquals("3.14", ((LiteralToken) tokens.getFirst()).getValue());
    }

    @Test
    void testLongLiteralSuffix() {

        var tokens = new Lexer("100L").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
        assertEquals("100L", ((LiteralToken) tokens.getFirst()).getValue());
    }

    @Test
    void testFloatLiteralSuffix() {

        var tokens = new Lexer("1.5f").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
        assertEquals("1.5f", ((LiteralToken) tokens.getFirst()).getValue());
    }

    @Test
    void testDoubleLiteralSuffix() {

        var tokens = new Lexer("2.7d").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
        assertEquals("2.7d", ((LiteralToken) tokens.getFirst()).getValue());
    }

    @Test
    void testByteLiteralSuffix() {

        var tokens = new Lexer("10b").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
        assertEquals("10b", ((LiteralToken) tokens.getFirst()).getValue());
    }

    /// A second decimal point must stop the number token so "1.2.3" produces
    /// three tokens: the number "1.2", the operator ".", and "3", not a single
    /// malformed token.  This prevents a cryptic NumberFormatException deep in
    /// the parser.
    @Test
    void testSecondDecimalPointStopsNumberToken() {

        var tokens = new Lexer("1.2.3").tokenize();
        // token[0] = NumberLiteral "1.2"
        // token[1] = some token starting with "."  (either UNKNOWN or operator)
        // token[2] = NumberLiteral "3"
        // token[3] = EOF
        assertTrue(tokens.size() >= 3, "1.2.3 should produce at least 3 tokens");
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
        assertEquals("1.2", ((LiteralToken) tokens.getFirst()).getValue(), "First token should be 1.2");
        assertInstanceOf(NumberLiteral.class, tokens.get(tokens.size() - 2).getType());
    }

    @Test
    void testIntegerZero() {

        var tokens = new Lexer("0").tokenize();
        assertEquals(2, tokens.size());
        assertEquals("0", ((LiteralToken) tokens.getFirst()).getValue());
    }

    // ─── Char literals ────────────────────────────────────────────────────────

    @Test
    void testNormalCharLiteral() {

        var tokens = new Lexer("'a'").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(CharLiteral.class, tokens.getFirst().getType());
    }

    @Test
    void testEscapeCharLiteral() {

        var tokens = new Lexer("'\\n'").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(CharLiteral.class, tokens.getFirst().getType());
    }

    /// Empty char literal '' should produce an UNKNOWN token, not throw or silently produce \0.
    @Test
    void testEmptyCharLiteralProducesUnknown() {

        var tokens = new Lexer("''").tokenize();
        assertTrue(tokens.size() >= 2);
        assertEquals(Special.UNKNOWN, tokens.getFirst().getType(),
                "Empty char literal should produce an UNKNOWN token");
    }

    /// Unterminated char literal at end-of-file should produce an UNKNOWN token.
    @Test
    void testUnterminatedCharLiteralProducesUnknown() {

        var tokens = new Lexer("'a").tokenize();
        assertTrue(tokens.size() >= 2);
        assertEquals(Special.UNKNOWN, tokens.getFirst().getType(),
                "Unterminated char literal should produce an UNKNOWN token");
    }

    /// EOF immediately after opening quote should also produce UNKNOWN.
    @Test
    void testBareOpenQuoteProducesUnknown() {

        var tokens = new Lexer("'").tokenize();
        assertTrue(tokens.size() >= 2);
        assertEquals(Special.UNKNOWN, tokens.getFirst().getType(),
                "Bare opening quote should produce an UNKNOWN token");
    }

    // ─── String literals ──────────────────────────────────────────────────────

    @Test
    void testStringLiteralEscapeNewline() {

        var tokens = new Lexer("\"hello\\nworld\"").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(StringLiteral.class, tokens.getFirst().getType());
        assertTrue(((LiteralToken) tokens.getFirst()).getValue().contains("\n"));
    }

    @Test
    void testStringLiteralEscapeTab() {

        var tokens = new Lexer("\"col1\\tcol2\"").tokenize();
        assertEquals(2, tokens.size());
        assertTrue(((LiteralToken) tokens.getFirst()).getValue().contains("\t"));
    }

    @Test
    void testStringLiteralEscapeQuote() {

        var tokens = new Lexer("\"say \\\"hello\\\"\"").tokenize();
        assertEquals(2, tokens.size());
        assertTrue(((LiteralToken) tokens.getFirst()).getValue().contains("\""));
    }

    @Test
    void testEmptyStringLiteral() {

        var tokens = new Lexer("\"\"").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(StringLiteral.class, tokens.getFirst().getType());
        assertEquals("", ((LiteralToken) tokens.getFirst()).getValue());
    }

    // ─── Identifiers ──────────────────────────────────────────────────────────

    @Test
    void testIdentifier() {

        var tokens = new Lexer("myVariable").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(IdentifierLiteral.class, tokens.getFirst().getType());
        assertEquals("myVariable", ((LiteralToken) tokens.getFirst()).getValue());
    }

    @Test
    void testIdentifierWithUnderscore() {

        var tokens = new Lexer("_my_var").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(IdentifierLiteral.class, tokens.getFirst().getType());
    }

    @Test
    void testIdentifierStartingWithUnderscore() {

        var tokens = new Lexer("_privateField").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(IdentifierLiteral.class, tokens.getFirst().getType());
    }

    // ─── Comments ─────────────────────────────────────────────────────────────

    @Test
    void testSingleLineCommentSkipped() {

        var tokens = new Lexer("// comment\n42").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
    }

    @Test
    void testMultilineCommentSkipped() {

        var tokens = new Lexer("/* block\ncomment */42").tokenize();
        assertEquals(2, tokens.size());
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
    }

    @Test
    void testNestedCommentNotSupported() {

        // Outer /* ... */ ends at the first */ — the inner /* does NOT open a nested comment
        var tokens = new Lexer("/* outer /* inner */ 42 */").tokenize();
        // After the first */, "42" and "*/" are in the token stream
        assertTrue(tokens.stream().anyMatch(t -> t.getType() instanceof NumberLiteral),
                "42 should be present after the first comment closes");
    }

    // ─── Whitespace ───────────────────────────────────────────────────────────

    @Test
    void testWhitespaceSkipped() {

        var tokens = new Lexer("   42   ").tokenize();
        assertEquals(2, tokens.size());
    }

    @Test
    void testNewlineSkipped() {

        var tokens = new Lexer("42\n+\n1").tokenize();
        assertEquals(4, tokens.size()); // 42, +, 1, EOF
    }

    // ─── Unknown characters ───────────────────────────────────────────────────

    @Test
    void testUnknownCharacterProducesUnknownToken() {

        var tokens = new Lexer("@").tokenize();
        assertEquals(2, tokens.size());
        assertEquals(Special.UNKNOWN, tokens.getFirst().getType());
    }
}
