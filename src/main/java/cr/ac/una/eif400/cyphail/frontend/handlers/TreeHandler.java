package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.frontend.ReplCommand;
import cr.ac.una.eif400.cyphail.output.TreePrinter;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import cr.ac.una.eif400.cyphail.validation.VariableScopeChecker;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class TreeHandler implements ReplCommand {

    @Override
    public void execute(String args) {
        if (args.isBlank()) {
            System.out.println("Usage: .tree <query>");
            return;
        }

        switch (CyphailParser.parse(args)) {
            case Fail(String reason) -> System.out.println("ERROR: Syntax error. " + reason);
            case Ok(QueryNode query, var rest) -> {
                System.out.println(TreePrinter.print(query));
                VariableScopeChecker.check(query).forEach(error -> System.out.println("ERROR: " + error));
            }
        }
    }
}
