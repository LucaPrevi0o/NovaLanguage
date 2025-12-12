package src.parser.parser;

import src.lexer.*;
import src.lexer.token.LiteralToken;
import src.lexer.token.TypeToken;
import src.parser.ast.SymbolTable;
import src.parser.ast.nodes.ExpressionNode;
import src.parser.ast.nodes.StatementNode;
import src.parser.ast.nodes.statement.*;
import src.parser.ast.nodes.statement.conditional.*;
import src.parser.ast.nodes.statement.declaration.*;
import src.parser.parser.util.ParseException;
import src.parser.parser.util.ParserBase;
import src.parser.parser.util.ParserState;
import src.token.ReturnType;
import src.token.TokenFamily;
import src.token.TypeRegistry;
import src.token.family.AccessModifier;
import src.token.family.Delimiter;
import src.token.family.Keyword;
import src.token.family.Literal;
import src.token.family.Operator;
import src.token.family.PrimitiveType;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified parser for declarations and statements.
 * Handles:
 * - Declarations (variables, functions, classes)
 * - Statements (control flow, blocks, expressions)
 * - Type parsing (primitives and custom classes)
 * 
 * This combines the functionality of the former DeclarationParser and StatementParser
 * to eliminate circular dependencies.
 */
public class DeclarationParser extends ParserBase {
    
    private ExpressionParser expressionParser;
    private ClassParser classParser;
    
    public DeclarationParser(ParserState state) {
        super(state);
        // Share the symbol table with child parsers
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        this.classParser = new ClassParser(this);
    }
    
    /**
     * Constructor for child parsers (shares symbol table).
     */
    public DeclarationParser(ParserState state, SymbolTable symbolTable) {
        super(state, symbolTable);
        this.expressionParser = new ExpressionParser(state, symbolTable);
        this.classParser = new ClassParser(this);
    }
    
    // ============================================================================
    // TYPE PARSING (formerly TypeParser)
    // ============================================================================
    
    /**
     * Parse array dimensions: [] or [size] or [][]...
     * Returns a list of size expressions (null for unsized dimensions).
     */
    private ExpressionNode[] parseArrayDimensions() {

        List<ExpressionNode> arraySizes = new ArrayList<>();
        
        while (match(Delimiter.LSQUARE)) {
            
            if (!check(Delimiter.RSQUARE)) {

                ExpressionNode sizeExpr = expressionParser.parseExpression();
                arraySizes.add(sizeExpr);
            } else arraySizes.add(null);  // No size specified
            consume(Delimiter.RSQUARE, "Expect ']' after array dimension");
        }
        return arraySizes.toArray(new ExpressionNode[0]);
    }
    
    /**
     * Parse a complete type with optional array dimensions.
     * Returns a ReturnType object containing the TokenFamily and array information.
     */
    public ReturnType parseTypeWithArrays() {

        TokenFamily tokenFamily = parseType();
        ExpressionNode[] arraySizes = parseArrayDimensions();
        return new ReturnType(tokenFamily, arraySizes);
    }
    
    /**
     * Check if the current token can be used as a type (primitive type or class name).
     */
    public boolean isValidType(Token token) { return isTypeToken(token) || isClassName(token); }
    
    /**
     * Parse a type token (primitive or class name) and return a TokenFamily.
     * This handles both built-in types (int, float, etc.) and user-defined class types.
     */
    private TokenFamily parseType() {

        Token token = peek();
        
        // Try primitive type first
        if (isTypeToken(token)) {

            TypeToken typeToken = (TypeToken) advance();
            return typeToken.getType();
        } 
        
        // Try custom class type
        if (isClassName(token)) {

            LiteralToken classToken = (LiteralToken) advance();
            String className = classToken.getValue();
            
            // Get the type from TypeRegistry
            TokenFamily classType = TypeRegistry.getTokenFamilyByName(className);
            if (classType != null) return classType;
        } 
        
        throw new ParseException("Expect type (primitive or class name)", token);
    }
    
