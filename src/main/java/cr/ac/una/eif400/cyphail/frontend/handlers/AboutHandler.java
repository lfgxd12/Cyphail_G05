package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.frontend.ReplCommand;
import cr.ac.una.eif400.cyphail.model.Author;
import java.util.List;

/**
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 *          Jostin Jimenez Alfaro, Angel Rojas Ruano
 */
public class AboutHandler implements ReplCommand {
    private final List<Author> authors = List.of(
            new Author("Luis Felipe Jimenez Fernandez", "119130110", "G05"),
            new Author("Jose David Chavarria Villalobos", "402710170", "G05"),
            new Author("Jostin Jimenez Alfaro", "119620942", "G05"),
            new Author("Angel Rojas Ruano", "118780534", "G05")
    );

    @Override
    public void execute(String args) {
        System.out.println("Cyphail Engine v0.1 - EIF400-II-2026 ESCINF/UNA");
        System.out.println("Authors:");
        for (Author author : authors) {
            System.out.printf(" - %s (ID: %s, Group: %s)%n", author.name(), author.id(), author.group());
        }
    }
}
