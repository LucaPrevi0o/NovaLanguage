package src.parser.parser;

import src.lexer.*;
import src.lexer.token.LiteralToken;
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

        var arraySizes = new ArrayList<ExpressionNode>();
        
        while (match(Delimiter.LSQUARE)) {
            
            if (!check(Delimiter.RSQUARE)) {

                var sizeExpr = expressionParser.parseExpression();
                arraySizes.add(sizeExpr);
            } else arraySizes.add(null);  // No size specified
            consume(Delimiter.RSQUARE, "Expect ']' after array dimension");
        }
        return arraySizes.toArray(new ExpressionNode[0]);
    }
    
    /**
     * Check if the current token can be used as a type (primitive type or class name).
     */
    public boolean isValidType(Token token) { return isTypeToken(token) || isClassName(token); }
    
    /**
     * Parse a type token (primitive or class name) and return a TokenFamily.
     * This handles both built-in types (int, float, etc.) and user-defined class types.
     */
    protected ReturnType parseType() {

        var token = peek();
        if (isTypeToken(token)) {

            var typeToken = advance();
            var baseType = typeToken.getType(); // TokenFamily (PrimitiveType etc.)
            var arrayDims = parseArrayDimensions();
            return new ReturnType(baseType, arrayDims, null, null);
        } else if (isClassName(token)) {

            var classToken = (LiteralToken) advance();
            var className = classToken.getValue();

            var classType = TypeRegistry.getReturnType(className);
            var superTypes = classType.getSuperTypes();
            var genericParamType = classType.getGenericParameterType();
            if (classType != null) {

                var arrayDims = parseArrayDimensions();
                if (arrayDims.length == 0) return classType;
                return new ReturnType(classType.getBaseType(), arrayDims, superTypes, genericParamType);
            }
        } 

        throw new ParseException("Expect type (primitive or class name)", token);
    }
    
    /**
     * Check if a token represents a class name (i.e., an identifier that matches a registered class).
     */
    private boolean isClassName(Token token) {

        if (!(token instanceof LiteralToken)) return false;
        if (!(token.getType() instanceof Literal.IdentifierLiteral)) return false;

        var name = ((LiteralToken) token).getValue();
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

        var parameters = new ArrayList<FunctionParameter>();
        if (!check(Delimiter.RPAREN)) do {

            if (parameters.size() >= 255) throw new ParseException("Cannot have more than 255 parameters", peek());
            
            var paramType = parseType();
            var paramName = consume(new Literal.IdentifierLiteral(), "Expect parameter name");
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

        var accessModifier = parseAccessModifier();
        if (match(Keyword.CLASS)) {

            if (accessModifier == null) throw new ParseException("Class declaration requires an access modifier", previous());
            return classParser.parseClassDeclaration(accessModifier);
        }
        
        if (isValidType(peek())) {
            
            var returnType = parseType();
            if (check(new Literal.IdentifierLiteral())) {

                var nameToken = advance();
                var name = getLiteralValue(nameToken);
                
                if (check(Delimiter.LPAREN)) return parseFunctionDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
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
        
        var parameters = parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");
        
        var decl = new FunctionDeclarationStatement(line, column, returnType, name, parameters, null);
        this.symbolTable.register(decl);  // Register function as symbol
        
        var functionScope = enterScope();
        this.symbolTable = functionScope;
        
        for (FunctionParameter param : parameters) this.symbolTable.register(param);
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        consume(Delimiter.LBRACE, "Expect '{' before function body");
        
        var bodyStatements = new ArrayList<StatementNode>();
        while (!check(Delimiter.RBRACE) && !isAtEnd()) bodyStatements.add(parseDeclaration());

        consume(Delimiter.RBRACE, "Expect '}' after block");
        this.symbolTable = exitScope(functionScope);
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new FunctionDeclarationStatement(line, column, returnType, name, parameters, body);
    }
    
    /**
     * varDecl → type[size?]* IDENTIFIER ("=" expression)? ("," IDENTIFIER ("=" expression)?)* ";"
     */
    private StatementNode parseVariableDeclaration(ReturnType type, String name, int line, int column) {

        ExpressionNode initializer = null;
        if (match(Operator.ASSIGN)) initializer = expressionParser.parseExpression();
        
        var firstDecl = new VariableDeclarationStatement(line, column, type, name, initializer);
        if (match(Delimiter.COMMA)) {

            var declarations = new ArrayList<StatementNode>();
            declarations.add(firstDecl);
            
            do {

                var nameToken = consume(new Literal.IdentifierLiteral(), "Expect variable name");
                var varName = getLiteralValue(nameToken);
                
                ExpressionNode init = null;
                if (match(Operator.ASSIGN)) init = expressionParser.parseExpression();
                
                var decl = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(), type, varName, init);
                this.symbolTable.register(decl);  // Register variable as symbol
                declarations.add(decl);
            } while (match(Delimiter.COMMA));

            consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");
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

        for (var modifier : AccessModifier.values()) if (match(modifier)) return modifier;
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

        var ifToken = previous();

        consume(Delimiter.LPAREN, "Expect '(' after 'if'");
        var condition = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after if condition");

        var thenBranch = parseStatement();
        StatementNode elseBranch = null;

        if (match(Keyword.ELSE)) elseBranch = parseStatement();
        return new IfStatement(ifToken.getLine(), ifToken.getColumn(), condition, thenBranch, elseBranch);
    }
    
    /**
     * whileStmt → "while" "(" expression ")" statement
     */
    private WhileStatement parseWhileStatement() {

        var whileToken = previous();

        consume(Delimiter.LPAREN, "Expect '(' after 'while'");
        var condition = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after while condition");

        var body = parseStatement();
        return new WhileStatement(whileToken.getLine(), whileToken.getColumn(), condition, body);
    }
    
    /**
     * forStmt → "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ")" statement
     */
    private ForStatement parseForStatement() {

        var forToken = previous();
        consume(Delimiter.LPAREN, "Expect '(' after 'for'");
        
        var forScope = enterScope();
        this.symbolTable = forScope;
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        StatementNode initializer = null;
        if (match(Delimiter.SEMICOLON)) initializer = null;
        else if (isTypeToken(peek())) {

            var type = advance();
            var nameToken = consume(new Literal.IdentifierLiteral(), "Expect variable name");
            var name = getLiteralValue(nameToken);
            
            TokenFamily tokenFamily;
            if (type.getType() instanceof PrimitiveType) tokenFamily = type.getType();
            else throw new ParseException("Unknown primitive type: " + type.getType(), type);
            
            ExpressionNode init = null;
            if (match(Operator.ASSIGN)) init = expressionParser.parseExpression();
            consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");
            
            initializer = new VariableDeclarationStatement(
                nameToken.getLine(), nameToken.getColumn(),
                new ReturnType(tokenFamily, new ExpressionNode[0], null, null),
                name, init
            );
            
            this.symbolTable.register((VariableDeclarationStatement) initializer);
        } else {

            var expr = expressionParser.parseExpression();
            consume(Delimiter.SEMICOLON, "Expect ';' after expression");
            initializer = new ExpressionStatement(expr.getLine(), expr.getColumn(), expr);
        }
        
        ExpressionNode condition = null;
        if (!check(Delimiter.SEMICOLON)) condition = expressionParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after loop condition");
        
        ExpressionNode incrementExpr = null;
        if (!check(Delimiter.RPAREN)) incrementExpr = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after for clauses");
        
        StatementNode increment = null;
        if (incrementExpr != null) increment = new ExpressionStatement(incrementExpr.getLine(), incrementExpr.getColumn(), incrementExpr);
        
        var body = parseStatement();
        this.symbolTable = exitScope(forScope);
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        return new ForStatement(forToken.getLine(), forToken.getColumn(), initializer, condition, increment, body);
    }
    
    /**
     * returnStmt → "return" expression? ";"
     */
    private ReturnStatement parseReturnStatement() {

        var returnToken = previous();
        
        ExpressionNode value = null;
        if (!check(Delimiter.SEMICOLON)) value = expressionParser.parseExpression();

        consume(Delimiter.SEMICOLON, "Expect ';' after return value");
        return new ReturnStatement(returnToken.getLine(), returnToken.getColumn(), value);
    }
    
    /**
     * block → "{" statement* "}"
     */
    public BlockStatement parseBlock() {

        var lbrace = previous();
        
        var blockScope = enterScope();
        this.symbolTable = blockScope;
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        
        var statements = new ArrayList<StatementNode>();
        
        while (!check(Delimiter.RBRACE) && !isAtEnd()) statements.add(parseDeclaration());
        consume(Delimiter.RBRACE, "Expect '}' after block");
        
        this.symbolTable = exitScope(blockScope);
        this.expressionParser = new ExpressionParser(state, this.symbolTable);
        return new BlockStatement(lbrace.getLine(), lbrace.getColumn(), statements.toArray(new StatementNode[0]));
    }
    
    /**
     * exprStmt → expression ";"
     */
    private ExpressionStatement parseExpressionStatement() {

        var expr = expressionParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after expression");
        return new ExpressionStatement(expr.getLine(), expr.getColumn(), expr);
    }
}
