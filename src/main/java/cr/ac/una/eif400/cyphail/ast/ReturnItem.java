package cr.ac.una.eif400.cyphail.ast;

import java.util.Optional;

public record ReturnItem(Expression expression, Optional<String> alias) {
}
