package src.parser.parser;

import src.lexer.Token;
import src.parser.ast.nodes.ExpressionNode;
import src.parser.ast.nodes.StatementNode;
import src.parser.ast.nodes.statement.BlockStatement;
import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.FunctionParameter;
import src.parser.ast.nodes.statement.declaration.object.*;
import src.parser.parser.util.ParseException;
import src.parser.parser.util.ParserBase;
import src.token.ReturnType;
import src.token.TypeRegistry;
import src.token.family.AccessModifier;
import src.token.family.Delimiter;
import src.token.family.Keyword;
import src.token.family.Literal;
import src.token.family.Operator;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles parsing of class declarations and their members.
 */
public class ClassParser extends ParserBase {
    
    private final DeclarationParser declarationParser;
    
    public ClassParser(DeclarationParser declarationParser) {

        super(declarationParser.getState());
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

        Token classToken = previous();
        
        // Parse class name
        Token nameToken = consume(new Literal.IdentifierLiteral(), "Expect class name after 'class'");
        String className = getLiteralValue(nameToken);
        
        // Check for inheritance: :: SuperClassName
        ClassDeclarationStatement superClass = null;
        if (match(Delimiter.DOUBLE_COLON)) {

            Token superClassToken = consume(new Literal.IdentifierLiteral(), "Expect superclass name after '::'");
            String superClassName = getLiteralValue(superClassToken);
            
            // Search for the superclass in TypeRegistry
            superClass = TypeRegistry.getClassDeclaration(superClassName);
            if (superClass == null) throw new ParseException("Superclass '" + superClassName + "' not found.", superClassToken);
        }

        consume(Delimiter.LBRACE, "Expect '{' before class body");

        List<ClassFieldDeclaration> fields = new ArrayList<>();
        List<ClassConstructorDeclaration> constructors = new ArrayList<>();
        List<ClassMethodDeclaration> methods = new ArrayList<>();
        List<ClassDeclarationStatement> innerClasses = new ArrayList<>();
        
        // Parse class body
        while (!check(Delimiter.RBRACE) && !isAtEnd()) {

            // Parse member access modifier
            AccessModifier memberAccessModifier = parseAccessModifier();
            
            // Check for inner class: [access_modifier] class InnerClassName { ... }
            if (match(Keyword.CLASS)) {

                if (memberAccessModifier == null) throw new ParseException("Inner class declaration requires an access modifier", previous());
                innerClasses.add(parseClassDeclaration(memberAccessModifier));
                continue;
            }
            
            // Check for constructor: [access_modifier] ClassName(params) { ... }
            // Constructor has NO return type, so it's: access_modifier followed directly by class name
            if (check(new Literal.IdentifierLiteral()) && getLiteralValue(peek()).equals(className)) {

                advance();  // consume class name
                constructors.add(parseConstructor(className, classToken.getLine(), classToken.getColumn(), memberAccessModifier));
                continue;
            }
            
            // Must be a type (field or method) - can be primitive or class name
            if (!declarationParser.isValidType(peek())) throw new ParseException("Expect type, constructor, inner class, or closing '}' in body", peek());
            
            // Parse type with array dimensions
            ReturnType type = declarationParser.parseTypeWithArrays();

            Token memberNameToken = consume(new Literal.IdentifierLiteral(), "Expect member name");
            String memberName = getLiteralValue(memberNameToken);
            
            // Method: type methodName(params) { ... }
            if (check(Delimiter.LPAREN))
                methods.add(parseClassMethod(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
            // Field: type fieldName [= initializer];
            else fields.add(parseClassField(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
        }

        consume(Delimiter.RBRACE, "Expect '}' after class body");

        ClassDeclarationStatement classDecl = new ClassDeclarationStatement(
            classToken.getLine(),
            classToken.getColumn(),
            className,
            methods.toArray(new ClassMethodDeclaration[0]),
            fields.toArray(new ClassFieldDeclaration[0]),
            superClass,
            innerClasses.toArray(new ClassDeclarationStatement[0]),
            classAccessModifier,
            constructors.toArray(new ClassConstructorDeclaration[0])
        );
        
        // Register this class in TypeRegistry for future type resolution and inheritance
        TypeRegistry.registerClass(classDecl);
        this.symbolTable.register(classDecl);  // Register class as symbol
        return classDecl;
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

        FunctionParameter[] parameters = declarationParser.parseParameters();

        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        // Parse method body
        consume(Delimiter.LBRACE, "Expect '{' before method body");

        List<StatementNode> bodyStatements = new ArrayList<>();
        while (!check(Delimiter.RBRACE) && !isAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());
        consume(Delimiter.RBRACE, "Expect '}' after method body");

        BlockStatement body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        var decl = new ClassMethodDeclaration(line, column, returnType, name, parameters, body, accessModifier);
        this.symbolTable.register(decl);  // Register method as symbol
        return decl;
    }
    
    /**
     * Parse constructor: ClassName(params) { ... }
     */
    private ClassConstructorDeclaration parseConstructor(String className, int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after constructor name");

        FunctionParameter[] parameters = declarationParser.parseParameters();

        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        // Parse constructor body
        consume(Delimiter.LBRACE, "Expect '{' before constructor body");

        List<StatementNode> bodyStatements = new ArrayList<>();
        while (!check(Delimiter.RBRACE) && !isAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());
        consume(Delimiter.RBRACE, "Expect '}' after constructor body");

        BlockStatement body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassConstructorDeclaration(line, column, parameters, body, accessModifier);
    }
}
