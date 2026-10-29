package cr.ac.una.eif400.cyphail.broker;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Lo unico que el REPL necesita del engine: ejecutar un goal Prolog y recibir el texto Rows.
// EngineBroker es la implementacion real; en las pruebas del REPL se usa una falsa.
public interface GoalRunner extends AutoCloseable {

    // Lanza EngineException si el engine falla
    String run(String goal);

    @Override
    default void close() {
    }
}
