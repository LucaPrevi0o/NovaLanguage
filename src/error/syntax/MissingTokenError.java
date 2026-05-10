package error.syntax;

import error.Error;

@SyntaxError
public class MissingTokenError implements Error {

    public MissingTokenError() {
    }

    @Override
    public String getMessage() { return "Missing Token"; }
}
