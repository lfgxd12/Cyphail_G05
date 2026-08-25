package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.frontend.ReplCommand;
import cr.ac.una.eif400.cyphail.model.Author;
import java.util.List;

public class AboutHandler implements ReplCommand {
    private final List<Author> authors = List.of(
            new Author("Estudiante 1", "ID-12345", "G05"),
            new Author("Estudiante 2", "ID-67890", "G05")
            // Completen con los nombres y carnés de los integrantes del grupo
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