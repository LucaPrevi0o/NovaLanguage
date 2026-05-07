package backend.lowering;

import backend.ir.IRInstruction;
import backend.ir.IRProgram;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.VariableDeclarationStatement;

import java.util.List;

/// Baseline AST -> IR lowering scaffold.
public class AstLowerer {

    public IRProgram lower(List<StatementNode> statements) {

        var program = new IRProgram();
        for (var statement : statements) lowerStatement(statement, program);
        return program;
    }

    private static void lowerStatement(StatementNode statement, IRProgram program) {

        switch (statement) {
            case ClassDeclarationStatement cls -> program.add(new IRInstruction("CLASS", new String[]{cls.getName()}));
            case FunctionDeclarationStatement fn -> program.add(new IRInstruction("FUNC", new String[]{fn.getName()}));
            case VariableDeclarationStatement var -> program.add(new IRInstruction("VAR", new String[]{var.getName(), var.getDeclaredType().getBaseType().get()}));
            default -> program.add(new IRInstruction("STMT", new String[]{statement.getClass().getSimpleName()}));
        }
    }
}
