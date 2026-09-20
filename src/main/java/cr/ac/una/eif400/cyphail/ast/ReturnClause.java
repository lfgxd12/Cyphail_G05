package cr.ac.una.eif400.cyphail.ast;

import java.util.List;

public record ReturnClause(List<ReturnItem> items) implements Clause { }
