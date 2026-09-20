package cr.ac.una.eif400.cyphail.ast;

public record BinaryExpr(Expression left, String operator, Expression right) implements Expression { }