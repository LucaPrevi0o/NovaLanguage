package src.test.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import src.lexer.Lexer;
import src.lexer.Token;
import src.token.family.Literal;
import src.token.family.Keyword;
import src.token.family.Operator;
import src.token.family.Special;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Test suite for the Lexer class
public class LexerTest {

    @Test
    void testIntegerLiteral() {
        var lexer = new Lexer("42");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // number + EOF
        assertTrue(tokens.get(0).getType() instanceof Literal.NumberLiteral);
    }

    @Test
    void testStringLiteral() {
        var lexer = new Lexer("\"hello\"");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // string + EOF
        assertTrue(tokens.get(0).getType() instanceof Literal.StringLiteral);
    }

    @Test
    void testKeyword() {
        var lexer = new Lexer("if");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // keyword + EOF
        assertEquals(Keyword.IF, tokens.get(0).getType());
    }

    @Test
    void testAllKeywords() {
        for (var keyword : Keyword.values()) {
            var lexer = new Lexer(keyword.get());
            var tokens = lexer.tokenize();
            assertEquals(2, tokens.size());
            assertEquals(keyword, tokens.get(0).getType());
        }
    }

    @Test
    void testOperator() {
        var lexer = new Lexer("+");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // operator + EOF
        assertEquals(Operator.PLUS, tokens.get(0).getType());
    }

    @Test
    void testAllOperators() {
        for (var operator : Operator.values()) {
            var lexer = new Lexer(operator.get());
            var tokens = lexer.tokenize();
            assertEquals(2, tokens.size());
            assertEquals(operator, tokens.get(0).getType());
        }
    }

    @Test
    void testSingleLineComment() {
        var lexer = new Lexer("// this is a comment\n42");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // only number + EOF, comment is skipped
        assertTrue(tokens.get(0).getType() instanceof Literal.NumberLiteral);
    }

    @Test
    void testMultiLineComment() {
        var lexer = new Lexer("/* comment */42");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size()); // only number + EOF, comment is skipped
        assertTrue(tokens.get(0).getType() instanceof Literal.NumberLiteral);
    }

    @Test
    void testUnknownCharacter() {
        var lexer = new Lexer("@");
        var tokens = lexer.tokenize();
        assertEquals(2, tokens.size());
        assertEquals(Special.UNKNOWN, tokens.get(0).getType());
    }
}


