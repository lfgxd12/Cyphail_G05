package cr.ac.una.eif400.cyphail.ast;

import java.util.List;

public record RemoveClause(List<PropertyLookup> items) implements Clause { }
