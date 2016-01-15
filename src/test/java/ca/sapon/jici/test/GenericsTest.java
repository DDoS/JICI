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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.sapon.jici.SourceException;
import ca.sapon.jici.SourceMetadata;
import ca.sapon.jici.decoder.Decoder;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.evaluator.type.SingleReferenceType;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.WildcardType;
import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.parser.Parser;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.util.IntegerCounter;
import ca.sapon.jici.util.TypeUtil;
import org.junit.Assert;
import org.junit.Test;

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
                "M<M<? extends CharSequence>> l1; M<M<? extends String>> l2 = null; l1 = l2;",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "M<? extends M<? extends CharSequence>> l3; M<M<? extends String>> l4 = null; l3 = l4;",
                environment
        );
        EvaluatorTest.assertFails(
                "M<String> n; K k = null; n = k;",
                environment
        );
    }

    @Test
    public void testCasts() {
        final Environment environment = new Environment();
        environment.importClass(I.class);
        environment.importClass(J.class);
        environment.importClass(X.class);
        environment.importClass(N.class);
        environment.importClass(M.class);
        EvaluatorTest.assertSucceeds(
                "J<String> i1 = null; Object o1 = (I<String>) i1;",
                environment
        );
        EvaluatorTest.assertFails(
                "N<Integer> i2 = null; Object o2 = (N<String>) i2;",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "J<String> i3 = null; Object o3 = (X) i3;",
                environment
        );
        EvaluatorTest.assertFails(
                "J<Integer> i4 = null; Object o4 = (X) i4;",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "M<? extends CharSequence> i5 = null; Object o5 = (M<? extends String>) i5;",
                environment
        );
        EvaluatorTest.assertFails(
                "M<CharSequence> i6 = null; Object o6 = (M<Number>) i6;",
                environment
        );
        EvaluatorTest.assertFails(
                "M<? extends CharSequence> i7 = null; Object o7 = (M<Number>) i7;",
                environment
        );
        EvaluatorTest.assertFails(
                "M<CharSequence> i8 = null; Object o8 = (M<? extends Number>) i8;",
                environment
        );
        EvaluatorTest.assertFails(
                "M<? extends CharSequence> i9 = null; Object o9 = (M<? extends Number>) i9;",
                environment
        );
    }

    @Test
    public void testTypeArgumentSubstitution() {
        Assert.assertEquals(
                Collections.singleton(
                        ParametrizedType.of(M.class,
                                Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_STRING)
                        )
                ),
                ParametrizedType.of(N.class,
                        Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_STRING))
                        .getDirectSuperTypes()
        );
        Assert.assertEquals(
                Collections.singleton(
                        ParametrizedType.of(M.class,
                                Collections.<TypeArgument>singletonList(ParametrizedType.of(M.class,
                                        Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_STRING)
                                ))
                        )
                ),
                ParametrizedType.of(Q.class,
                        Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_STRING))
                        .getDirectSuperTypes()
        );
    }

    @Test
    public void testCaptureConversion() {
        ParametrizedType type;
        // Y<? extends Serializable, ? extends Comparable>
        type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Serializable.class))),
                WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Comparable.class)))
        ));
        Assert.assertEquals(
                "ca.sapon.jici.test.GenericsTest.Y<CAP#2 extends (CAP#1 extends java.lang.Comparable & java.io.Serializable), CAP#1 extends java.lang.Comparable>",
                type.capture(new IntegerCounter()).getName()
        );
        // Y<? extends String, Serializable>
        type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.asList(
                WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(String.class))),
                LiteralReferenceType.of(Serializable.class)
        ));
        Assert.assertEquals(
                "ca.sapon.jici.test.GenericsTest.Y<CAP#1 extends java.lang.String, java.io.Serializable>",
                type.capture(new IntegerCounter()).getName()
        );
        // Y<? extends Integer, ? extends String>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Integer.class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(String.class)))
            ));
            type.capture(new IntegerCounter());
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // Y<? extends Integer, ? extends Serializable>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Integer.class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Serializable.class)))
            ));
            type.capture(new IntegerCounter());
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // Y<? extends Integer, ? extends TypeArgument>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Integer.class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(TypeArgument.class)))
            ));
            type.capture(new IntegerCounter());
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // Y<? extends Integer[], ? extends Number[]>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Integer[].class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Number[].class)))
            ));
            type.capture(new IntegerCounter());
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // Y<? extends float[], ? extends int[]> l
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(float[].class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(int[].class)))
            ));
            type.capture(new IntegerCounter());
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testSuperTypes() {
        // Parametrized of L
        final Set<LiteralReferenceType> superTypes1 = TypeUtil.getSuperTypes(ParametrizedType.of(L.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_CLONEABLE)));
        Assert.assertEquals(new HashSet<>(Arrays.asList(
                ParametrizedType.of(L.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_CLONEABLE)),
                ParametrizedType.of(N.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_STRING)),
                ParametrizedType.of(M.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_STRING)),
                LiteralReferenceType.THE_OBJECT)
        ), superTypes1);
        // Raw of L
        final Set<LiteralReferenceType> superTypes2 = TypeUtil.getSuperTypes(LiteralReferenceType.of(L.class));
        Assert.assertEquals(new HashSet<>(Arrays.asList(
                LiteralReferenceType.of(L.class),
                LiteralReferenceType.of(N.class),
                LiteralReferenceType.of(M.class),
                LiteralReferenceType.THE_OBJECT)
        ), superTypes2);
    }

    public static class M<T> {
    }

    public static class N<T> extends M<T> {
    }

    public static class L<T> extends N<String> {
    }

    public static class K extends N<Integer> {
    }

    public class Q<T> extends M<M<T>> {
    }

    interface I<A> {
    }

    public static class J<A> implements I<A> {
    }

    public static final class X implements I<String> {
    }

    public class Y<T extends S, S> {
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
