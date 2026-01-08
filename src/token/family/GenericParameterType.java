package src.token.family;

import src.token.TokenFamily;

public class GenericParameterType implements TokenFamily {

    private final String parameterName;

    public GenericParameterType(String parameterName) { this.parameterName = parameterName; }

    @Override
    public String get() { return parameterName; }
}
