package cr.ac.una.eif400.cyphail.broker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public record PrologCompound(String functor, List<Object> args) {
    public PrologCompound {
        args = Collections.unmodifiableList(new ArrayList<>(args));
    }
}