    /**
     * Check if a token represents a class name (i.e., an identifier that matches a registered class).
     */
    private boolean isClassName(Token token) {

        if (!(token instanceof LiteralToken)) return false;
        if (!(token.getType() instanceof Literal.IdentifierLiteral)) return false;

        String name = ((LiteralToken) token).getValue();
        return TypeRegistry.isCustomClass(name);
    }
    
    // ============================================================================
    // PUBLIC METHODS - Used by ClassParser
    // ============================================================================
    
    /**
     * Parse an expression (exposed for ClassParser).
     */
    public ExpressionNode parseExpression() { return expressionParser.parseExpression(); }
    
    /**
     * Parse function/method parameters (exposed for ClassParser).
     */
    public FunctionParameter[] parseParameters() {

        List<FunctionParameter> parameters = new ArrayList<>();
        
        if (!check(Delimiter.RPAREN)) do {

            if (parameters.size() >= 255) throw new ParseException("Cannot have more than 255 parameters", peek());
            
            // Parse parameter type with optional array dimensions
            ReturnType paramType = parseTypeWithArrays();
            
            // Parse parameter name
            Token paramName = consume(new Literal.IdentifierLiteral(), "Expect parameter name");
            parameters.add(new FunctionParameter(paramName.getLine(), paramName.getColumn(), getLiteralValue(paramName), paramType));
        } while (match(Delimiter.COMMA));
        
        return parameters.toArray(new FunctionParameter[0]);
    }
    
    // ============================================================================
    // DECLARATION PARSING
    // ============================================================================
    
    /**
     * declaration → classDecl | functionDecl | varDecl | statement
     */
    public StatementNode parseDeclaration() {

        // Check for access modifiers (public, private, protected)
        AccessModifier accessModifier = parseAccessModifier();
        
        // Class declaration: access_modifier class ClassName [:: SuperClassName] { ... }
        if (match(Keyword.CLASS)) {

            if (accessModifier == null) throw new ParseException("Class declaration requires an access modifier", previous());
            
            // Parse class declaration - it will register itself in the symbol table
            ClassDeclarationStatement classDecl = classParser.parseClassDeclaration(accessModifier);
            
            return classDecl;
        }
        
        // Check if this is a function or variable declaration
        if (isValidType(peek())) {
            
            // Parse type with optional array dimensions
            ReturnType returnType = parseTypeWithArrays();
            
            // Expect identifier (variable or function name)
            if (check(new Literal.IdentifierLiteral())) {

                Token nameToken = advance();
                String name = getLiteralValue(nameToken);
                
                // Function declaration: type[] IDENTIFIER "(" ...
                if (check(Delimiter.LPAREN)) return parseFunctionDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
                
                // Variable declaration: type[] IDENTIFIER ...
                return parseVariableDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
            }
        }
        
        return parseStatement();
    }
    /**
     * functionDecl → type[]* IDENTIFIER "(" parameters? ")" block
     */
    private FunctionDeclarationStatement parseFunctionDeclaration(ReturnType returnType, String name, int line, int column) {

        consume(Delimiter.LPAREN, "Expect '(' after function name");
        
        FunctionParameter[] parameters = parseParameters();

        consume(Delimiter.RPAREN, "Expect ')' after parameters");
        
        // Register function in CURRENT scope (before entering function scope)
        var decl = new FunctionDeclarationStatement(line, column, returnType, name, parameters, null);
        this.symbolTable.register(decl);  // Register function as symbol
        
        // Create NEW scope for function body (parameters + local variables)
        SymbolTable functionScope = enterScope();
        this.symbolTable = functionScope;
        
        // Register parameters in FUNCTION scope
        for (FunctionParameter param : parameters) {
            this.symbolTable.register(param);
        }
        
        // Update the ExpressionParser to use the new scope
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        // Parse function body in the new scope
        consume(Delimiter.LBRACE, "Expect '{' before function body");
        
        List<StatementNode> bodyStatements = new ArrayList<>();
        while (!check(Delimiter.RBRACE) && !isAtEnd()) {
            bodyStatements.add(parseDeclaration());
        }

        consume(Delimiter.RBRACE, "Expect '}' after block");
        
        // Exit function scope
        this.symbolTable = exitScope(functionScope);
        
        // Restore ExpressionParser with original scope
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        BlockStatement body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        
        // Update the function declaration with the actual body
        return new FunctionDeclarationStatement(line, column, returnType, name, parameters, body);
    }
    
