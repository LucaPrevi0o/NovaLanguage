package src.lexer;

import src.lexer.token.*;
import src.token.TokenFamily;
import src.token.family.AccessModifier;
import src.token.family.Delimiter;
import src.token.family.Keyword;
import src.token.family.Literal;
import src.token.family.Operator;
import src.token.family.PrimitiveType;
import src.token.family.Special;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lexical analyzer that converts source code into a stream of tokens.
 * Handles whitespace, comments, literals, keywords, operators, and delimiters.
 */
public class Lexer {

    private final String source;
    private int position;
    private int line;
    private int column;
    private char currentChar;
    
    private static final Map<String, TokenFamily> TYPES = new HashMap<>();
    private static final Map<String, TokenFamily> KEYWORDS = new HashMap<>();
    private static final Map<String, TokenFamily> OPERATORS = new HashMap<>();
    private static final Map<String, TokenFamily> DELIMITERS = new HashMap<>();

    static {

        // Automatically populate types from TokenType enum
        for (var value : Delimiter.values()) DELIMITERS.put(value.get(), value);
        for (var value : Keyword.values()) KEYWORDS.put(value.get(), value);
        for (var value : AccessModifier.values()) KEYWORDS.put(value.get(), value);  // Add access modifiers
        for (var value : PrimitiveType.values()) TYPES.put(value.get(), value);
        for (var value: Operator.values()) OPERATORS.put(value.get(), value);
    }
    
    public Lexer(String source) {

        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.currentChar = source.length() > 0 ? source.charAt(0) : '\0';
    }
    
    // ========== Character Navigation ==========
    
    /**
     * Advance to the next character in the source.
     */
    private void advance() {

        if (currentChar == '\n') {

            line++;
            column = 1;
        } else column++;
        
        position++;
        if (position < source.length()) currentChar = source.charAt(position);
        else currentChar = '\0';
    }
    
    /**
     * Look at the next character without consuming it.
     */
    private char peek() { return peek(1); }
    
    /**
     * Look ahead n characters without consuming them.
     */
    private char peek(int offset) {

        int peekPos = position + offset;
        if (peekPos < source.length()) return source.charAt(peekPos);
        return '\0';
    }
    
    // ========== Whitespace and Comments ==========
    
    /**
     * Skip all whitespace characters.
     */
    private void skipWhitespace() {

        while (currentChar != '\0' && Character.isWhitespace(currentChar))
            advance();
    }
    
    /**
     * Skip single-line and multi-line comments.
     */
    private void skipComment() {

        if (currentChar == '/' && peek() == '/') skipSingleLineComment();
        else if (currentChar == '/' && peek() == '*') skipMultiLineComment();
    }
    
    private void skipSingleLineComment() {

        // Skip until end of line or end of file
        while (currentChar != '\0' && currentChar != '\n')
            advance();
    }
    
    private void skipMultiLineComment() {

        advance(); // skip '/'
        advance(); // skip '*'
        
        while (currentChar != '\0') {

            if (currentChar == '*' && peek() == '/') {

                advance(); // skip '*'
                advance(); // skip '/'
                break;
            }
            advance();
        }
    }
    
    // ========== Token Reading ==========
    
    /**
     * Read a number literal (integer or float).
     */
    private Token readNumber() {

        int startLine = line;
        int startColumn = column;
        StringBuilder number = new StringBuilder();
        
        while (currentChar != '\0' && (Character.isDigit(currentChar) || currentChar == '.')) {

            number.append(currentChar);
            advance();
        }

        return new LiteralToken(new Literal.NumberLiteral(number.toString()), startLine, startColumn);
    }
    
