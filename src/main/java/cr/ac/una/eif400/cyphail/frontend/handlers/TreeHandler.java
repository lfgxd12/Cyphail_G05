package cr.ac.una.eif400.cyphail.frontend.handlers;

import cr.ac.una.eif400.cyphail.ast.QueryNode;
import cr.ac.una.eif400.cyphail.frontend.ReplCommand;
import cr.ac.una.eif400.cyphail.output.TreePrinter;
import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;

public class TreeHandler implements ReplCommand {

    @Override
    public void execute(String args) {
        if (args.isBlank()) {
            System.out.println("Usage: .tree <query>");
            return;
        }

        switch (CyphailParser.parse(args)) {
            case Fail(String reason) ->
                    System.out.println("ERROR: " + reason);
            case Ok(QueryNode node, var rest) -> {
                if (rest.index() < rest.input().length()) {
                    System.out.println("ERROR: Unexpected trailing input: \""
                            + rest.input().substring(rest.index()).trim() + "\"");
                    return;
                }
                System.out.println("S-Expression:");
                System.out.println("  " + TreePrinter.toSExpr(node));
                System.out.println("JSON:");
                System.out.println("  " + TreePrinter.toJson(node));
            }
        }
    }
}