    /**
     * varDecl → type[size?]* IDENTIFIER ("=" expression)? ("," IDENTIFIER ("=" expression)?)* ";"
     */
    private StatementNode parseVariableDeclaration(ReturnType type, String name, int line, int column) {

        // Parse first variable
        ExpressionNode initializer = null;
        if (match(Operator.ASSIGN)) initializer = expressionParser.parseExpression();
        
        // Create first variable declaration
        var firstDecl = new VariableDeclarationStatement(line, column, type, name, initializer);
        
        // Check for multiple declarations: int a, b = 5, c;
        if (match(Delimiter.COMMA)) {

            // Create a block to hold multiple declarations
            List<StatementNode> declarations = new ArrayList<>();
            declarations.add(firstDecl);
            
            do {

                Token nameToken = consume(new Literal.IdentifierLiteral(), "Expect variable name");
                String varName = getLiteralValue(nameToken);
                
                ExpressionNode init = null;
                if (match(Operator.ASSIGN)) init = expressionParser.parseExpression();
                
                var decl = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(), type, varName, init);
                this.symbolTable.register(decl);  // Register variable as symbol
                declarations.add(decl);
            } while (match(Delimiter.COMMA));

            consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");
            
            // Return a block containing all declarations
            return new BlockStatement(line, column, declarations.toArray(new StatementNode[0]));
        }

        consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");

