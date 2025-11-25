package src.token.family;

import src.token.TokenFamily;

public enum Special implements TokenFamily {
    
    EOF,
    UNKNOWN;

    public String get() { return this.name(); }
}
