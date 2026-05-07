import backend.BackendPipeline;
import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import semantic.SemanticAnalysisException;
import semantic.SemanticAnalyzer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SemanticAndBackendTest {

    @Test
    void semanticAnalyzerFlagsMissingReturn() {

        var ast = new Parser(new Lexer("int f() { int x = 1; }").tokenize()).parse();
        var analyzer = new SemanticAnalyzer();
        var ex = assertThrows(SemanticAnalysisException.class, () -> analyzer.analyzeOrThrow(ast));
        assertFalse(ex.getDiagnostics().isEmpty());
    }

    @Test
    void backendPipelineProducesPseudoAssembly() {

        var ast = new Parser(new Lexer("int x;").tokenize()).parse();
        var output = new BackendPipeline().compileToPseudoAssembly(ast);
        assertTrue(output.contains("VAR"));
    }
}
