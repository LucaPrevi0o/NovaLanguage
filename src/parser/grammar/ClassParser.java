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
import parser.ast.nodes.type.GenericTypeSyntax;
import parser.ast.nodes.type.NamedTypeSyntax;
import parser.ast.nodes.type.TypeSyntax;
import parser.support.ParserBase;

import java.util.ArrayList;
import java.util.List;

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

    /// Represents the header of a class declaration, including its name, generic parameter, and superclasses.
    /// @param classToken The token representing the 'class' keyword.
    /// @param className The name of the class being declared.
    /// @param genericParameters The optional generic parameters of the class, or `null` if none are specified.
    /// @param superClasses An array of TypeSyntax nodes representing the superclasses of the class, or an empty array if none are specified.
    private record ClassHeader(Token classToken, String className, TypeSyntax[] genericParameters, TypeSyntax[] superClasses) { }

    /// Represents the members of a class, including fields, constructors, methods, and inner classes.
    /// @param fields A list of ClassFieldDeclaration representing the fields of the class.
    /// @param constructors A list of ClassConstructorDeclaration representing the constructors of the class.
    /// @param methods A list of ClassMethodDeclaration representing the methods of the class.
    /// @param innerClasses A list of ClassDeclarationStatement representing the inner classes of the class.
    private record ClassMembers(List<ClassFieldDeclaration> fields, List<ClassConstructorDeclaration> constructors,
    List<ClassMethodDeclaration> methods, List<ClassDeclarationStatement> innerClasses) {

        /// Constructs a new ClassMembers instance with empty initializer lists.
        private ClassMembers() { this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()); }
    }

    /// Constructs a new ClassParser with the given DeclarationParser.
    /// @param declarationParser The DeclarationParser to use for parsing types and other declarations within the class.
    public ClassParser(DeclarationParser declarationParser) {

        super(declarationParser.getState());
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

        var members = parseClassBody(header);
        populateClassDeclaration(classDecl, members);
        return classDecl;
    }

    /// Parses the header of a class declaration, including its name, optional generic parameter, and optional superclasses.
    ///
    /// Grammar rule:
    /// ```
    /// classHeader → "class" IDENTIFIER ["[" IDENTIFIER "]"] ["::" IDENTIFIER ("," IDENTIFIER)*]
    /// ```
    /// @return A ClassHeader containing the parsed class name, generic parameter, and superclasses.
    private ClassHeader parseClassHeader() {

        var classToken = state.previous(); // 'class' token was consumed by caller.
        var nameToken = state.consume(new IdentifierLiteral(), "Expect class name after 'class'");
        var className = state.getLiteralValue(nameToken);
        var genericParameters = parseGenericParameters();
        var superClasses = parseSuperClasses();
        return new ClassHeader(classToken, className, genericParameters, superClasses);
    }

    /// Parses a single superclass name for a class declaration, which is expected to be an identifier.
    ///
    /// @param message The error message to display if the superclass name is not found.
    /// @return A TypeSyntax node representing the parsed superclass.
    private TypeSyntax parseGenericParameter(String message) {

        var genericParameterToken = state.consume(new IdentifierLiteral(), message);
        var genericParameterName = state.getLiteralValue(genericParameterToken);
        return new GenericTypeSyntax(genericParameterToken.getLine(), genericParameterToken.getColumn(), genericParameterName);
    }

    /// Parses an optional generic parameter for a class declaration, which is specified inside square brackets.
    ///
    /// Grammar rule:
    /// ```
    /// genericParameter → "[" IDENTIFIER "]"
    /// ```
    /// @return A TypeSyntax node representing the generic parameter, or `null` if no generic parameter is present.
    private TypeSyntax[] parseGenericParameters() {

        var genericParameters = new ArrayList<TypeSyntax>();
        if (!state.match(Delimiter.LSQUARE)) return genericParameters.toArray(new TypeSyntax[0]);

        genericParameters.add(parseGenericParameter("Expect generic type name after '['"));
        while (state.match(Delimiter.COMMA)) genericParameters.add(parseGenericParameter("Expect generic type name after ','"));
        state.consume(Delimiter.RSQUARE, "Expect ']' after generic type name");
        return genericParameters.toArray(new TypeSyntax[0]);
    }

    /// Parses an optional list of superclasses for a class declaration, which is specified after a double colon `::` and can include multiple comma-separated class names.
    ///
    /// Grammar rule:
    /// ```
    /// superClasses → "::" IDENTIFIER ("," IDENTIFIER)*
    /// ```
    /// @return An array of TypeSyntax nodes representing the superclasses, or an empty array if no superclasses are specified.
    private TypeSyntax[] parseSuperClasses() {

        var superClasses = new ArrayList<TypeSyntax>();
        if (!state.match(Delimiter.DOUBLE_COLON)) return superClasses.toArray(new TypeSyntax[0]);

        superClasses.add(parseSuperClass("Expect superclass name after '::'"));
        while (state.match(Delimiter.COMMA)) superClasses.add(parseSuperClass("Expect superclass name after ','"));
        return superClasses.toArray(new TypeSyntax[0]);
    }

    /// Parses a single superclass name for a class declaration, which is expected to be an identifier.
    ///
    /// @param message The error message to display if the superclass name is not found.
    /// @return A TypeSyntax node representing the parsed superclass.
    private TypeSyntax parseSuperClass(String message) {

        var superClassToken = state.consume(new IdentifierLiteral(), message);
        var superClassName = state.getLiteralValue(superClassToken);
        return new NamedTypeSyntax(superClassToken.getLine(), superClassToken.getColumn(), superClassName, false);
    }

    /// Creates a ClassDeclarationStatement with the given header and access modifier, initializing its members to empty arrays.
    /// @param header The ClassHeader containing the class name, generic parameter, and superclasses.
    /// @param classAccessModifier The access modifier of the class (e.g., public, private).
    /// @return A ClassDeclarationStatement representing the class declaration with empty members.
    private ClassDeclarationStatement createClassDeclaration(ClassHeader header, AccessModifier classAccessModifier) {

        return new ClassDeclarationStatement(
            header.classToken().getLine(),
            header.classToken().getColumn(),
            header.className(),
            new ClassMethodDeclaration[0],
            new ClassFieldDeclaration[0],
            header.superClasses(),
            header.genericParameters(),
            new ClassDeclarationStatement[0],
            classAccessModifier,
            new ClassConstructorDeclaration[0]
        );
    }

    /// Parses the body of a class declaration, which includes its fields, methods, constructors, and inner classes.
    ///
    /// Grammar rule:
    /// ```
    /// classMembers → (classField | classMethod | constructor | innerClass)*
    /// ```
    /// @param header The ClassHeader containing the class name, generic parameter, and superclasses.
    /// @return A ClassMembers object containing the parsed fields, methods, constructors, and inner classes of the class.
    private ClassMembers parseClassBody(ClassHeader header) {

        state.consume(Delimiter.LBRACE, "Expect '{' before class body");

        var members = new ClassMembers();
        while (!state.isAtEnd() && !currentTokenIs(Delimiter.RBRACE)) try {

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

        state.consume(Delimiter.RBRACE, "Expect '}' after class body");
        return members;
    }

    /// Populates the given ClassDeclarationStatement with the parsed members (fields, methods, constructors, and inner classes).
    /// @param classDecl The ClassDeclarationStatement to populate.
    /// @param members The ClassMembers containing the parsed fields, methods, constructors, and inner classes to add to the class declaration.
    private void populateClassDeclaration(ClassDeclarationStatement classDecl, ClassMembers members) {

        classDecl.setMethods(members.methods().toArray(new ClassMethodDeclaration[0]));
        classDecl.setFields(members.fields().toArray(new ClassFieldDeclaration[0]));
        classDecl.setConstructors(members.constructors().toArray(new ClassConstructorDeclaration[0]));
        classDecl.setInnerClasses(members.innerClasses().toArray(new ClassDeclarationStatement[0]));
    }

    /// Parses a single class member, which can be a field, method, constructor, or inner class, and adds it to the provided ClassMembers object.
    ///
    /// Grammar rule:
    /// ```
    /// classMember → accessModifier (classField | classMethod | constructor | innerClass)
    /// ```
    /// @param className The name of the class being parsed, used to identify constructors.
    /// @param classLine The line number where the class declaration starts, used for error reporting.
    /// @param classColumn The column number where the class declaration starts, used for error reporting.
    /// @param members The ClassMembers object to which the parsed member will be added.
    private void parseClassMember(String className, int classLine, int classColumn, ClassMembers members) {

        var memberAccessModifier = parseAccessModifier();
        if (memberAccessModifier == null) throw parseError("Class member declaration requires an access modifier", state.peek());

        if (state.match(Keyword.CLASS)) {

            members.innerClasses().add(parseClassDeclaration(memberAccessModifier));
            return;
        }

        if (state.check(new IdentifierLiteral()) && className.equals(state.getLiteralValue(state.peek()))) {

            state.advance();  // consume constructor name
            members.constructors().add(parseConstructor(classLine, classColumn, memberAccessModifier));
            return;
        }

        if (!declarationParser.isValidType(state.peek()))
            throw parseError("Expect type, constructor, inner class, or closing '}' in class body", state.peek());

        var type = declarationParser.parseTypeSyntax();
        var memberNameToken = state.consume(new IdentifierLiteral(), "Expect member name");
        var memberName = state.getLiteralValue(memberNameToken);

        if (state.check(Delimiter.LPAREN)) members.methods().add(parseClassMethod(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
        else members.fields().add(parseClassField(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
    }

    /// Synchronizes after a class-body member error without escaping the current class body.
    private void synchronizeClassMember() {

        if (state.isAtEnd() || currentTokenIs(Delimiter.RBRACE)) return;
        if (currentTokenIs(Delimiter.SEMICOLON)) {

            state.advance();
            return;
        }

        if (isClassMemberRecoveryBoundary(state.peek())) return;
        var braceDepth = 0;
        while (!state.isAtEnd()) {

            if (braceDepth == 0 && currentTokenIs(Delimiter.RBRACE)) return;
            if (braceDepth == 0 && isClassMemberRecoveryBoundary(state.peek())) return;

            var consumedType = state.advance().getType();
            if (consumedType == Delimiter.LBRACE) braceDepth++;
            else if (consumedType == Delimiter.RBRACE && braceDepth > 0) braceDepth--;
            else if (consumedType == Delimiter.SEMICOLON && braceDepth == 0) return;
        }
    }

    /// Determines if the given token is a boundary for recovering from a class member parsing error.
    /// @param token The token to check.
    /// @return `true` if the token is an access modifier (public, private, or protected), indicating a boundary for class member recovery; otherwise, `false`.
    private boolean isClassMemberRecoveryBoundary(Token token) {

        if (token == null) return false;
        var type = token.getType();
        return type instanceof AccessModifier;
    }

    /// Parses a class field declaration, which may optionally include an initializer.
    ///
    /// Grammar rule:
    /// ```
    /// classField → accessModifier type IDENTIFIER ("=" expression)? ";"
    /// ```
    /// @param type The parsed source type syntax of the field.
    /// @param name The name of the field.
    /// @param line The line number where the field declaration appears.
    /// @param column The column number where the field declaration starts.
    /// @param accessModifier The access modifier of the field (e.g., public, private).
    /// @return A ClassFieldDeclaration representing the parsed class field declaration.
    private ClassFieldDeclaration parseClassField(TypeSyntax type, String name, int line, int column, AccessModifier accessModifier) {

        ExpressionNode initializer = null;

        if (state.match(Operator.ASSIGN)) initializer = declarationParser.parseExpression();
        state.consume(Delimiter.SEMICOLON, "Expect ';' after field declaration");
        return new ClassFieldDeclaration(line, column, type, name, initializer, accessModifier);
    }

    /// Parses a class method declaration, including its parameters and body.
    ///
    /// Grammar rule:
    /// ```
    /// classMethod → accessModifier type IDENTIFIER "(" parameters? ")" "{" declaration* "}"
    /// ```
    /// @param returnType The parsed source type syntax of the method.
    /// @param name The name of the method.
    /// @param line The line number where the method declaration appears.
    /// @param column The column number where the method declaration starts.
    /// @param accessModifier The access modifier of the method (e.g., public, private).
    /// @return A ClassMethodDeclaration representing the parsed class method declaration.
    private ClassMethodDeclaration parseClassMethod(TypeSyntax returnType, String name, int line, int column, AccessModifier accessModifier) {

        state.consume(Delimiter.LPAREN, "Expect '(' after method name");

        var parameters = declarationParser.parseParameters();
        state.consume(Delimiter.RPAREN, "Expect ')' after parameters");

        state.consume(Delimiter.LBRACE, "Expect '{' before method body");

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

        state.consume(Delimiter.LPAREN, "Expect '(' after constructor name");

        var parameters = declarationParser.parseParameters();
        state.consume(Delimiter.RPAREN, "Expect ')' after parameters");

        state.consume(Delimiter.LBRACE, "Expect '{' before constructor body");

        var bodyStatements = declarationParser.parseBlockStatements();
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassConstructorDeclaration(line, column, parameters, body, accessModifier);
    }
}
