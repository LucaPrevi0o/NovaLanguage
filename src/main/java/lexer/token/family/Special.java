package lexer.token.family;

import lexer.token.TokenClass;

/// Represents special tokens in the programming language, such as EOF (end of file) and UNKNOWN (unrecognized token).
public enum Special implements TokenClass {

    /// The EOF token, representing the end of the input file or stream.
    EOF,

    /// The UNKNOWN token, representing an unrecognized or invalid token that does not fit into any other token family.
    UNKNOWN;

    @Override
    public String token() { return this.name(); }
}
