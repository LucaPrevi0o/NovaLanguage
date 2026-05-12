package error.parsing;

import error.Error;

@ParsingError
public class DuplicateSymbolError implements Error {

    private final String name;
    private final int line;
    private final int column;

    public DuplicateSymbolError(String name, int line, int column) {

        this.name = name;
        this.line = line;
        this.column = column;
    }

    @Override
    public String getMessage() {
        return String.format("[line %d, col %d] Duplicate symbol declaration: '%s'", line, column, name);
    }
}
