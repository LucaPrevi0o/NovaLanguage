package semantic;

import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.ReturnStatement;
import parser.ast.nodes.statement.declaration.FunctionDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import parser.parser.util.Diagnostic;
import parser.parser.util.DiagnosticCode;
import parser.parser.util.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/// Minimal semantic-analysis pipeline stage.
/// Current checks:
/// - non-void functions/methods should contain at least one return statement;
/// - statements after an unconditional return inside the same block are flagged unreachable.
public class SemanticAnalyzer {

    public void analyzeOrThrow(List<StatementNode> statements) {

        var diagnostics = analyze(statements);
        if (!diagnostics.isEmpty()) throw new SemanticAnalysisException(diagnostics);
    }

    public List<Diagnostic> analyze(List<StatementNode> statements) {

        var diagnostics = new ArrayList<Diagnostic>();
        for (var statement : statements) analyzeStatement(statement, diagnostics);
        return diagnostics;
    }

    private void analyzeStatement(StatementNode statement, List<Diagnostic> diagnostics) {

        if (statement instanceof FunctionDeclarationStatement function) {
            checkFunctionReturns(function, diagnostics);
            if (function.getBody() instanceof BlockStatement block) checkUnreachableInBlock(block, diagnostics);
            return;
        }

        if (statement instanceof ClassDeclarationStatement clazz) {
            for (ClassMethodDeclaration method : clazz.getMethods()) {
                checkFunctionReturns(method, diagnostics);
                if (method.getBody() instanceof BlockStatement block) checkUnreachableInBlock(block, diagnostics);
            }
            return;
        }

        if (statement instanceof BlockStatement block) {
            checkUnreachableInBlock(block, diagnostics);
            for (var child : block.getStatements()) analyzeStatement(child, diagnostics);
        }
    }

    private static void checkFunctionReturns(FunctionDeclarationStatement function, List<Diagnostic> diagnostics) {

        var returnType = function.getDeclaredType();
        if ("void".equals(returnType.getBaseType().get())) return;
        if (containsReturn(function.getBody())) return;

        diagnostics.add(new Diagnostic(
            DiagnosticCode.SEMANTIC_ERROR,
            "Function '" + function.getName() + "' may not return a value on all paths",
            new SourceSpan(function.getLine(), function.getColumn(), function.getColumn()),
            function.getName()
        ));
    }

    private static boolean containsReturn(StatementNode node) {

        switch (node) {
            case null -> { return false; }
            case ReturnStatement _ -> { return true; }
            case BlockStatement block -> {
                for (var statement : block.getStatements()) if (containsReturn(statement)) return true;
            }
            default -> {}
        }
        return false;
    }

    private static void checkUnreachableInBlock(BlockStatement block, List<Diagnostic> diagnostics) {

        var sawReturn = false;
        for (var statement : block.getStatements()) {

            if (sawReturn) diagnostics.add(new Diagnostic(
                DiagnosticCode.SEMANTIC_ERROR,
                "Unreachable statement after return",
                new SourceSpan(statement.getLine(), statement.getColumn(), statement.getColumn()),
                statement.getClass().getSimpleName()
            ));
            if (statement instanceof ReturnStatement) sawReturn = true;
            if (statement instanceof BlockStatement inner) checkUnreachableInBlock(inner, diagnostics);
        }
    }
}
