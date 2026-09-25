package cr.ac.una.eif400.cyphail.parser.core;

import cr.ac.una.eif400.cyphail.parser.CyphailParser;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;


/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

public class ParsersTest {

    private static final Parser<InputString,String, String> ID =
            Parsers.Map(CyphailLexers.Id(), TokenString::value);

    private static <T> Ok<InputString, T, String> assertOk(Result<InputString, T, String> result) {
        return switch (result){
            case Ok<InputString, T, String> ok -> ok;
            case Fail(String reason) -> fail("Expected OK but got Fail" + reason);};
    }

    @Test
    void sepBy1_readsAllItemsSeparatedByComma() {
        var ok = assertOk(Parsers.SepBy1(ID, CyphailLexers.Comma()).parse(new InputString("a, b, c", 0)));
        assertEquals(List.of("a", "b", "c"), ok.token());
    }

    @Test
    void sepBy1_stopsBeforeDanglingComma() {
        var ok = assertOk(Parsers.SepBy1(ID, CyphailLexers.Comma()).parse(new InputString("a, b,", 0)));
        assertEquals(List.of("a", "b"), ok.token());
        assertEquals(4, ok.rest().index()); // quedo parado justo antes de la ultima coma
    }

    @Test
    void sepBy_acceptsEmptyInput() {
        var ok = assertOk(Parsers.SepBy(ID, CyphailLexers.Comma()).parse(new InputString("", 0)));
        assertTrue(ok.token().isEmpty());
    }

    @Test
    void between_keepsOnlyTheInnerValue() {
        var ok = assertOk(Parsers.Between(CyphailLexers.LParen(), ID, CyphailLexers.RParen())
                .parse(new InputString("( m )", 0)));
        assertEquals("m", ok.token());
    }

    @Test
    void right_discardsTheFirstPart() {
        var ok = assertOk(Parsers.Right(CyphailLexers.Colon(), ID).parse(new InputString(":Movie", 0)));
        assertEquals("Movie", ok.token());
    }
}
