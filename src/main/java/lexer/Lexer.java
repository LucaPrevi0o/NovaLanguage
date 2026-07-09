package lexer;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticBag;
import error.diagnostic.DiagnosticPhase;
import lexer.token.TokenClass;
import lexer.token.family.*;
import lexer.token.family.literal.*;
import lexer.token.type.*;

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
    private final DiagnosticBag diagnostics;

    // Maps for quick lookup of pre-defined token types by their string representation
    private static final Map<String, TokenClass> TYPES = new HashMap<>();
    private static final Map<String, TokenClass> KEYWORDS = new HashMap<>();
    private static final Map<String, TokenClass> ACCESS_MODIFIERS = new HashMap<>();
    private static final Map<String, TokenClass> OPERATORS = new HashMap<>();
    private static final Map<String, TokenClass> DELIMITERS = new HashMap<>();
    private static final Map<String, TokenClass> LITERALS = new HashMap<>();

    static {

        // Automatically populate types from TokenType enum
        for (var value : PrimitiveType.values()) TYPES.put(value.token(), value);
        for (var value : Keyword.values()) KEYWORDS.put(value.token(), value);
        for (var value : AccessModifier.values()) ACCESS_MODIFIERS.put(value.token(), value);  // Add access modifiers
        for (var value : Operator.values()) OPERATORS.put(value.token(), value);
        for (var value : Delimiter.values()) DELIMITERS.put(value.token(), value);

        // Boolean literals "true" and "false" are tokenized as LiteralTokens with BooleanLiteral type
        LITERALS.put("true", BoolLiteral.TRUE);
        LITERALS.put("false", BoolLiteral.FALSE);
    }

    /// Constructs a Lexer with the given source code.
    /// Initializes the position, line, column, and current character.
    /// @param source The source code to be tokenized.
    public Lexer(String source) { this(source, new DiagnosticBag()); }

    /// Constructs a Lexer with the given source code and diagnostic bag.
    /// @param source The source code to be tokenized.
    /// @param diagnostics The diagnostic bag used to collect lexer diagnostics.
    public Lexer(String source, DiagnosticBag diagnostics) {

        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.currentChar = !source.isEmpty() ? source.charAt(0) : '\0';
        this.diagnostics = diagnostics != null ? diagnostics : new DiagnosticBag();
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

    /// Returns the diagnostics collected during tokenization.
    /// @return An immutable list of lexer diagnostics.
    public List<Diagnostic> getDiagnostics() { return diagnostics.getDiagnostics(); }

    /// Create an `UNKNOWN` token for unrecognized characters or sequences, and report a diagnostic.
    /// @param tokenLine The line number where the unrecognized token starts.
    /// @param tokenColumn The column number where the unrecognized token starts.
    /// @param lexeme The unrecognized character or sequence.
    /// @return An `UNKNOWN` Token representing the unrecognized token.
    private Token unknownToken(int tokenLine, int tokenColumn, String lexeme) {

        var token = new Token(Special.UNKNOWN, tokenLine, tokenColumn, lexeme);
        diagnostics.report(Diagnostic.error(DiagnosticPhase.LEXER, "Unrecognized token: '" + lexeme + "'", token));
        return token;
    }

    /// Skip over comments (single-line and multi-line).
    ///
    /// Single-line comments start with `//` and continue to the end of the line.
    /// Multi-line comments start with `/*` and end with `*/`, and can span multiple lines.
    private void skipComment() {

        if (currentChar == '/' && peek() == '/') skipSingleLineComment();
        else if (currentChar == '/' && peek() == '*') skipMultiLineComment();
    }

    /// Skip a single-line comment by advancing until the end of the line or end of file.
    private void skipSingleLineComment() { while (currentChar != '\0' && currentChar != '\n') advance(); }

    /// Skip a multi-line comment by advancing until the closing `*/` is found.
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
    ///
    /// Handles sequences of digits and at most one decimal point. A second decimal
    /// point stops lexing the number; the remaining characters will be tokenized
    /// separately so that callers receive a clear, isolated bad-token rather than
    /// one bloated number string.
    ///
    /// An optional type suffix (`l/L`, `f/F`, `d/D`, `b/B`) after the digits is also consumed.
    /// @return A LiteralToken representing the number literal.
    private Token readNumber() {

        var startLine = line;
        var startColumn = column;
        var number = new StringBuilder();
        var seenDot = false;

        while (currentChar != '\0' && (Character.isDigit(currentChar) || currentChar == '.')) {

            if (currentChar == '.') {

                if (seenDot) break;   // second dot — stop here; leave it for the next token
                seenDot = true;
            }
            number.append(currentChar);
            advance();
        }

        // Consume optional type suffix: l/L, f/F, d/D, b/B
        if (currentChar != '\0' && "lLfFdDbB".indexOf(currentChar) >= 0) {

            number.append(currentChar);
            advance();
        }

        return new LiteralToken(new NumberLiteral(number.toString()), startLine, startColumn);
    }

    /// Read an identifier or keyword, which starts with a letter or underscore and can contain letters, digits, and underscores.
    ///
    /// After reading the sequence, check if the current token matches a keyword or type; if not, treat it as an identifier.
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

        var asLiteral = LITERALS.get(value);
        var asKeyword = KEYWORDS.get(value);
        var asAccessModifier = ACCESS_MODIFIERS.get(value);
        var asPrimitiveType = TYPES.get(value);

        if (asLiteral != null) return new LiteralToken((Literal) asLiteral, startLine, startColumn);
        if (asKeyword != null) return new KeywordToken((Keyword) asKeyword, startLine, startColumn);
        if (asAccessModifier != null) return new KeywordToken((AccessModifier) asAccessModifier, startLine, startColumn);
        if (asPrimitiveType != null) return new TypeToken((PrimitiveType) asPrimitiveType, startLine, startColumn);
        return new LiteralToken(new IdentifierLiteral(value), startLine, startColumn);
    }

    /// Read a string literal, which starts and ends with double quotes.
    ///
    /// Handles escape sequences for double quotes (e.g., `\\"` becomes a double quote in the string).
    /// Returns an `UNKNOWN` token when end-of-file is reached before the closing double quote.
    /// @return A LiteralToken representing the string literal, or an `UNKNOWN` token on error.
    private Token readString() {

        var startLine = line;
        var startColumn = column;
        var string = new StringBuilder();
        var raw = new StringBuilder("\"");
        advance(); // skip opening quote

        while (currentChar != '\0' && currentChar != '"') if (currentChar == '\\') {

            raw.append(currentChar);
            advance(); // skip backslash
            if (currentChar == '\0') return unknownToken(startLine, startColumn, raw.toString());

            raw.append(currentChar);
            // Handle escape sequences
            var escapedChar = switch (currentChar) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case '"' -> '"';
                case '\'' -> '\'';
                case '\\' -> '\\';
                default -> currentChar;  // Unrecognized escape: pass through verbatim
            };
            string.append(escapedChar);
            advance();
        } else {

            raw.append(currentChar);
            string.append(currentChar);
            advance();
        }

        if (currentChar == '\0') return unknownToken(startLine, startColumn, raw.toString());

        advance(); // skip closing quote
        return new LiteralToken(new StringLiteral(string.toString()), startLine, startColumn);
    }

    /// Read a character literal, which is a single character enclosed in single quotes.
    /// Handles escape sequences similar to string literals.
    /// Returns an {@code UNKNOWN} token when the literal is empty ({@code ''}) or unterminated
    /// (end-of-file reached before the closing single quote).
    /// @return A LiteralToken representing the character literal, or an `UNKNOWN` token on error.
    private Token readChar() {

        var startLine = line;
        var startColumn = column;
        advance(); // skip opening quote

        // Empty char literal: ''
        if (currentChar == '\'') {

            advance(); // skip closing quote
            return unknownToken(startLine, startColumn, "''");
        }

        // Unterminated char literal: reached EOF before closing quote
        if (currentChar == '\0') return unknownToken(startLine, startColumn, "'");

        var ch = '\0';
        var raw = new StringBuilder("'");
        if (currentChar == '\\') {

            raw.append('\\');
            advance(); // skip backslash
            if (currentChar == '\0') return unknownToken(startLine, startColumn, raw.toString());

            // Handle escape sequences
            raw.append(currentChar);
            ch = switch (currentChar) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case '\'' -> '\'';
                case '\\' -> '\\';
                default -> currentChar;
            };
            advance();
        } else {

            raw.append(currentChar);
            ch = currentChar;
            advance();
        }

        // Expect closing quote; if missing, return UNKNOWN
        if (currentChar != '\'') return unknownToken(startLine, startColumn, raw.toString());
        advance(); // skip closing quote
        return new LiteralToken(new CharLiteral(ch), startLine, startColumn);
    }

    /// Read an operator token, which can be one or two characters long.
    ///
    /// Checks for two-character operators first (e.g., {@code ==}, {@code !=}, {@code <=}, {@code >=}), then checks
    /// for single-character operators (e.g., {@code +}, {@code -}, {@code *}, {@code /}).
    /// @return An OperatorToken representing the operator, or `null` if no valid operator is found at the current position.
    private Token readOperator() {
        
        var tokenLine = line;
        var tokenColumn = column;
        
        var twoChar = "" + currentChar + peek();
        var type = OPERATORS.get(twoChar);
        
        if (type != null) {

            advance();
            advance();
            return new OperatorToken((Operator) type, tokenLine, tokenColumn);
        }
        
        var oneChar = "" + currentChar;
        type = OPERATORS.get(oneChar);
        
        if (type != null) {

            advance();
            return new OperatorToken((Operator) type, tokenLine, tokenColumn);
        }
        
        return null;
    }    

    /// Read a delimiter token, which can be one or two characters long.
    ///
    /// Checks for two-character delimiters first (e.g., {@code ::}), then checks for single-character delimiters (e.g., {@code (}, {@code }, {@code ,}).
    /// @return A DelimiterToken representing the delimiter, or `null` if no valid delimiter is found at the current position.
    private Token readDelimiter() {

        var tokenLine = line;
        var tokenColumn = column;
        
        var twoChar = "" + currentChar + peek();
        var asDelimiter = DELIMITERS.get(twoChar);

        if (asDelimiter != null) {

            advance();
            advance();
            return new DelimiterToken((Delimiter) asDelimiter, tokenLine, tokenColumn);
        }
        
        var oneChar = "" + currentChar;
        asDelimiter = DELIMITERS.get(oneChar);
        
        if (asDelimiter != null) {

            advance();
            return new DelimiterToken((Delimiter) asDelimiter, tokenLine, tokenColumn);
        }
        
        return null;
    }

    /// Get the next token from the source code.
    ///
    /// Skips whitespace and comments, then checks for literals, keywords, operators, and delimiters in that order.
    /// If no valid token is found, returns an {@code UNKNOWN} token for the current character.
    /// @return The next Token in the source code, or an `EOF` token if the end of the source is reached.
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
            if (currentChar == '\'') return readChar();

            var operator = readOperator();
            if (operator != null) return operator;

            var delimiter = readDelimiter();
            if (delimiter != null) return delimiter;
            
            var tokenLine = line;
            var tokenColumn = column;
            var lexeme = Character.toString(currentChar);
            advance();
            return unknownToken(tokenLine, tokenColumn, lexeme);
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
