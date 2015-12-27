/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2015 Aleksi Sapon <http://sapon.ca/jici/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ca.sapon.jici.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ca.sapon.jici.SourceException;
import ca.sapon.jici.SourceMetadata;
import ca.sapon.jici.decoder.Decoder;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.parser.Parser;
import ca.sapon.jici.parser.expression.Expression;

/**
 *
 */
public class GenericsTest {
    @Test
    public void testWildcards() {
        // Subtype assignable to upper-bound
        assertAssignSucceeds(
                "List<String>",
                "List<? extends CharSequence>"
        );
        assertLUB(
                "java.util.List<? extends java.lang.CharSequence>",
                "List<String>",
                "List<? extends CharSequence>"
        );
        assertAssignFails(
                "List<CharSequence>",
                "List<? extends String>"
        );
        // Super type assignable to lower-bound
        assertAssignSucceeds(
                "List<CharSequence>",
                "List<? super String>"
        );
        assertLUB(
                "java.util.List<? super java.lang.String>",
                "List<CharSequence>",
                "List<? super String>"
        );
        assertAssignFails(
                "List<String>",
                "List<? super CharSequence>"
        );
        // Unbounded assignable to unbounded
        assertAssignSucceeds(
                "List<?>",
                "List<?>"
        );
        assertLUB(
                "java.util.List<?>",
                "List<?>",
                "List<?>"
        );
        // Upper-bounded assignable to higher upper-bounded
        assertAssignSucceeds(
                "List<? extends String>",
                "List<? extends CharSequence>"
        );
        assertLUB(
                "java.util.List<? extends java.lang.CharSequence>",
                "List<? extends String>",
                "List<? extends CharSequence>"
        );
        assertAssignFails(
                "List<? extends CharSequence>",
                "List<? extends String>"
        );
        // Lower-bounded assignable to lower lower-bounded
        assertAssignSucceeds(
                "List<? super CharSequence>",
                "List<? super String>"
        );
        assertLUB(
                "java.util.List<? super java.lang.String>",
                "List<? super CharSequence>",
                "List<? super String>"
        );
        assertAssignFails(
                "List<? super String>",
                "List<? super CharSequence>"
        );
        // Similar as a above but with bounded and unbounded
        assertAssignSucceeds(
                "List<? extends String>",
                "List<?>"
        );
        assertLUB(
                "java.util.List<?>",
                "List<? extends String>",
                "List<?>"
        );
        assertAssignSucceeds(
                "List<? super String>",
                "List<?>"
        );
        assertLUB(
                "java.util.List<?>",
                "List<? super String>",
                "List<?>"
        );
        assertAssignFails(
                "List<?>",
                "List<? extends String>"
        );
        assertAssignFails(
                "List<?>",
                "List<? super String>"
        );
    }

    @Test
    public void testParametrizedConversions() {
        final Environment environment = new Environment();
        environment.importClass(M.class);
        environment.importClass(L.class);
        environment.importClass(K.class);
        EvaluatorTest.assertSucceeds(
                "M<String> m; L<Integer> l = null; m = l;",
                environment
        );
        EvaluatorTest.assertFails(
                "M<String> n; K k = null; n = k;",
                environment
        );
    }

    public static class M<T> {
    }

    public static class N<T> extends M<T> {
    }

    public static class L<T> extends N<String> {
    }

    public static class K extends N<Integer> {
    }

    @Test
    public void testCasts() {
        final Environment environment = new Environment();
        environment.importClass(S.class);
        environment.importClass(J.class);
        environment.importClass(X.class);
        EvaluatorTest.assertSucceeds(
                "J<String> j = null; Object o = (S<String>) j;",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "S<String> s = null; Object w = (X) s;",
                environment
        );
        EvaluatorTest.assertFails(
                "S<Integer> t = null; Object l = (X) t;",
                environment
        );
    }

    interface S<A> {
    }

    public static class J<A> implements S<A> {
    }

    public static final class X implements S<String> {
    }

    private static Environment assertSucceeds(String source) {
        final Environment environment = new Environment();
        return EvaluatorTest.assertSucceeds(source, environment);
    }

    private static Environment assertFails(String source) {
        final Environment environment = new Environment();
        return EvaluatorTest.assertFails(source, environment);
    }

    private static void assertAssignSucceeds(String leftType, String rightType) {
        assertSucceeds(generateDeclarationSource(leftType, rightType) + "l2 = l1;");
    }

    private static void assertAssignFails(String leftType, String rightType) {
        assertFails(generateDeclarationSource(leftType, rightType) + "l2 = l1;");
    }

    private static void assertLUB(String expectedType, String leftType, String rightType) {
        final Environment environment = assertSucceeds(generateDeclarationSource(leftType, rightType));
        String source = "true ? l1 : l2";
        final SourceMetadata metadata = new SourceMetadata(source);
        try {
            source = Decoder.decode(source, metadata);
            final List<Token> tokens = Lexer.lex(source);
            final Expression expression = Parser.parseExpression(tokens);
            Assert.assertEquals(expectedType, expression.getType(environment).toString());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
    }

    private static String generateDeclarationSource(String leftType, String rightType) {
        return "import java.util.List;"
                + leftType + " l1 = null;"
                + rightType + " l2;";
    }
}
