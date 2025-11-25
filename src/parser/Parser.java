package src.parser;

import src.lexer.Token;
import src.parser.ast.nodes.StatementNode;
import src.parser.parser.DeclarationParser;
import src.parser.parser.util.ParserBase;
import src.parser.parser.util.ParserState;

import java.util.ArrayList;
import java.util.List;

/**
 * Main parser that coordinates specialized parsers for expressions, statements, and declarations.
 * Uses a modular architecture with separate parser classes for different language constructs.
 * 
 * Grammar (simplified):
 * 
 * program        → declaration* EOF
 * declaration    → functionDecl | varDecl | statement
 * functionDecl   → type IDENTIFIER "(" parameters? ")" block
 * varDecl        → type IDENTIFIER ("=" expression)? ";"
 * statement      → exprStmt | ifStmt | whileStmt | forStmt | returnStmt | block
 * block          → "{" declaration* "}"
 * 
 * expression     → assignment
 * assignment     → IDENTIFIER "=" assignment | logicOr
 * logicOr        → logicAnd ("||" logicAnd)*
 * logicAnd       → equality ("&&" equality)*
 * equality       → comparison (("==" | "!=") comparison)*
 * comparison     → term (("<" | ">" | "<=" | ">=") term)*
 * term           → factor (("+" | "-") factor)*
 * factor         → unary (("*" | "/") unary)*
 * unary          → ("!" | "-" | "++") unary | call
 * call           → primary ("(" arguments? ")")*
 * primary        → NUMBER | STRING | "true" | "false" | IDENTIFIER | "(" expression ")"
 */
public class Parser extends ParserBase {
    
    private final DeclarationParser declarationParser;
    
    public Parser(List<Token> tokens) {

        super(new ParserState(tokens));
        
        // DeclarationParser creates its own ExpressionParser internally
        this.declarationParser = new DeclarationParser(state);
    }
    
    // ========== Public API ==========
    
    /**
     * Parse the entire program and return the AST (list of statements).
     * Throws ParseException immediately if any parsing error occurs.
     */
    public List<StatementNode> parse() {

        List<StatementNode> statements = new ArrayList<>();
        while (!isAtEnd()) statements.add(declarationParser.parseDeclaration());
        
        // Collect all symbols from the declaration parser (which includes symbols from ClassParser)
        for (var sym : declarationParser.getSymbolTable().getSymbols()) this.symbolTable.register(sym);
        
        return statements;
    }
}
