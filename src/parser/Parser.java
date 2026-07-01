package parser;

import lexer.Token;
import parser.ast.nodes.StatementNode;
import parser.grammar.DeclarationParser;
import error.diagnostic.ParseErrorsException;
import error.diagnostic.ParseException;
import parser.support.ParserBase;
import parser.support.ParserState;
import parser.grammar.ClassParser;
import parser.grammar.ExpressionParser;
import lexer.token.family.Delimiter;
import lexer.token.family.Keyword;

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
/// synchronization point, and then continues with the next declaration.  At the end of the run,
/// if any errors were collected, a {@link ParseErrorsException} is thrown containing all of them so
/// that callers receive a complete picture of every problem in a single pass.</p>
///
/// @see DeclarationParser
/// @see ClassParser
/// @see ExpressionParser
public class Parser extends ParserBase {

    private final DeclarationParser declarationParser;
    private List<StatementNode> parsedStatements = List.of();

    /// Constructs a new Parser with the given list of tokens.
    /// Creates shared parser state and initializes the declaration parser.
    /// @param tokens The list of tokens to be parsed into an AST.
    public Parser(List<Token> tokens) {

        super(new ParserState(tokens));
        this.declarationParser = new DeclarationParser(state);
    }

    /// Returns the successfully parsed top-level statements from the most recent parse run.
    /// This is useful when parse recovery collected diagnostics and {@link #parse()} threw an aggregate exception.
    /// @return An immutable list of successfully parsed top-level AST nodes.
    public List<StatementNode> getParsedStatements() { return List.copyOf(parsedStatements); }

    /// Parses the list of tokens into a list of statement nodes representing the program's AST.
    ///
    /// <p>Errors are collected via {@link #synchronize() error recovery}: parsing continues after
    /// each top-level error.  When the token stream is exhausted, a {@link ParseErrorsException}
    /// is thrown if any errors were recorded during the run.</p>
    ///
    /// @return A list of StatementNode objects representing the parsed program (contains only successfully parsed nodes).
    /// @throws ParseErrorsException if one or more parse errors were encountered.
    public List<StatementNode> parse() {

        state.clearErrors();
        var statements = new ArrayList<StatementNode>();
        parsedStatements = List.of();

        while (!state.isAtEnd()) try { statements.add(declarationParser.parseDeclaration()); }
        catch (ParseException e) {

            state.report(e);
            synchronize();
        }

        var errors = state.getErrors();
        parsedStatements = List.copyOf(statements);
        if (!errors.isEmpty()) throw new ParseErrorsException(errors);
        return parsedStatements;
    }

    // ─── Error recovery ────────────────────────────────────────────────────────

    /// Advances the token stream to the next plausible statement boundary so that parsing
    /// can resume after an error.
    ///
    /// <p>The synchronization heuristics are (in order):</p>
    /// <ul>
    ///   <li>Skip the current token (the one that caused the error).</li>
    ///   <li>Stop at a semicolon (end of an expression statement).</li>
    ///   <li>Stop before any keyword that typically starts a new declaration or statement:
    ///       {@code class}, {@code if}, {@code while}, {@code for}, {@code switch}, {@code return},
    ///       {@code break}, {@code continue}.</li>
    /// </ul>
    private void synchronize() {

        if (!state.isAtEnd()) state.advance();  // always skip the offending token
        while (!state.isAtEnd()) {

            // A semicolon marks the end of the previous statement — safe to resume after it.
            if (state.previous().getType() == Delimiter.SEMICOLON) return;

            // Keywords that begin a new declaration or statement are also safe resume points.
            var next = state.peek().getType();
            if (next == Keyword.CLASS   || next == Keyword.IF      || next == Keyword.WHILE  ||
                next == Keyword.FOR     || next == Keyword.SWITCH  || next == Keyword.RETURN ||
                next == Keyword.BREAK   || next == Keyword.CONTINUE) return;

            state.advance();
        }
    }
}
