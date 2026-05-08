package lexer.token.family;

import lexer.token.TokenFamily;

/// Represents special tokens in the programming language, such as EOF (end of file) and UNKNOWN (unrecognized token).
public enum Special implements TokenFamily {

    /// The EOF token, representing the end of the input file or stream.
    EOF,

    /// The UNKNOWN token, representing an unrecognized or invalid token that does not fit into any other token family.
    UNKNOWN;

    @Override
    public String get() { return this.name(); }
}
