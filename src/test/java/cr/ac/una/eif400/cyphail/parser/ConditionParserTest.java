package cr.ac.una.eif400.cyphail.parser;

import cr.ac.una.eif400.cyphail.ast.*;
import cr.ac.una.eif400.cyphail.parser.core.Fail;
import cr.ac.una.eif400.cyphail.parser.core.Ok;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

// Condiciones del WHERE (Sprint P2): AND, OR, NOT, parentesis y precedencia NOT > AND > OR.
class ConditionParserTest {

    private static QueryNode parseOrFail(String query) {
        return switch (CyphailParser.parse(query)) {
            case Fail(String reason) -> fail("Query should parse, but failed: " + reason);
            case Ok(QueryNode node, var rest) -> node;
        };
    }

    private static void assertSyntaxError(String query) {
        assertInstanceOf(Fail.class, CyphailParser.parse(query), "Should be a syntax error: " + query);
    }

    // La condicion del WHERE de "MATCH (a) WHERE <condition> RETURN a"
    private static Expression condition(String condition) {
        return parseOrFail("MATCH (a) WHERE " + condition + " RETURN a").where().orElseThrow().condition();
    }

    private static PropertyLookup a(String property) {
        return new PropertyLookup(new VariableExpr("a"), property);
    }

    private static LiteralExpr num(String value) {
        return new LiteralExpr(value);
    }

    private static BinaryExpr cmp(String property, String op, String value) {
        return new BinaryExpr(a(property), op, num(value));
    }

    private static BinaryExpr and(Expression left, Expression right) {
        return new BinaryExpr(left, "AND", right);
    }

    private static BinaryExpr or(Expression left, Expression right) {
        return new BinaryExpr(left, "OR", right);
    }

    @Test
    void singleComparisonStillWorks() {
        assertEquals(cmp("x", ">", "1"), condition("a.x > 1"));
    }

    @Test
    void allComparatorsAreStoredAsTheirSymbol() {
        assertEquals(cmp("x", "=", "1"), condition("a.x = 1"));
        assertEquals(cmp("x", "<>", "1"), condition("a.x <> 1"));
        assertEquals(cmp("x", ">", "1"), condition("a.x > 1"));
        assertEquals(cmp("x", "<", "1"), condition("a.x < 1"));
        assertEquals(cmp("x", ">=", "1"), condition("a.x >= 1"));
        assertEquals(cmp("x", "<=", "1"), condition("a.x <= 1"));
    }

    @Test
    void andBuildsABinaryExprWithUppercaseOperator() {
        assertEquals(and(cmp("x", ">", "1"), cmp("y", "=", "2")), condition("a.x > 1 AND a.y = 2"));
    }

    @Test
    void orBuildsABinaryExprWithUppercaseOperator() {
        assertEquals(or(cmp("x", ">", "1"), cmp("y", "=", "2")), condition("a.x > 1 OR a.y = 2"));
    }

    @Test
    void lowercaseAndMixedCaseKeywordsProduceUppercaseOperators() {
        assertEquals(and(cmp("x", ">", "1"), cmp("y", "=", "2")), condition("a.x > 1 and a.y = 2"));
        assertEquals(or(cmp("x", ">", "1"), cmp("y", "=", "2")), condition("a.x > 1 Or a.y = 2"));
    }

    @Test
    void andBindsTighterThanOr_leftSide() {
        // a AND b OR c  ->  (a AND b) OR c
        assertEquals(or(and(cmp("x", "=", "1"), cmp("y", "=", "2")), cmp("z", "=", "3")),
                condition("a.x = 1 AND a.y = 2 OR a.z = 3"));
    }

    @Test
    void andBindsTighterThanOr_rightSide() {
        // a OR b AND c  ->  a OR (b AND c)
        assertEquals(or(cmp("x", "=", "1"), and(cmp("y", "=", "2"), cmp("z", "=", "3"))),
                condition("a.x = 1 OR a.y = 2 AND a.z = 3"));
    }

    @Test
    void andIsLeftAssociative() {
        assertEquals(and(and(cmp("x", "=", "1"), cmp("y", "=", "2")), cmp("z", "=", "3")),
                condition("a.x = 1 AND a.y = 2 AND a.z = 3"));
    }

