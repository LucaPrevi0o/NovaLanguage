package parser.grammar;

import error.diagnostic.ParseException;
import lexer.Token;
import lexer.token.family.*;
import lexer.token.family.literal.IdentifierLiteral;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.*;
import parser.support.ParserBase;
import lexer.token.ReturnType;

import java.util.ArrayList;

/// Parses class declarations, including their methods, fields, superclasses, generic parameters, inner classes, access modifiers, and constructors.
///
/// <hr>
///
/// GRAMMAR FOR CLASS DECLARATIONS
///
/// ```
/// classDecl      → "class" IDENTIFIER ["[" IDENTIFIER "]"] ["::" IDENTIFIER ("," IDENTIFIER)*] "{" classMembers "}"
/// classMembers   → (classField | classMethod | constructor | innerClass)*
///
/// classField     → accessModifier type IDENTIFIER ("=" expression)? ";"
/// classMethod    → accessModifier type IDENTIFIER "(" parameters? ")" "{" declaration* "}"
/// constructor    → accessModifier CLASSNAME "(" parameters? ")" "{" declaration* "}"
/// innerClass     → accessModifier classDecl
///
/// accessModifier → "public" | "private" | "protected"
/// parameters     → type IDENTIFIER ("," type IDENTIFIER)*
/// ```
///
/// Features:
/// - **Generic Parameters**: Classes can have a single generic parameter specified with square brackets `[T]`
/// - **Superclasses**: Multiple inheritance supported using double colon `::` followed by comma-separated class names
/// - **Inner Classes**: Nested class declarations within the class body
/// - **Members**: Mix of fields (with optional initializers), methods, and constructors
/// - **Access Control**: Public, private, and protected modifiers are required for all class members
///
/// @see DeclarationParser
/// @see ExpressionParser
public class ClassParser extends ParserBase {

    private final DeclarationParser declarationParser;

    private record ClassHeader(Token classToken, String className, ReturnType genericParameter, ReturnType[] superClasses) { }

