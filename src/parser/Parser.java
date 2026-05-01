package src.parser;

import src.lexer.Token;
import src.parser.ast.nodes.StatementNode;
import src.parser.parser.DeclarationParser;
import src.parser.parser.util.ParserBase;
import src.parser.parser.util.ParserState;
import src.parser.parser.ClassParser;
import src.parser.parser.ExpressionParser;

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
/// @see DeclarationParser
/// @see ClassParser
/// @see ExpressionParser
public class Parser extends ParserBase {
    
    private final DeclarationParser declarationParser;

    /// Constructs a new Parser with the given list of tokens. Initializes the parser state and the declaration parser.
    /// @param tokens The list of tokens to be parsed into an AST.
    public Parser(List<Token> tokens) {

        super(new ParserState(tokens));
        this.declarationParser = new DeclarationParser(state, this.symbolTable);
    }

    /// Parses the list of tokens into a list of statement nodes representing the program's AST.
    /// Continues parsing declarations until the end of the token list is reached.
    /// @return A list of StatementNode objects representing the parsed program.
    public List<StatementNode> parse() {

        var statements = new ArrayList<StatementNode>();
        while (!isAtEnd()) statements.add(declarationParser.parseDeclaration());
        return statements;
    }
}
