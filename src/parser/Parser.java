package parser;

import lexer.Token;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionParameter;
import parser.parser.DeclarationParser;
import parser.parser.util.ParseErrorsException;
import parser.parser.util.ParseException;
import parser.parser.util.ParserBase;
import parser.parser.util.ParserState;
import parser.parser.ClassParser;
import parser.parser.ExpressionParser;
import token.ReturnType;
import token.TypeRegistry;
import token.family.Delimiter;
import token.family.Keyword;
import token.family.PrimitiveType;

import java.util.ArrayList;
import java.util.List;

/// Main parser that coordinates specialized parsers for types, declarations, statements, and expressions.
/// Uses a modular architecture with separate parser classes for different language constructs:
/// - {@link DeclarationParser} handles function and variable declarations, statements, and control flow
/// - {@link ClassParser} handles class declarations and their members (fields, methods, constructors)
/// - {@link ExpressionParser} handles all expression parsing
///
/// <hr>
///
/// GRAMMAR FOR PROGRAM STRUCTURE
///
/// ```
/// program → declaration* EOF
/// ```
///
/// This rule defines the top-level structure of a program, which consists of zero or more declarations
/// followed by an end-of-file marker. The specialized parsers handle the specific details of each
/// declaration type and statement.
///
/// <p>The parser implements <em>error recovery</em>: when a {@link ParseException} is thrown while
/// parsing a top-level declaration the parser records the error, skips tokens until a safe
/// synchronisation point, and then continues with the next declaration.  At the end of the run,
/// if any errors were collected, a {@link ParseErrorsException} is thrown containing all of them so
/// that callers receive a complete picture of every problem in a single pass.</p>
///
/// @see DeclarationParser
/// @see ClassParser
/// @see ExpressionParser
public class Parser extends ParserBase {

    private final DeclarationParser declarationParser;

    /// Constructs a new Parser with the given list of tokens.
    /// Creates a fresh per-session {@link TypeRegistry}, pre-populates the global symbol table
    /// with built-in stdlib functions, and initialises the declaration parser.
    /// @param tokens The list of tokens to be parsed into an AST.
    public Parser(List<Token> tokens) {

        super(new ParserState(tokens), new TypeRegistry());
        this.declarationParser = new DeclarationParser(state, this.symbolTable, this.typeRegistry);
        registerStdLib();
    }

    /// Pre-registers built-in standard-library functions in the global symbol table so user code
    /// can call them without declaring them.
    ///
    /// Registered functions:
    /// <ul>
    ///   <li>{@code print(string value) → void}</li>
    ///   <li>{@code println(string value) → void}</li>
    ///   <li>{@code printInt(int value) → void}</li>
    ///   <li>{@code printFloat(float value) → void}</li>
    ///   <li>{@code printBool(boolean value) → void}</li>
    ///   <li>{@code readLine() → string}</li>
    ///   <li>{@code parseInt(string value) → int}</li>
    ///   <li>{@code parseFloat(string value) → float}</li>
    /// </ul>
    private void registerStdLib() {

        var voidType   = new ReturnType(PrimitiveType.VOID);
        var stringType = new ReturnType(PrimitiveType.STRING);
        var intType    = new ReturnType(PrimitiveType.INT);
        var floatType  = new ReturnType(PrimitiveType.FLOAT);
        var boolType   = new ReturnType(PrimitiveType.BOOL);

        registerFunc("print",      voidType,   new FunctionParameter(0, 0, "value", stringType));
        registerFunc("println",    voidType,   new FunctionParameter(0, 0, "value", stringType));
        registerFunc("printInt",   voidType,   new FunctionParameter(0, 0, "value", intType));
        registerFunc("printFloat", voidType,   new FunctionParameter(0, 0, "value", floatType));
        registerFunc("printBool",  voidType,   new FunctionParameter(0, 0, "value", boolType));
        registerFunc("readLine",   stringType);
        registerFunc("parseInt",   intType,    new FunctionParameter(0, 0, "value", stringType));
        registerFunc("parseFloat", floatType,  new FunctionParameter(0, 0, "value", stringType));
    }

    /// Convenience helper for registering a stdlib function with zero or more parameters.
    private void registerFunc(String name, ReturnType returnType, FunctionParameter... params) {
        symbolTable.register(new FunctionDeclarationStatement(0, 0, returnType, name, params, null));
    }

    /// Parses the list of tokens into a list of statement nodes representing the program's AST.
    ///
    /// <p>Errors are collected via {@link #synchronize() error recovery}: parsing continues after
    /// each top-level error.  When the token stream is exhausted, a {@link ParseErrorsException}
    /// is thrown if any errors were recorded during the run.</p>
    ///
    /// @return A list of StatementNode objects representing the parsed program (contains only
    ///         successfully parsed nodes).
    /// @throws ParseErrorsException if one or more parse errors were encountered.
    public List<StatementNode> parse() {

        var statements = new ArrayList<StatementNode>();
        var errors     = new ArrayList<ParseException>();

        while (isNotAtEnd()) try {
            statements.add(declarationParser.parseDeclaration());
        } catch (ParseException e) {

            errors.add(e);
            synchronize();
        }

        if (!errors.isEmpty()) throw new ParseErrorsException(errors);
        return statements;
    }

    // ─── Error recovery ────────────────────────────────────────────────────────

    /// Advances the token stream to the next plausible statement boundary so that parsing
    /// can resume after an error.
    ///
    /// <p>The synchronisation heuristics are (in order):</p>
    /// <ul>
    ///   <li>Skip the current token (the one that caused the error).</li>
    ///   <li>Stop at a semicolon (end of an expression statement).</li>
    ///   <li>Stop before any keyword that typically starts a new declaration or statement:
    ///       {@code class}, {@code if}, {@code while}, {@code for}, {@code switch}, {@code return},
    ///       {@code break}, {@code continue}.</li>
    /// </ul>
    private void synchronize() {

        if (isNotAtEnd()) advance();  // always skip the offending token
        while (isNotAtEnd()) {

            // A semicolon marks the end of the previous statement — safe to resume after it.
            if (previous().getType() == Delimiter.SEMICOLON) return;

            // Keywords that begin a new declaration or statement are also safe resume points.
            var next = peek().getType();
            if (next == Keyword.CLASS   || next == Keyword.IF      || next == Keyword.WHILE  ||
                next == Keyword.FOR     || next == Keyword.SWITCH  || next == Keyword.RETURN ||
                next == Keyword.BREAK   || next == Keyword.CONTINUE) return;

            advance();
        }
    }
}