        this.symbolTable.register(firstDecl);  // Register variable as symbol
        return firstDecl;
    }
    
    /**
     * Parse optional access modifier (public, private, protected).
     * Returns null if no access modifier is present.
     */
    private AccessModifier parseAccessModifier() {

        for (AccessModifier modifier : AccessModifier.values()) if (match(modifier)) return modifier;
        return null;  // No access modifier (package-private)
    }
    
    // ============================================================================
    // STATEMENT PARSING (formerly StatementParser)
    // ============================================================================
    
    /**
     * statement → exprStmt | ifStmt | whileStmt | forStmt | returnStmt | block
     */
    public StatementNode parseStatement() {

        if (match(Keyword.IF)) return parseIfStatement();
        if (match(Keyword.WHILE)) return parseWhileStatement();
        if (match(Keyword.FOR)) return parseForStatement();
        if (match(Keyword.RETURN)) return parseReturnStatement();
        if (match(Delimiter.LBRACE)) return parseBlock();

        return parseExpressionStatement();
    }
    
    /**
     * ifStmt → "if" "(" expression ")" statement ("else" statement)?
     */
    private IfStatement parseIfStatement() {

        Token ifToken = previous();

        consume(Delimiter.LPAREN, "Expect '(' after 'if'");
        ExpressionNode condition = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after if condition");

        StatementNode thenBranch = parseStatement();
        StatementNode elseBranch = null;

        if (match(Keyword.ELSE)) elseBranch = parseStatement();
        return new IfStatement(ifToken.getLine(), ifToken.getColumn(), condition, thenBranch, elseBranch);
    }
    
    /**
     * whileStmt → "while" "(" expression ")" statement
     */
    private WhileStatement parseWhileStatement() {

        Token whileToken = previous();

        consume(Delimiter.LPAREN, "Expect '(' after 'while'");
        ExpressionNode condition = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after while condition");

        StatementNode body = parseStatement();
        return new WhileStatement(whileToken.getLine(), whileToken.getColumn(), condition, body);
    }
    
    /**
     * forStmt → "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ")" statement
     */
    private ForStatement parseForStatement() {

        Token forToken = previous();
        consume(Delimiter.LPAREN, "Expect '(' after 'for'");
        
        // Create NEW scope for for loop (initializer + body)
        SymbolTable forScope = enterScope();
        this.symbolTable = forScope;
        
        // Update ExpressionParser to use the new scope
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        // Initializer
        StatementNode initializer = null;
        if (match(Delimiter.SEMICOLON)) {
            initializer = null;
        } else if (isTypeToken(peek())) {

            // Variable declaration: type IDENTIFIER = expr ;
            TypeToken type = (TypeToken) advance();
            Token nameToken = consume(new Literal.IdentifierLiteral(), "Expect variable name");
            String name = getLiteralValue(nameToken);
            
            // Map TypeToken to PrimitiveType
            TokenFamily tokenFamily;
            if (type.getType() instanceof PrimitiveType) tokenFamily = type.getType();
            else throw new ParseException("Unknown primitive type: " + type.getType(), type);
            
            ExpressionNode init = null;
            if (match(Operator.ASSIGN)) init = expressionParser.parseExpression();
            consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");
            
            initializer = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(),
                new ReturnType(tokenFamily, new ExpressionNode[0]), name, init);
            
            // Register the initializer variable in the FOR scope
            this.symbolTable.register((VariableDeclarationStatement) initializer);
        } else {

            // Expression statement: expr ;
            ExpressionNode expr = expressionParser.parseExpression();
            consume(Delimiter.SEMICOLON, "Expect ';' after expression");
            initializer = new ExpressionStatement(expr.getLine(), expr.getColumn(), expr);
        }
        
        // Condition (now 'i' is in scope)
        ExpressionNode condition = null;
        if (!check(Delimiter.SEMICOLON)) condition = expressionParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after loop condition");
        
        // Increment - just an expression, no semicolon required
        ExpressionNode incrementExpr = null;
        if (!check(Delimiter.RPAREN)) incrementExpr = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after for clauses");
        
        // Wrap increment expression in ExpressionStatement if present
        StatementNode increment = null;
        if (incrementExpr != null) increment = new ExpressionStatement(incrementExpr.getLine(), incrementExpr.getColumn(), incrementExpr);
        
        // Parse body in the FOR scope
        StatementNode body = parseStatement();
        
        // Exit for scope
        this.symbolTable = exitScope(forScope);
        
        // Restore ExpressionParser with original scope
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        return new ForStatement(forToken.getLine(), forToken.getColumn(), initializer, condition, increment, body);
    }
    
    /**
     * returnStmt → "return" expression? ";"
     */
    private ReturnStatement parseReturnStatement() {

        Token returnToken = previous();
        
        ExpressionNode value = null;
        if (!check(Delimiter.SEMICOLON)) value = expressionParser.parseExpression();

        consume(Delimiter.SEMICOLON, "Expect ';' after return value");
        return new ReturnStatement(returnToken.getLine(), returnToken.getColumn(), value);
    }
    
    /**
     * block → "{" statement* "}"
     */
    public BlockStatement parseBlock() {

        Token lbrace = previous();
        
        // Create NEW scope for block
        SymbolTable blockScope = enterScope();
        this.symbolTable = blockScope;
        
        // Update ExpressionParser to use the new scope
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        List<StatementNode> statements = new ArrayList<>();
        
        while (!check(Delimiter.RBRACE) && !isAtEnd()) {
            StatementNode stmt = parseDeclaration();
            statements.add(stmt);
        }

        consume(Delimiter.RBRACE, "Expect '}' after block");
        
        // Exit block scope
        this.symbolTable = exitScope(blockScope);
        
        // Restore ExpressionParser with original scope
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        return new BlockStatement(lbrace.getLine(), lbrace.getColumn(), statements.toArray(new StatementNode[0]));
    }
    
    /**
     * exprStmt → expression ";"
     */
    private ExpressionStatement parseExpressionStatement() {

        ExpressionNode expr = expressionParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after expression");
        return new ExpressionStatement(expr.getLine(), expr.getColumn(), expr);
    }
}