    private record ClassMembers(
        ArrayList<ClassFieldDeclaration> fields,
        ArrayList<ClassConstructorDeclaration> constructors,
        ArrayList<ClassMethodDeclaration> methods,
        ArrayList<ClassDeclarationStatement> innerClasses
    ) {

        private ClassMembers() {

            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    /// Constructs a new ClassParser with the given DeclarationParser.
    /// @param declarationParser The DeclarationParser to use for parsing types and other declarations within the class.
    public ClassParser(DeclarationParser declarationParser) {

        super(declarationParser.getState(), declarationParser.getTypeRegistry());
        this.declarationParser = declarationParser;
    }

    /// Parses a class declaration according to the grammar defined in the class documentation.
    ///
    /// Grammar rule:
    /// ```
    /// classDecl → "class" IDENTIFIER ["[" IDENTIFIER "]"] ["::" IDENTIFIER ("," IDENTIFIER)*]
    ///             "{" classMembers "}"
    /// ```
    /// @param classAccessModifier The access modifier of the class (e.g., public, private).
    /// @return A ClassDeclarationStatement representing the parsed class declaration.
    public ClassDeclarationStatement parseClassDeclaration(AccessModifier classAccessModifier) {

        var header = parseClassHeader();
        var classDecl = createClassDeclaration(header, classAccessModifier);

        typeRegistry.registerType(classDecl.getReturnType()); // Register class as a type before parsing members to allow for recursive references

        var members = parseClassBody(header);
        populateClassDeclaration(classDecl, members);
        return classDecl;
    }

    private ClassHeader parseClassHeader() {

        var classToken = previous(); // 'class' token was consumed by caller.
        var nameToken = consume(new IdentifierLiteral(), "Expect class name after 'class'");
        var className = getLiteralValue(nameToken);
        var genericParameter = parseGenericParameter();
        var superClasses = parseSuperClasses();

        return new ClassHeader(classToken, className, genericParameter, superClasses);
    }

    private ReturnType parseGenericParameter() {

        if (!match(Delimiter.LSQUARE)) return null;

        var genToken = consume(new IdentifierLiteral(), "Expect generic parameter name inside '[' ']'");
        var genericParameterName = getLiteralValue(genToken);
        consume(Delimiter.RSQUARE, "Expect ']' after generic parameter");

        var genericParameter = new ReturnType(new GenericParameterType(genericParameterName));
        typeRegistry.registerType(genericParameter); // Register generic parameter as a type to allow it to be used in member declarations
        return genericParameter;
    }

    private ReturnType[] parseSuperClasses() {

        var superClasses = new ArrayList<ReturnType>();
        if (!match(Delimiter.DOUBLE_COLON)) return superClasses.toArray(new ReturnType[0]);

        superClasses.add(parseSuperClass("Expect superclass name after '::'"));
        while (match(Delimiter.COMMA)) superClasses.add(parseSuperClass("Expect superclass name after ','"));
        return superClasses.toArray(new ReturnType[0]);
    }

    private ReturnType parseSuperClass(String message) {

        var superClassToken = consume(new IdentifierLiteral(), message);
        var superClassName = getLiteralValue(superClassToken);
        return new ReturnType(new NonPrimitiveType(superClassName));
    }

    private ClassDeclarationStatement createClassDeclaration(ClassHeader header, AccessModifier classAccessModifier) {

        return new ClassDeclarationStatement(
            header.classToken().getLine(),
            header.classToken().getColumn(),
            header.className(),
            new ClassMethodDeclaration[0],
            new ClassFieldDeclaration[0],
            header.superClasses(),
            header.genericParameter(),
            new ClassDeclarationStatement[0],
            classAccessModifier,
            new ClassConstructorDeclaration[0]
        );
    }

    private ClassMembers parseClassBody(ClassHeader header) {

        consume(Delimiter.LBRACE, "Expect '{' before class body");

        var members = new ClassMembers();

        while (isNotAtEnd() && !currentTokenIs(Delimiter.RBRACE)) try {

            parseClassMember(
                header.className(),
                header.classToken().getLine(),
                header.classToken().getColumn(),
                members
            );
        } catch (ParseException e) {

            state.report(e);
            synchronizeClassMember();
        }

        consume(Delimiter.RBRACE, "Expect '}' after class body");
        return members;
    }

    private void populateClassDeclaration(ClassDeclarationStatement classDecl, ClassMembers members) {

        classDecl.setMethods(members.methods().toArray(new ClassMethodDeclaration[0]));
        classDecl.setFields(members.fields().toArray(new ClassFieldDeclaration[0]));
        classDecl.setConstructors(members.constructors().toArray(new ClassConstructorDeclaration[0]));
        classDecl.setInnerClasses(members.innerClasses().toArray(new ClassDeclarationStatement[0]));
    }

    private void parseClassMember(
        String className,
        int classLine,
        int classColumn,
        ClassMembers members
    ) {

        var memberAccessModifier = parseAccessModifier();
        if (memberAccessModifier == null) throw parseError("Class member declaration requires an access modifier", peek());

        if (match(Keyword.CLASS)) {

            members.innerClasses().add(parseClassDeclaration(memberAccessModifier));
            return;
        }

        if (check(new IdentifierLiteral()) && className.equals(getLiteralValue(peek()))) {

            advance();  // consume constructor name
            members.constructors().add(parseConstructor(classLine, classColumn, memberAccessModifier));
            return;
        }

        if (!declarationParser.isValidType(peek()))
            throw parseError("Expect type, constructor, inner class, or closing '}' in class body", peek());

        var type = declarationParser.parseType();
        var memberNameToken = consume(new IdentifierLiteral(), "Expect member name");
        var memberName = getLiteralValue(memberNameToken);

        if (check(Delimiter.LPAREN)) members.methods().add(parseClassMethod(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
        else members.fields().add(parseClassField(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
    }

    /// Synchronizes after a class-body member error without escaping the current class body.
    private void synchronizeClassMember() {

        if (!isNotAtEnd() || currentTokenIs(Delimiter.RBRACE)) return;
        if (currentTokenIs(Delimiter.SEMICOLON)) {

            advance();
            return;
        }
        if (isClassMemberRecoveryBoundary(peek())) return;

        var braceDepth = 0;
        while (isNotAtEnd()) {

            if (braceDepth == 0 && currentTokenIs(Delimiter.RBRACE)) return;
            if (braceDepth == 0 && isClassMemberRecoveryBoundary(peek())) return;

            var consumedType = advance().getType();
            if (consumedType == Delimiter.LBRACE) braceDepth++;
            else if (consumedType == Delimiter.RBRACE && braceDepth > 0) braceDepth--;
            else if (consumedType == Delimiter.SEMICOLON && braceDepth == 0) return;
        }
    }

    private boolean isClassMemberRecoveryBoundary(Token token) {

        if (token == null) return false;

        var type = token.getType();
        return type == AccessModifier.PUBLIC ||
               type == AccessModifier.PRIVATE ||
               type == AccessModifier.PROTECTED;
    }

    /// Parses a class field declaration, which may optionally include an initializer.
    ///
    /// Grammar rule:
    /// ```
    /// classField → accessModifier type IDENTIFIER ("=" expression)? ";"
    /// ```
    /// @param type The return type of the field.
    /// @param name The name of the field.
    /// @param line The line number where the field declaration appears.
    /// @param column The column number where the field declaration starts.
    /// @param accessModifier The access modifier of the field (e.g., public, private).
    /// @return A ClassFieldDeclaration representing the parsed class field declaration.
    private ClassFieldDeclaration parseClassField(ReturnType type, String name, int line, int column, AccessModifier accessModifier) {

        ExpressionNode initializer = null;

        if (match(Operator.ASSIGN)) initializer = declarationParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after field declaration");
        return new ClassFieldDeclaration(line, column, type, name, initializer, accessModifier);
    }

    /// Parses a class method declaration, including its parameters and body.
    ///
    /// Grammar rule:
    /// ```
    /// classMethod → accessModifier type IDENTIFIER "(" parameters? ")" "{" declaration* "}"
    /// ```
    /// @param returnType The return type of the method.
    /// @param name The name of the method.
    /// @param line The line number where the method declaration appears.
    /// @param column The column number where the method declaration starts.
    /// @param accessModifier The access modifier of the method (e.g., public, private).
    /// @return A ClassMethodDeclaration representing the parsed class method declaration.
    private ClassMethodDeclaration parseClassMethod(ReturnType returnType, String name, int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after method name");

        var parameters = declarationParser.parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        consume(Delimiter.LBRACE, "Expect '{' before method body");

        var bodyStatements = declarationParser.parseBlockStatements();
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassMethodDeclaration(line, column, returnType, name, parameters, body, accessModifier);
    }

    /// Parses a class constructor declaration, including its parameters and body.
    ///
    /// Grammar rule:
    /// ```
    /// constructor → accessModifier CLASSNAME "(" parameters? ")" "{" declaration* "}"
    /// ```
    /// @param line The line number where the constructor declaration appears.
    /// @param column The column number where the constructor declaration starts.
    /// @param accessModifier The access modifier of the constructor (e.g., public, private).
    /// @return A ClassConstructorDeclaration representing the parsed class constructor declaration.
    private ClassConstructorDeclaration parseConstructor(int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after constructor name");

        var parameters = declarationParser.parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        consume(Delimiter.LBRACE, "Expect '{' before constructor body");

        var bodyStatements = declarationParser.parseBlockStatements();
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassConstructorDeclaration(line, column, parameters, body, accessModifier);
    }
}
