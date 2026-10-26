package cr.ac.una.eif400.cyphail.ast;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Un literal tal como se escribio: con comillas es un string ("adult", 'Marta') y sin ellas es un numero (19.99).
// Se guarda como texto; isString() e isNumber() le dicen al Codegen de que clase es.
public record LiteralExpr(String value) implements Expression {

    public boolean isString() {
        return value.startsWith("\"") || value.startsWith("'");
    }

    public boolean isNumber() {
        return !isString();
    }

    // El texto sin las comillas de los extremos: "adult" -> adult. Un numero se devuelve igual.
    public String text() {
        return isString() ? value.substring(1, value.length() - 1) : value;
    }
}