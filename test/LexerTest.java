import lexer.token.family.literal.NumberLiteral;
import lexer.token.family.literal.StringLiteral;
import org.junit.jupiter.api.Test;
import lexer.Lexer;
import lexer.token.family.Keyword;
import lexer.token.family.Operator;
import lexer.token.family.Special;

import static org.junit.jupiter.api.Assertions.*;

/// Test suite for the Lexer class
public class LexerTest {

    @Test
    void testIntegerLiteral() {

        var lexer = new Lexer("42");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // number + EOF
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
    }

    @Test
    void testStringLiteral() {

        var lexer = new Lexer("\"hello\"");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // string + EOF
        assertInstanceOf(StringLiteral.class, tokens.getFirst().getType());
    }

    @Test
    void testKeyword() {

        var lexer = new Lexer("if");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // keyword + EOF
        assertEquals(Keyword.IF, tokens.getFirst().getType());
    }

    @Test
    void testAllKeywords() {

        for (var keyword : Keyword.values()) {

            var lexer = new Lexer(keyword.get());
            var tokens = lexer.tokenize();
            assertEquals(2, tokens.size());
            assertEquals(keyword, tokens.getFirst().getType());
        }
    }

    @Test
    void testOperator() {

        var lexer = new Lexer("+");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // operator + EOF
        assertEquals(Operator.PLUS, tokens.getFirst().getType());
    }

    @Test
    void testAllOperators() {

        for (var operator : Operator.values()) {

            var lexer = new Lexer(operator.get());
            var tokens = lexer.tokenize();
            assertEquals(2, tokens.size());
            assertEquals(operator, tokens.getFirst().getType());
        }
    }

    @Test
    void testSingleLineComment() {

        var lexer = new Lexer("// this is a comment\n42");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // only number + EOF, comment is skipped
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
    }

    @Test
    void testMultiLineComment() {

        var lexer = new Lexer("/* comment */42");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // only number + EOF, comment is skipped
        assertInstanceOf(NumberLiteral.class, tokens.getFirst().getType());
    }

    @Test
    void testUnknownCharacter() {

        var lexer = new Lexer("@");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size());
        assertEquals(Special.UNKNOWN, tokens.getFirst().getType());
    }
}


