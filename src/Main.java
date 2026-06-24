import lexer.Lexer;
import parser.Parser;
import parser.ast.Printable;
import error.diagnostic.ParseErrorsException;
import printer.AstPrinter;
import printer.SymbolTablePrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {

        String sourceCode;
        if (args.length < 1) {

            System.out.println("Usage: java Main <source-file-path>");
            System.exit(1);
        }

        var filePath = args[0];
        try {

            sourceCode = readFile(filePath);
            System.out.println("=== Reading from file: " + filePath + " ===\n");
        } catch (IOException e) {

            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        var lexer = new Lexer(sourceCode);
        var tokens = lexer.tokenize();

        System.out.println("=== TOKENS ===\n");
        for (var token : tokens) System.out.println(token);

        System.out.println("\n=== PARSING ===\n");
        try {

            var parser = new Parser(tokens);
            var ast = parser.parse();
            System.out.println("Parsing completed successfully!");
            System.out.println("Total AST nodes: " + ast.size());

            System.out.println("\n=== AST STRUCTURE ===\n");
            for (var node : ast) AstPrinter.printASTNode((Printable) node, new ArrayList<>(), !node.equals(ast.getLast()) ? "├─ " : "└─ ");

            System.out.println("\n=== SYMBOL TABLE ===\n");
            SymbolTablePrinter.printSymbolTableNode(parser.getSymbolTable());

        } catch (ParseErrorsException e) {

            System.out.println("Parsing failed with " + e.getDiagnostics().size() + " error(s).\n");
            System.out.println("=== PARSING ERRORS ===\n");
            for (var diagnostic : e.getDiagnostics()) System.out.println("  " + diagnostic.format());

        } catch (Exception e) {

            System.err.println("\n=== PARSING ERROR ===");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String readFile(String filePath) throws IOException { return Files.readString(Paths.get(filePath)); }
}