    @Test
    void orIsLeftAssociative() {
        assertEquals(or(or(cmp("x", "=", "1"), cmp("y", "=", "2")), cmp("z", "=", "3")),
                condition("a.x = 1 OR a.y = 2 OR a.z = 3"));
    }

    @Test
    void parenthesesOverridePrecedence() {
        // a AND (b OR c)
        assertEquals(and(cmp("x", "=", "1"), or(cmp("y", "=", "2"), cmp("z", "=", "3"))),
                condition("a.x = 1 AND (a.y = 2 OR a.z = 3)"));
    }

    @Test
    void parenthesesAroundASingleComparisonChangeNothing() {
        assertEquals(cmp("x", "=", "1"), condition("(a.x = 1)"));
        assertEquals(cmp("x", "=", "1"), condition("((a.x = 1))"));
    }

    @Test
    void notWrapsAComparison() {
        assertEquals(new NotExpr(cmp("x", "=", "1")), condition("NOT a.x = 1"));
    }

    @Test
    void notCanBeNested() {
        assertEquals(new NotExpr(new NotExpr(cmp("x", "=", "1"))), condition("NOT NOT a.x = 1"));
    }

    @Test
    void notBindsTighterThanAnd() {
        // NOT a AND b  ->  (NOT a) AND b
        assertEquals(and(new NotExpr(cmp("x", "=", "1")), cmp("y", "=", "2")),
                condition("NOT a.x = 1 AND a.y = 2"));
    }

    @Test
    void notAppliesToAParenthesizedCondition() {
        assertEquals(new NotExpr(or(cmp("x", "=", "1"), cmp("y", "=", "2"))),
                condition("NOT (a.x = 1 OR a.y = 2)"));
    }

    @Test
    void notIsCaseInsensitive() {
        assertEquals(new NotExpr(cmp("x", "=", "1")), condition("not a.x = 1"));
    }

    @Test
    void variableNamesThatStartWithAKeywordAreNotKeywords() {
        // "android" empieza con "and", "notes" con "not" y "orb" con "or": son identificadores
        QueryNode query = parseOrFail(
                "MATCH (android)-[:T]->(notes) WHERE android.age > 1 AND notes.x = 2 RETURN orb");
        assertEquals(
                and(new BinaryExpr(new PropertyLookup(new VariableExpr("android"), "age"), ">", num("1")),
                        new BinaryExpr(new PropertyLookup(new VariableExpr("notes"), "x"), "=", num("2"))),
                query.where().orElseThrow().condition());
    }

    @Test
    void comparisonBetweenTwoProperties() {
        assertEquals(new BinaryExpr(a("age"), "=", new PropertyLookup(new VariableExpr("b"), "age")),
                condition("a.age = b.age"));
    }

    @Test
    void decimalLiteralIsASingleToken() {
        assertEquals(cmp("price", ">", "19.99"), condition("a.price > 19.99"));
    }

    @Test
    void trailingAndIsSyntaxError() {
        assertSyntaxError("MATCH (a) WHERE a.x = 1 AND");
    }

    @Test
    void trailingOrIsSyntaxError() {
        assertSyntaxError("MATCH (a) WHERE a.x = 1 OR");
    }

    @Test
    void unclosedParenthesisIsSyntaxError() {
        assertSyntaxError("MATCH (a) WHERE (a.x = 1 RETURN a");
    }

    @Test
    void unopenedParenthesisIsSyntaxError() {
        assertSyntaxError("MATCH (a) WHERE a.x = 1) RETURN a");
    }

    @Test
    void notWithoutOperandIsSyntaxError() {
        assertSyntaxError("MATCH (a) WHERE NOT");
    }

    @Test
    void keywordAfterAndIsNotReadAsAVariable() {
        // "RETURN" no puede ser la variable que le falta a AND
        assertSyntaxError("MATCH (a) WHERE a.x = 1 AND RETURN a");
    }

    @Test
    void reservedWordIsNotAValidVariable() {
        assertSyntaxError("MATCH (a) RETURN where");
        assertSyntaxError("MATCH (a) WHERE set.x = 1 RETURN a");
    }
}
