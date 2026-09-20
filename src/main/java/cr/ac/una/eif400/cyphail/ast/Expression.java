package cr.ac.una.eif400.cyphail.ast;

public sealed interface Expression permits BinaryExpr, PropertyLookup, VariableExpr, LiteralExpr {
}