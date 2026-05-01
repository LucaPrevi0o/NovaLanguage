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

 /// Lexical analyzer that converts source code into a stream of tokens.
 /// Handles whitespace, comments, literals, keywords, operators, and delimiters.
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
        for (var value : Operator.values()) OPERATORS.put(value.get(), value);
    }

    /// Constructs a Lexer with the given source code.
    /// Initializes the position, line, column, and current character.
    /// @param source The source code to be tokenized.
    public Lexer(String source) {

        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.currentChar = !source.isEmpty() ? source.charAt(0) : '\0';
    }

    /// Advance the current position by one character, updating line and column numbers.
    private void advance() {

        if (currentChar == '\n') {

            line++;
            column = 1;
        } else column++;
        
        position++;
        if (position < source.length()) currentChar = source.charAt(position);
        else currentChar = '\0';
    }

    /// Peek at the next character without advancing the position and return it.
    /// @return The next character in the source code, or {@code '\0'} if at the end of the source.
    private char peek() {

        var peekPos = position + 1;
        if (peekPos < source.length()) return source.charAt(peekPos);
        return '\0';
    }

    /// Skip over any whitespace characters.
    private void skipWhitespace() { while (currentChar != '\0' && Character.isWhitespace(currentChar)) advance(); }

    /// Skip over comments (single-line and multi-line).
    ///
    /// Single-line comments start with '//' and continue to the end of the line.
    /// Multi-line comments start with '/*' and end with '*/', and can span multiple lines.
    private void skipComment() {

        if (currentChar == '/' && peek() == '/') skipSingleLineComment();
        else if (currentChar == '/' && peek() == '*') skipMultiLineComment();
    }

    /// Skip a single-line comment by advancing until the end of the line or end of file.
    private void skipSingleLineComment() { while (currentChar != '\0' && currentChar != '\n') advance(); }

    /// Skip a multi-line comment by advancing until the closing '*/' is found.
    /// Handles nested comments and updates line and column numbers appropriately.
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

    /// Read a number literal, which can be an integer or a floating-point number.
    /// Handles sequences of digits and at most one decimal point.
    /// @return A LiteralToken representing the number literal.
    private Token readNumber() {

        var startLine = line;
        var startColumn = column;
        var number = new StringBuilder();
        
        while (currentChar != '\0' && (Character.isDigit(currentChar) || currentChar == '.')) {

            number.append(currentChar);
            advance();
        }

        return new LiteralToken(new Literal.NumberLiteral(number.toString()), startLine, startColumn);
    }

    /// Read an identifier or keyword, which starts with a letter or underscore and can contain letters, digits, and underscores.
    /// After reading the sequence, check if it matches a keyword or type; if not, treat it as an identifier.
    /// @return A KeywordToken, TypeToken, or LiteralToken representing the identifier or keyword.
    private Token readIdentifierOrKeyword() {

        var startLine = line;
        var startColumn = column;
        var identifier = new StringBuilder();
        
        while (currentChar != '\0' && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {

            identifier.append(currentChar);
            advance();
        }
        
        var value = identifier.toString();
        var isKeyword = KEYWORDS.get(value);
        var isType = TYPES.get(value);

        if (isKeyword != null) return new KeywordToken(isKeyword, startLine, startColumn);
        if (isType != null) return new TypeToken(isType, startLine, startColumn);
        return new LiteralToken(new Literal.IdentifierLiteral(value), startLine, startColumn);
    }

    /// Read a string literal, which starts and ends with double quotes.
    ///
    /// Handles escape sequences for double quotes (e.g., {@code \\"} becomes a double quote in the string).
    /// @return A LiteralToken representing the string literal.
    private Token readString() {

        var startLine = line;
        var startColumn = column;
        var string = new StringBuilder();
        advance(); // skip opening quote
        while (currentChar != '\0' && currentChar != '"') if (currentChar == '\\' && peek() == '"') {

            advance(); // skip backslash
            string.append(currentChar);
            advance();
        } else {

            string.append(currentChar);
            advance();
        }
        
        if (currentChar == '"')  advance(); // skip closing quote
        return new LiteralToken(new Literal.StringLiteral(string.toString()), startLine, startColumn);
    }

    /// Read an operator token, which can be one or two characters long.
    ///
    /// Checks for two-character operators first (e.g., {@code ==}, {@code !=}, {@code <=}, {@code >=}), then checks
    /// for single-character operators (e.g., {@code +}, {@code -}, {@code *}, {@code /}).
    /// @return An OperatorToken representing the operator, or null if no valid operator is found at the current position.
    private Token readOperator() {
        
        var tokenLine = line;
        var tokenColumn = column;
        
        var twoChar = "" + currentChar + peek();
        var type = OPERATORS.get(twoChar);
        
        if (type != null) {

            advance();
            advance();
            return new OperatorToken(type, tokenLine, tokenColumn);
        }
        
        var oneChar = "" + currentChar;
        type = OPERATORS.get(oneChar);
        
        if (type != null) {

            advance();
            return new OperatorToken(type, tokenLine, tokenColumn);
        }
        
        return null;
    }    

    /// Read a delimiter token, which can be one or two characters long.
    ///
    /// Checks for two-character delimiters first (e.g., {@code ::}), then checks for single-character delimiters (e.g., {@code (}, {@code }, {@code ,}).
    /// @return A DelimiterToken representing the delimiter, or null if no valid delimiter is found at the current position.
    private Token readDelimiter() {

        var tokenLine = line;
        var tokenColumn = column;
        
        var twoChar = "" + currentChar + peek();
        var type = DELIMITERS.get(twoChar);

        if (type != null) {

            advance();
            advance();
            return new DelimiterToken(type, tokenLine, tokenColumn);
        }
        
        var oneChar = "" + currentChar;
        type = DELIMITERS.get(oneChar);
        
        if (type != null) {

            advance();
            return new DelimiterToken(type, tokenLine, tokenColumn);
        }
        
        return null;
    }

    /// Get the next token from the source code.
    ///
    /// Skips whitespace and comments, then checks for literals, keywords, operators, and delimiters in that order.
    /// If no valid token is found, returns an {@code UNKNOWN} token for the current character.
    /// @return The next Token in the source code, or an EOF token if the end of the source is reached.
    public Token getNextToken() {

        while (currentChar != '\0') {

            if (Character.isWhitespace(currentChar)) {

                skipWhitespace();
                continue;
            }
            
            if (currentChar == '/' && (peek() == '/' || peek() == '*')) {

                skipComment();
                continue;
            }
            
            if (Character.isDigit(currentChar)) return readNumber();
            if (Character.isLetter(currentChar) || currentChar == '_') return readIdentifierOrKeyword();
            if (currentChar == '"') return readString();

            var operator = readOperator();
            if (operator != null) return operator;

            var delimiter = readDelimiter();
            if (delimiter != null) return delimiter;
            
            var tokenLine = line;
            var tokenColumn = column;
            advance();
            return new Token(Special.UNKNOWN, tokenLine, tokenColumn);
        }
        
        return new Token(Special.EOF, line, column);
    }

    /// Tokenize the entire source code into a list of tokens.
    ///
    /// Continuously calls {@code getNextToken()} until an {@code EOF} token is returned, adding each token to the list.
    /// The resulting list includes all tokens in the source code.
    /// @return A list of Tokens representing the tokenized source code.
    public List<Token> tokenize() {

        var tokens = new ArrayList<Token>();
        Token token;
        
        do {

            token = getNextToken();
            tokens.add(token);
        } while (token.getType() != Special.EOF);
        return tokens;
    }
}