    /**
     * Read an identifier or keyword.
     */
    private Token readIdentifierOrKeyword() {

        int startLine = line;
        int startColumn = column;
        StringBuilder identifier = new StringBuilder();
        
        while (currentChar != '\0' && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {

            identifier.append(currentChar);
            advance();
        }
        
        String value = identifier.toString();
        TokenFamily isKeyword = KEYWORDS.get(value);
        TokenFamily isType = TYPES.get(value);

        if (isKeyword != null) return new KeywordToken(isKeyword, startLine, startColumn);
        if (isType != null) return new TypeToken(isType, startLine, startColumn);
        return new LiteralToken(new Literal.IdentifierLiteral(value), startLine, startColumn);
    }
    
    /**
     * Read a string literal enclosed in double quotes.
     */
    private Token readString() {

        int startLine = line;
        int startColumn = column;
        StringBuilder string = new StringBuilder();
        
        advance(); // skip opening quote
        
        while (currentChar != '\0' && currentChar != '"') {

            if (currentChar == '\\' && peek() == '"') {

                advance(); // skip backslash
                string.append(currentChar);
                advance();
            } else {

                string.append(currentChar);
                advance();
            }
        }
        
        if (currentChar == '"')  advance(); // skip closing quote
        return new LiteralToken(new Literal.StringLiteral(string.toString()), startLine, startColumn);
    }
    
    /**
     * Read an operator token (single or double character).
     */
    private Token readOperator() {
        
        int tokenLine = line;
        int tokenColumn = column;
        
        // Try two-character operators first
        String twoChar = "" + currentChar + peek();
        TokenFamily type = OPERATORS.get(twoChar);
        
        if (type != null) {

            advance();
            advance();
            return new OperatorToken(type, tokenLine, tokenColumn);
        }
        
        // Try single-character operators
        String oneChar = "" + currentChar;
        type = OPERATORS.get(oneChar);
        
        if (type != null) {

            advance();
            return new OperatorToken(type, tokenLine, tokenColumn);
        }
        
        return null;
    }    /**
     * Read a delimiter token.
     */
    private Token readDelimiter() {

        int tokenLine = line;
        int tokenColumn = column;
        
        // Try two-character delimiters first (::)
        String twoChar = "" + currentChar + peek();
        TokenFamily type = DELIMITERS.get(twoChar);

        if (type != null) {
            advance();
            advance();
            return new DelimiterToken(type, tokenLine, tokenColumn);
        }
        
        // Try single-character delimiters
        String oneChar = "" + currentChar;
        type = DELIMITERS.get(oneChar);
        
        if (type != null) {
            advance();
            return new DelimiterToken(type, tokenLine, tokenColumn);
        }
        
        return null;
    }
    
    // ========== Public API ==========
    
    /**
     * Get the next token from the source code.
     */
    public Token getNextToken() {

        while (currentChar != '\0') {

            // Skip whitespace
            if (Character.isWhitespace(currentChar)) {

                skipWhitespace();
                continue;
            }
            
            // Skip comments
            if (currentChar == '/' && (peek() == '/' || peek() == '*')) {

                skipComment();
                continue;
            }
            
            // Numbers
            if (Character.isDigit(currentChar)) return readNumber();
            
            // Identifiers and keywords
            if (Character.isLetter(currentChar) || currentChar == '_') return readIdentifierOrKeyword();
            
            // Strings
            if (currentChar == '"') return readString();

            // Operators
            Token operator = readOperator();
            if (operator != null) return operator;

            // Delimiters
            Token delimiter = readDelimiter();
            if (delimiter != null) return delimiter;
            
            // Unknown character
            int tokenLine = line;
            int tokenColumn = column;
            advance();
            return new Token(Special.UNKNOWN, tokenLine, tokenColumn);
        }
        
        // End of file
        return new Token(Special.EOF, line, column);
    }
    
    /**
     * Tokenize the entire source code and return a list of tokens.
     */
    public List<Token> tokenize() {

        List<Token> tokens = new ArrayList<>();
        Token token;
        
        do {

            token = getNextToken();
            tokens.add(token);
        } while (token.getType() != Special.EOF);
        
        return tokens;
    }
}
