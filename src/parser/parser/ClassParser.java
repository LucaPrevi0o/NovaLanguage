package src.parser.parser;

import src.parser.ast.SymbolTable;
import src.parser.ast.nodes.ExpressionNode;
import src.parser.ast.nodes.StatementNode;
import src.parser.ast.nodes.statement.BlockStatement;
import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.object.*;
import src.parser.parser.util.ParseException;
import src.parser.parser.util.ParserBase;
import src.token.ReturnType;
import src.token.TypeRegistry;
import src.token.family.AccessModifier;
import src.token.family.Delimiter;
import src.token.family.GenericParameterType;
import src.token.family.Keyword;
import src.token.family.Literal;
import src.token.family.Operator;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles parsing of class declarations and their members.
 */
public class ClassParser extends ParserBase {
    
    private DeclarationParser declarationParser;
    
    public ClassParser(DeclarationParser declarationParser) {
        
        super(declarationParser.getState(), declarationParser.getSymbolTable());
        this.declarationParser = declarationParser;
    }
    
    /**
     * Parse class declaration:
     * [access_modifier] class ClassName [:: SuperClassName] {
     *     [access_modifier] type field;
     *     [access_modifier] ClassName(params) { ... }
     *     [access_modifier] returnType method(params) { ... }
     *     [access_modifier] class InnerClass { ... }
     * }
     */
    public ClassDeclarationStatement parseClassDeclaration(AccessModifier classAccessModifier) {

        var classToken = previous();
        
        var nameToken = consume(new Literal.IdentifierLiteral(), "Expect class name after 'class'");
        var className = getLiteralValue(nameToken);
        
        ReturnType superClass = null;
        ReturnType genericClassParameter = null;
        if (match(Delimiter.DOUBLE_COLON)) {

            if (match(Delimiter.LSQUARE)) {

                var genToken = consume(new Literal.IdentifierLiteral(), "Expect generic parameter name inside '[' ']'");
                var genericParameterName = getLiteralValue(genToken);
                var genericType = new GenericParameterType(genericParameterName);
                genericClassParameter = new ReturnType(genericType);
                consume(Delimiter.RSQUARE, "Expect ']' after generic parameter");
            } else {

                var superClassToken = consume(new Literal.IdentifierLiteral(), "Expect superclass name after '::'");
                var superClassName = getLiteralValue(superClassToken);
                superClass = TypeRegistry.getReturnType(superClassName);
                if (superClass == null) throw new ParseException("Superclass '" + superClassName + "' not found.", superClassToken);
            }
        }

        var classDecl = new ClassDeclarationStatement(
            classToken.getLine(),
            classToken.getColumn(),
            className,
            new ClassMethodDeclaration[0],
            new ClassFieldDeclaration[0],
            superClass,
            genericClassParameter,
            new ClassDeclarationStatement[0],
            classAccessModifier,
            new ClassConstructorDeclaration[0]
        );
        
        TypeRegistry.registerClass(classDecl);
        this.symbolTable.register(classDecl);

        consume(Delimiter.LBRACE, "Expect '{' before class body");

        var classScope = enterScope();
        this.symbolTable = classScope;
        
        // Update declarationParser to use the new scope
        this.declarationParser = new DeclarationParser(state, this.symbolTable);

        List<ClassFieldDeclaration> fields = new ArrayList<>();
        List<ClassConstructorDeclaration> constructors = new ArrayList<>();
        List<ClassMethodDeclaration> methods = new ArrayList<>();
        List<ClassDeclarationStatement> innerClasses = new ArrayList<>();
        
        while (!check(Delimiter.RBRACE) && !isAtEnd()) {

            var memberAccessModifier = parseAccessModifier();
            if (match(Keyword.CLASS)) {

                if (memberAccessModifier == null) throw new ParseException("Inner class declaration requires an access modifier", previous());
                innerClasses.add(parseClassDeclaration(memberAccessModifier));
                continue;
            }
            
            if (check(new Literal.IdentifierLiteral()) && getLiteralValue(peek()).equals(className)) {

                advance();  // consume class name
                constructors.add(parseConstructor(className, classToken.getLine(), classToken.getColumn(), memberAccessModifier));
                continue;
            }
            
            if (!declarationParser.isValidType(peek()))
                throw new ParseException("Expect type, constructor, inner class, or closing '}' in body", peek());
            
            var type = declarationParser.parseType();
            var memberNameToken = consume(new Literal.IdentifierLiteral(), "Expect member name");
            var memberName = getLiteralValue(memberNameToken);
            
            if (check(Delimiter.LPAREN))
                methods.add(parseClassMethod(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
            else fields.add(parseClassField(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
        }

        consume(Delimiter.RBRACE, "Expect '}' after class body");

        this.symbolTable = exitScope(classScope);
        this.declarationParser = new DeclarationParser(state, this.symbolTable);

        return new ClassDeclarationStatement(
            classToken.getLine(),
            classToken.getColumn(),
            className,
            methods.toArray(new ClassMethodDeclaration[0]),
            fields.toArray(new ClassFieldDeclaration[0]),
            superClass,
            genericClassParameter,
            innerClasses.toArray(new ClassDeclarationStatement[0]),
            classAccessModifier,
            constructors.toArray(new ClassConstructorDeclaration[0])
        );
    }
    
    /**
     * Parse optional access modifier (public, private, protected).
     * Returns null if no access modifier is present.
     */
    private AccessModifier parseAccessModifier() {

        if (match(AccessModifier.PUBLIC)) return AccessModifier.PUBLIC;
        if (match(AccessModifier.PRIVATE)) return AccessModifier.PRIVATE;
        if (match(AccessModifier.PROTECTED)) return AccessModifier.PROTECTED;
        return null;
    }
    
    /**
     * Parse class field: type fieldName [= initializer];
     */
    private ClassFieldDeclaration parseClassField(ReturnType type, String name, int line, int column, AccessModifier accessModifier) {

        ExpressionNode initializer = null;

        if (match(Operator.ASSIGN)) initializer = declarationParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after field declaration");
        var decl = new ClassFieldDeclaration(line, column, type, name, initializer, accessModifier);
        this.symbolTable.register(decl);  // Register field as symbol
        return decl;
    }
    
    /**
     * Parse class method: type methodName(params) { ... }
     */
    private ClassMethodDeclaration parseClassMethod(ReturnType returnType, String name, int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after method name");

        var parameters = declarationParser.parseParameters();

        consume(Delimiter.RPAREN, "Expect ')' after parameters");
        var decl = new ClassMethodDeclaration(line, column, returnType, name, parameters, null, accessModifier);
        this.symbolTable.register(decl);  // Register method as symbol
        
        var methodScope = enterScope();
        this.symbolTable = methodScope;
        
        this.declarationParser = new DeclarationParser(state, this.symbolTable);
        for (var param : parameters) this.symbolTable.register(param);  // Register parameters as symbols

        consume(Delimiter.LBRACE, "Expect '{' before method body");

        var bodyStatements = new ArrayList<StatementNode>();
        while (!check(Delimiter.RBRACE) && !isAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());
        
        consume(Delimiter.RBRACE, "Expect '}' after method body");

        this.symbolTable = exitScope(methodScope);
        this.declarationParser = new DeclarationParser(state, this.symbolTable);

        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassMethodDeclaration(line, column, returnType, name, parameters, body, accessModifier);
    }
    
    /**
     * Parse constructor: ClassName(params) { ... }
     */
    private ClassConstructorDeclaration parseConstructor(String className, int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after constructor name");

        var parameters = declarationParser.parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        SymbolTable constructorScope = enterScope();
        this.symbolTable = constructorScope;
        
        this.declarationParser = new DeclarationParser(state, this.symbolTable);
        
        for (var param : parameters) this.symbolTable.register(param);

        consume(Delimiter.LBRACE, "Expect '{' before constructor body");

        var bodyStatements = new ArrayList<StatementNode>();
        while (!check(Delimiter.RBRACE) && !isAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());
        
        consume(Delimiter.RBRACE, "Expect '}' after constructor body");

        this.symbolTable = exitScope(constructorScope);
        this.declarationParser = new DeclarationParser(state, this.symbolTable);

        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassConstructorDeclaration(line, column, parameters, body, accessModifier);
    }
}
