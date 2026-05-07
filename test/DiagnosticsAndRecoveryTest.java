import lexer.Lexer;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.parser.util.ParseErrorsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiagnosticsAndRecoveryTest {

    @Test
    void parseErrorIncludesCaretFormatting() {

        var parser = new Parser(new Lexer("badVar;").tokenize());
        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        var msg = ex.getMessage();
        assertTrue(msg.contains("^"), "Diagnostic message should include caret indicator");
        assertTrue(msg.contains("line"), "Diagnostic should include line/column context");
    }

    @Test
    void recoversWithinFunctionBlockAndCollectsMultipleErrors() {

        var source = "int f() { badA; badB; return 0; }";
        var parser = new Parser(new Lexer(source).tokenize());
        var ex = assertThrows(ParseErrorsException.class, parser::parse);
        assertEquals(2, ex.getErrors().size());
    }
}
