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

    private static final List<ReturnType> types = new ArrayList<>();

    static { for (var primitive : PrimitiveType.values()) types.add(new ReturnType(primitive)); }
    
    public static void registerClass(ClassDeclarationStatement classDecl) { types.add(new ReturnType(new NonPrimitiveType(classDecl))); }

    public static ReturnType getReturnType(String typeName) { return types.stream().filter(t -> t.getBaseType().get().equals(typeName)).findFirst().orElse(null); }

    public static boolean isType(String typeName) { return types.stream().anyMatch(t -> t.getBaseType().get().equals(typeName)); }

    public static ReturnType getClassDeclaration(String className) {

        for (var type : types) if (type.getBaseType() instanceof NonPrimitiveType npt)
            if (npt.getClassDeclaration().getName().equals(className)) return type;
        return null;
    }

    public static boolean isCustomClass(String typeName) { return getClassDeclaration(typeName) != null; }
}
