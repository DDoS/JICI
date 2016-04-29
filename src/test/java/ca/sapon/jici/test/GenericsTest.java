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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ca.sapon.jici.SourceException;
import ca.sapon.jici.SourceMetadata;
import ca.sapon.jici.decoder.Decoder;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.type.IntersectionType;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.ParametrizedType;
import ca.sapon.jici.evaluator.type.ReferenceType;
import ca.sapon.jici.evaluator.type.SingleReferenceType;
import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;
import ca.sapon.jici.evaluator.type.WildcardType;
import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.parser.Parser;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.test.GenericsTest.Outer.Inner;
import ca.sapon.jici.test.GenericsTest.Outer.Normal;
import ca.sapon.jici.test.GenericsTest.Outer.Ref;

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
        EvaluatorTest.assertSucceeds(
                "M i; M<String> j = null; i = j;",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "M<String> a; M b = null; a = b;",
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
                type.capture().getName()
        );
        // Y<? extends String, Serializable>
        type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.asList(
                WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(String.class))),
                LiteralReferenceType.of(Serializable.class)
        ));
        Assert.assertEquals(
                "ca.sapon.jici.test.GenericsTest.Y<CAP#1 extends java.lang.String, java.io.Serializable>",
                type.capture().getName()
        );
        // Y<? extends Integer, ? extends String>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Integer.class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(String.class)))
            ));
            type.capture();
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // Y<? extends Integer, ? extends Serializable>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Integer.class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Serializable.class)))
            ));
            type.capture();
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // Y<? extends Integer, ? extends TypeArgument>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Integer.class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(TypeArgument.class)))
            ));
            type.capture();
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // Y<? extends Integer[], ? extends Number[]>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Integer[].class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(Number[].class)))
            ));
            type.capture();
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // Y<? extends float[], ? extends int[]>
        try {
            type = ParametrizedType.of(LiteralReferenceType.of(Y.class), Arrays.<TypeArgument>asList(
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(float[].class))),
                    WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>singleton(LiteralReferenceType.of(int[].class)))
            ));
            type.capture();
            Assert.fail("Expected type error");
        } catch (UnsupportedOperationException ignored) {
        }
        // W<?, String>
        type = ParametrizedType.of(LiteralReferenceType.of(W.class), Arrays.asList(
                WildcardType.of(Collections.<SingleReferenceType>emptySet(), Collections.<SingleReferenceType>emptySet()),
                LiteralReferenceType.THE_STRING
        ));
        Assert.assertEquals(
                "ca.sapon.jici.test.GenericsTest.W<CAP#1 extends java.util.List<java.lang.String>, java.lang.String>",
                type.capture().getName()
        );

        final Environment environment = new Environment();
        environment.importClass(D.class);

        assertType(
                environment,
                "java.util.List<CAP#1 extends java.lang.String>",
                "new D().ti"
        );
        assertType(
                environment,
                "java.util.List<CAP#1 super java.lang.String>",
                "D.ts"
        );
        assertType(
                environment,
                "java.util.List<CAP#1>",
                "new D().getWhat()"
        );
        assertType(
                environment,
                "java.util.List<CAP#1 extends java.lang.Integer>",
                "D.as[0]"
        );
        assertType(
                environment,
                "java.util.List<CAP#1>",
                "(java.util.List<?>) null"
        );
        assertType(
                environment,
                "java.util.List<CAP#1 super java.lang.String>",
                "D.ts = new java.util.ArrayList<Object>()"
        );
    }

    @Test
    public void testSuperTypes() {
        // Parametrized of L
        final Set<LiteralReferenceType> superTypes1 = ParametrizedType.of(L.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_CLONEABLE)).getSuperTypes();
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList(
                ParametrizedType.of(L.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_CLONEABLE)),
                ParametrizedType.of(N.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_STRING)),
                ParametrizedType.of(M.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.THE_STRING)),
                LiteralReferenceType.THE_OBJECT)
        ), superTypes1);
        // Raw of L
        final Set<LiteralReferenceType> superTypes2 = LiteralReferenceType.of(L.class).getSuperTypes();
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList(
                LiteralReferenceType.of(L.class),
                LiteralReferenceType.of(N.class),
                LiteralReferenceType.of(M.class),
                LiteralReferenceType.THE_OBJECT)
        ), superTypes2);
        // Raw of Ks
        final Set<LiteralReferenceType> superTypes3 = LiteralReferenceType.of(Ks.class).getSuperTypes();
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList(
                LiteralReferenceType.of(Ks.class),
                LiteralReferenceType.of(K.class),
                LiteralReferenceType.of(N.class),
                LiteralReferenceType.of(M.class),
                LiteralReferenceType.THE_OBJECT)
        ), superTypes3);
        // Parametrized of N with a wildcard
        final Set<LiteralReferenceType> superTypes4 = ParametrizedType.of(N.class, Collections.<TypeArgument>singletonList(
                WildcardType.of(IntersectionType.EVERYTHING, IntersectionType.of(LiteralReferenceType.THE_STRING))
        )).getSuperTypes();
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList(
                ParametrizedType.of(N.class, Collections.<TypeArgument>singletonList(TypeVariable.of("CAP#1", IntersectionType.EVERYTHING, IntersectionType.of(LiteralReferenceType.THE_STRING)))),
                ParametrizedType.of(M.class, Collections.<TypeArgument>singletonList(TypeVariable.of("CAP#1", IntersectionType.EVERYTHING, IntersectionType.of(LiteralReferenceType.THE_STRING)))),
                LiteralReferenceType.THE_OBJECT)
        ), superTypes4);
    }

    @Test
    public void testGenericNestedTypes() {
        final Environment environment = new Environment();
        environment.importClass(Map.class);
        environment.importClass(Map.Entry.class);
        environment.importClass(Outer.class);
        environment.importClass(Outer.Inner.class);
        environment.importClass(Outer.Normal.class);
        environment.importClass(Outer.Ref.class);
        environment.importClass(Z.class);

        EvaluatorTest.assertFails(
                "Outer<String>.Inner f;",
                environment
        );
        EvaluatorTest.assertFails(
                "Outer.Inner<Integer> g;",
                environment
        );
        EvaluatorTest.assertFails(
                "Inner<Integer> h;",
                environment
        );
        EvaluatorTest.assertFails(
                "Map<String, Integer>.Entry<String, Integer> e;",
                environment
        );
        EvaluatorTest.assertFails(
                "Outer<CharSequence>.Ref<Integer> j;",
                environment
        );

        final ParametrizedType type0 = ParametrizedType.of(
                ParametrizedType.of(Outer.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.of(CharSequence.class))),
                LiteralReferenceType.of(Ref.class),
                Collections.<TypeArgument>singletonList(LiteralReferenceType.of(String.class))
        );
        Assert.assertEquals(
                "ca.sapon.jici.test.GenericsTest.Outer<java.lang.CharSequence>.Ref<java.lang.String>",
                type0.capture().getName()
        );
        Assert.assertTrue(type0.convertibleTo(ParametrizedType.of(
                LiteralReferenceType.of(I.class),
                Collections.<TypeArgument>singletonList(LiteralReferenceType.of(CharSequence.class)))
        ));

        final LiteralType type1 = Parser.parseTypeName(Lexer.lex("Outer<String>.Inner<Integer>")).getType(environment);
        Assert.assertEquals(ParametrizedType.of(
                ParametrizedType.of(Outer.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.of(String.class))),
                Inner.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.of(Integer.class))
        ), type1);

        final LiteralType type2 = Parser.parseTypeName(Lexer.lex("Outer.Inner")).getType(environment);
        Assert.assertEquals(LiteralReferenceType.of(Inner.class), type2);

        final LiteralType type3 = Parser.parseTypeName(Lexer.lex("Map.Entry<String, Integer>")).getType(environment);
        Assert.assertEquals(ParametrizedType.of(
                Map.Entry.class, Arrays.<TypeArgument>asList(LiteralReferenceType.of(String.class), LiteralReferenceType.of(Integer.class))
        ), type3);

        final LiteralType type4 = Parser.parseTypeName(Lexer.lex("Outer.Normal")).getType(environment);
        Assert.assertEquals(LiteralReferenceType.of(Normal.class), type4);

        final LiteralType type5 = Parser.parseTypeName(Lexer.lex("Outer<String>.Normal")).getType(environment);
        Assert.assertEquals(ParametrizedType.of(
                ParametrizedType.of(Outer.class, Collections.<TypeArgument>singletonList(LiteralReferenceType.of(String.class))),
                Normal.class, Collections.<TypeArgument>emptyList()
        ), type5);

        final ParametrizedType type6 = (ParametrizedType) Parser.parseTypeName(Lexer.lex("Outer<? extends String>.Inner<? extends Integer>")).getType(environment);
        Assert.assertEquals(
                "ca.sapon.jici.test.GenericsTest.Outer<CAP#1 extends java.lang.String>" +
                        ".Inner<CAP#2 extends java.lang.Integer>",
                type6.capture().getName()
        );

        final ParametrizedType type7 = (ParametrizedType) Parser.parseTypeName(Lexer.lex("Outer<? extends Comparable<?>>.Ref<? extends java.io.Serializable>")).getType(environment);
        Assert.assertEquals("ca.sapon.jici.test.GenericsTest.Outer<CAP#1 extends java.lang.Comparable<?>>" +
                        ".Ref<CAP#2 extends (CAP#1 extends java.lang.Comparable<?> & java.io.Serializable)>",
                type7.capture().getName()
        );

        final ParametrizedType type8 = (ParametrizedType) Parser.parseTypeName(Lexer.lex("Z<?, String, Integer>")).getType(environment);
        Assert.assertEquals(
                "ca.sapon.jici.test.GenericsTest.Z<CAP#1 extends ca.sapon.jici.test.GenericsTest.Outer<" +
                        "java.lang.String>.Inner<java.lang.Integer>, java.lang.String, java.lang.Integer>",
                type8.capture().getName()
        );
    }

    @Test
    public void testErasure() {
        final Environment environment = new Environment();
        environment.importClass(Outer.class);
        environment.importClass(Outer.Inner.class);
        environment.importClass(Outer.Normal.class);

        final ReferenceType type1 = ((ReferenceType) Parser.parseTypeName(Lexer.lex("Outer")).getType(environment)).getErasure();
        Assert.assertEquals(LiteralReferenceType.of(Outer.class), type1);

        final ReferenceType type2 = ((ReferenceType) Parser.parseTypeName(Lexer.lex("Outer<String>")).getType(environment)).getErasure();
        Assert.assertEquals(LiteralReferenceType.of(Outer.class), type2);

        final ReferenceType type3 = ((ReferenceType) Parser.parseTypeName(Lexer.lex("Outer<String>.Inner<Integer>")).getType(environment)).getErasure();
        Assert.assertEquals(LiteralReferenceType.of(Inner.class), type3);

        final ReferenceType type4 = ((ReferenceType) Parser.parseTypeName(Lexer.lex("Outer<String>.Normal")).getType(environment)).getErasure();
        Assert.assertEquals(LiteralReferenceType.of(Normal.class), type4);
    }

    @Test
    public void testConstructors() {
        final Environment environment = new Environment();
        environment.importClass(M.class);
        environment.importClass(N.class);
        environment.importClass(K.class);
        environment.importClass(Outer.class);
        environment.importClass(Inner.class);
        environment.importClass(U.class);
        environment.importClass(V.class);

        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.M<java.lang.String>",
                "new M<String>()"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.N<java.lang.Integer>",
                "new N<Integer>()"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.M<ca.sapon.jici.test.GenericsTest.N<?>>",
                "new M<N<?>>()"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.U<java.lang.String>",
                "new U<String>(\"Me too thanks\")"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.U<java.lang.String>",
                "new U<String>(\"1\", \"2\", \"3\")"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.U<java.lang.String>",
                "new U<String>(new String[1])"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.U<java.lang.String>",
                "new U<String>()"
        );
        EvaluatorTest.assertFails(
                "new U<String>(2);",
                environment
        );
        EvaluatorTest.assertFails(
                "new M<?>();",
                environment
        );
        EvaluatorTest.assertFails(
                "new M<? extends String>();",
                environment
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.Outer<java.lang.Integer>.Inner<java.lang.String>",
                "new Outer<Integer>().newStringInner()"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.Outer<java.lang.Integer>.Inner<java.lang.Float>",
                "new Outer<Integer>().<Float>newInner()"
        );
        EvaluatorTest.assertFails(
                "new V<String>(\"1\");",
                environment
        );
        EvaluatorTest.assertFails(
                "new <? extends String>V<String>(\"1\");",
                environment
        );
        EvaluatorTest.assertFails(
                "new <Integer>V<String>(\"1\");",
                environment
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.V<java.lang.Integer>",
                "new <String>V<Integer>(\"1\")"
        );
        EvaluatorTest.assertFails(
                "new <Integer>V<CharSequence>(\"1\", new StringBuilder());",
                environment
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.V<java.lang.CharSequence>",
                "new <String>V<CharSequence>(\"1\", new StringBuilder())"
        );
        EvaluatorTest.assertFails(
                "new <Integer>V<CharSequence>((Integer) 1, \"1\", false);",
                environment
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.V<java.lang.Float>",
                "new <Integer, CharSequence>V<Float>((Integer) 1, \"1\", true)"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.V<java.lang.Float>",
                "new <Integer, String>V<Float>((Integer) 1, \"1\", true)"
        );
    }

    @Test
    public void testFields() {
        final Environment environment = new Environment();
        environment.importClass(M.class);
        environment.importClass(N.class);
        environment.importClass(K.class);
        environment.importClass(Outer.class);
        environment.importClass(Inner.class);

        assertType(
                environment,
                "java.lang.Integer",
                "new M<String>().m"
        );
        assertType(
                environment,
                "java.lang.Double",
                "new N<String>().o"
        );
        assertType(
                environment,
                "java.lang.Float",
                "new K().p"
        );
        assertType(
                environment,
                "java.lang.String",
                "new M<String>().t"
        );
        assertType(
                environment,
                "java.lang.Float",
                "new N<Float>().t"
        );
        assertType(
                environment,
                "java.lang.Integer",
                "new K().t"
        );
        assertType(
                environment,
                "CAP#1 extends java.lang.String",
                "M.newWildcardM().t"
        );
        assertType(
                environment,
                "java.lang.Integer",
                "new Outer<Integer>().newStringInner().t"
        );
        assertType(
                environment,
                "java.lang.Integer",
                "new Outer<Integer>().<Float>newInner().t"
        );
    }

    @Test
    public void testMethods() {
        final Environment environment = new Environment();
        environment.importClass(M.class);
        environment.importClass(N.class);
        environment.importClass(K.class);
        environment.importClass(Outer.class);
        environment.importClass(Inner.class);

        assertType(
                environment,
                "java.lang.String",
                "new M<String>().getT()"
        );
        assertType(
                environment,
                "java.lang.Integer",
                "new K().getT()"
        );
        assertType(
                environment,
                "java.lang.Short",
                "new M<String>().getShort()"
        );
        assertType(
                environment,
                "CAP#1 extends java.lang.String",
                "M.newWildcardM().getT()"
        );
        assertType(
                environment,
                "java.lang.Integer",
                "new Outer<Integer>().newStringInner().getT()"
        );
        assertType(
                environment,
                "java.lang.Integer",
                "new Outer<Integer>().<Float>newInner().getT()"
        );
        EvaluatorTest.assertFails(
                "new M<String>().setT();",
                environment
        );
        EvaluatorTest.assertFails(
                "new M<String>().setT(new Integer(1));",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "new M<String>().setT(\"1\");",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "new M<N<String>>().setT(new N());",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "new M<String>().setTs(\"1\", \"2\", \"3\");",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "new M<String>().setTs(new String[1]);",
                environment
        );
        EvaluatorTest.assertFails(
                "new M<String>().setTs(1, 2);",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "new M<String>().setTs();",
                environment
        );
        EvaluatorTest.assertFails(
                "new M<Integer>().setT(\"1\");",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "new M<CharSequence>().setT(\"1\");",
                environment
        );
        EvaluatorTest.assertSucceeds(
                "new K().setT(new Integer(1));",
                environment
        );
        EvaluatorTest.assertFails(
                "new K().setT(\"1\");",
                environment
        );
        assertType(
                environment,
                "java.lang.Integer[]",
                "new M<Integer>().getTs()"
        );
        assertType(
                environment,
                "java.lang.Integer[][]",
                "new M<Integer>().getTts()"
        );
        assertType(
                environment,
                "CAP#1[] extends java.lang.String[]",
                "M.newWildcardM().getTs()"
        );
        assertType(
                environment,
                "CAP#1[][] extends java.lang.String[][]",
                "M.newWildcardM().getTts()"
        );
        EvaluatorTest.assertFails(
                "new M<Integer>().getS();",
                environment
        );
        assertType(
                environment,
                "java.lang.String",
                "new M<Integer>().<String>getS(1)"
        );
        assertType(
                environment,
                "java.lang.String",
                "new M<CharSequence>().<String>getS2()"
        );
        EvaluatorTest.assertFails(
                "new M<CharSequence>().<Integer>getS2()",
                environment
        );
        EvaluatorTest.assertFails(
                "new M<CharSequence>().<? extends String>getS2()",
                environment
        );
        assertType(
                environment,
                "java.lang.String",
                "new M<Integer>().<String>getS3(\"1\")"
        );
        EvaluatorTest.assertFails(
                "new M<Integer>().<String>getS3((Integer) 1)",
                environment
        );
        assertType(
                environment,
                "java.lang.String",
                "new M<Integer>().<String>getT2(\"1\")"
        );
        assertType(
                environment,
                "ca.sapon.jici.test.GenericsTest.M<CAP#1 extends java.lang.String>",
                "M.newWildcardM()"
        );
    }

    @Test
    public void testRawTypeMembers() {
        final Environment environment = new Environment();
        environment.importClass(M.class);
        environment.importClass(L.class);
        environment.importClass(Ks.class);
        environment.importClass(Kr.class);

        assertType(
                environment,
                "java.util.List",
                "new M().d(null)"
        );
        assertType(
                environment,
                "java.util.List<java.lang.String>",
                "new M<Integer>().d(null)"
        );
        assertType(
                environment,
                "java.util.List",
                "new M<Integer>().d(new java.util.ArrayList())"
        );
        assertType(
                environment,
                "java.util.List<java.lang.String>",
                "new M<Integer>().d(new java.util.ArrayList<String>())"
        );
        assertType(
                environment,
                "java.util.List<java.lang.String>",
                "new M<Integer>().li"
        );
        assertType(
                environment,
                "java.util.List",
                "new M().li"
        );
        assertType(
                environment,
                "java.util.List<java.lang.String>",
                "new M<Integer>().ls"
        );
        assertType(
                environment,
                "java.util.List<java.lang.String>",
                "new M().ls"
        );
        assertType(
                environment,
                "java.util.List<java.lang.String>",
                "new M<Integer>().<String>ds()"
        );
        assertType(
                environment,
                "java.util.List",
                "new M().ds()"
        );
        EvaluatorTest.assertFails(
                "new M().<String>ds()",
                environment
        );
        assertType(
                environment,
                "java.lang.Object",
                "new L().t"
        );
        assertType(
                environment,
                "java.lang.Object",
                "new Ks().t"
        );
        assertType(
                environment,
                "java.lang.String",
                "new Kr<String>().r"
        );
        assertType(
                environment,
                "java.lang.Object",
                "new Kr<String>().t"
        );
        EvaluatorTest.assertFails(
                "new <String>M(null)",
                environment
        );
        assertType(
                environment,
                "java.util.List",
                "new M<Integer>().setM(new Kr<Integer>())"
        );
    }

    public static class M<T> {
        public static Short s = null;
        public static List<String> ls = null;
        public T t = null;
        public Integer m = null;
        public List<String> li = null;

        public M() {
        }

        public <S> M(List<S> s) {
        }

        public T getT() {
            return t;
        }

        public void setT(T t) {
            this.t = t;
        }

        public void setTL(List<T> t) {
        }

        @SafeVarargs
        public final void setTs(T... t) {
        }

        public T[] getTs() {
            return null;
        }

        public T[][] getTts() {
            return null;
        }

        public <S> S getS(T t) {
            return null;
        }

        public <S extends T> S getS2() {
            return null;
        }

        public <S> S getS3(S s) {
            return s;
        }

        public <T> T getT2(T t) {
            return t;
        }

        public List<String> d(List<String> l) {
            return l;
        }

        public <S> List<S> ds() {
            return null;
        }

        public List<String> setM(M<T> m) {
            return null;
        }

        public static Short getShort() {
            return s;
        }

        public static M<? extends String> newWildcardM() {
            return new M<>();
        }
    }

    public static class N<T> extends M<T> {
        public Double o = null;
    }

    public static class L<T> extends N<String> {
    }

    public static class K extends N<Integer> {
        public Float p = null;
    }

    public static class Ks<T> extends K {
    }

    @SuppressWarnings("rawtypes")
    public static class Kr<T> extends M {
        public T r = null;
    }

    public static class Q<T> extends M<M<T>> {
    }

    interface I<A> {
    }

    public static class J<A> implements I<A> {
    }

    public static final class X implements I<String> {
    }

    public static class Y<T extends S, S> {
    }

    public static class W<T extends List<S>, S> {
    }

    public static class Outer<T> {
        public class Inner<S> {
            public T t = null;

            public T getT() {
                return t;
            }
        }

        public class Normal {
        }

        public class Ref<R extends T> implements I<T> {
        }

        public <S> Inner<S> newInner() {
            return new Inner<>();
        }

        public Inner<String> newStringInner() {
            return new Inner<>();
        }
    }

    public static class Z<T extends Outer<S>.Inner<R>, S, R> {
    }

    public static class D {
        public static List<? super String> ts = null;
        @SuppressWarnings({"rawtypes", "unchecked"})
        public static List<? extends Integer>[] as = new List[]{null};
        public List<? extends String> ti = null;

        public List<?> getWhat() {
            return null;
        }
    }

    public static class U<T> {
        public T t;

        public U(T t) {
            this.t = t;
        }

        @SafeVarargs
        public U(T... t) {
            this.t = null;
        }
    }

    public static class V<T> {
        public <S> V(S s) {
        }

        public <S extends T> V(S s, T t) {
        }

        public <S, T> V(S s, T t, boolean b) {
        }
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
        assertType(environment, expectedType, source);
    }

    private static void assertType(Environment environment, String expectedType, String source) {
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
