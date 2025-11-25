package src.token;

import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.token.family.NonPrimitiveType;
import src.token.family.PrimitiveType;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry for all types (primitive and custom).
 * Single source of truth for type resolution.
 * 
 * Uses a unified List<TokenFamily> containing:
 * - PrimitiveType enum instances (pre-populated)
 * - NonPrimitiveType instances (added dynamically during parsing)
 */
public class TypeRegistry {

    private static final List<TokenFamily> types = new ArrayList<>();

    static { for (PrimitiveType primitive : PrimitiveType.values()) types.add(primitive); }
    
    public static void registerClass(ClassDeclarationStatement classDecl) { types.add(new NonPrimitiveType(classDecl)); }

    public static TokenFamily getTokenFamilyByName(String typeName) { return types.stream().filter(t -> t.get().equals(typeName)).findFirst().orElse(null); }

    public static boolean isType(String typeName) { return types.stream().anyMatch(t -> t.get().equals(typeName)); }

    public static ClassDeclarationStatement getClassDeclaration(String className) {

        return types.stream()
            .filter(NonPrimitiveType.class :: isInstance)
            .map(NonPrimitiveType.class :: cast)
            .map(NonPrimitiveType :: getClassDeclaration)
            .filter(decl -> decl.getName().equals(className))
            .findFirst()
            .orElse(null);
    }

    public static boolean isCustomClass(String typeName) { return getClassDeclaration(typeName) != null; }
}
