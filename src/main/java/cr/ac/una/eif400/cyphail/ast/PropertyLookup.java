package cr.ac.una.eif400.cyphail.ast;

public record PropertyLookup(VariableExpr variable, String property) implements Expression { }