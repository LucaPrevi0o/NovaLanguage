package parser;

import error.ErrorCollector;
import lexer.Token;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionParameter;
import parser.parser.DeclarationParser;
import parser.parser.util.ParserBase;
import parser.parser.util.ParserState;
import parser.parser.ClassParser;
import parser.parser.ExpressionParser;
import lexer.token.ReturnType;
import lexer.token.TypeRegistry;
import lexer.token.family.PrimitiveType;

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
/// The parser implements *error recovery*: when a syntax error is encountered, it is recorded in the global
/// {@link ErrorCollector}, and the parser synchronizes to the next plausible statement boundary to continue parsing.
/// This allows multiple syntax errors to be collected in a single run, rather than stopping at the first error.
///
/// @see DeclarationParser
/// @see ClassParser
/// @see ExpressionParser
public class Parser extends ParserBase {

    private final DeclarationParser declarationParser;

    /// Constructs a new Parser with the given list of tokens.
    /// Creates a fresh per-session {@link TypeRegistry}, pre-populates the global symbol table
    /// with built-in stdlib functions, and initializes the declaration parser.
    /// @param tokens The list of tokens to be parsed into an AST.
    public Parser(List<Token> tokens) {

        super(new ParserState(tokens), new TypeRegistry());
        this.declarationParser = new DeclarationParser(state, this.symbolTable, this.typeRegistry);
        ErrorCollector.clear();  // Reset the global collector for each new parse session
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
    /// The parser implements error recovery: if a syntax error is encountered, it is recorded in the global
    /// {@link ErrorCollector}, and the parser synchronizes to the next plausible statement boundary to
    /// continue parsing.
    /// This allows multiple syntax errors to be collected in a single run, rather than stopping at the first error.
    ///
    /// @return A list of StatementNode objects representing the parsed program (contains only
    /// successfully parsed nodes).
    public List<StatementNode> parse() {

        var statements = new ArrayList<StatementNode>();
        while (isNotAtEnd()) statements.add(declarationParser.parseDeclaration());
        return statements;
    }
}
