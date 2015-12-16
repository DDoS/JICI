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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.sapon.jici.SourceException;
import ca.sapon.jici.SourceMetadata;
import ca.sapon.jici.decoder.Decoder;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.type.ReferenceIntersectionType;
import ca.sapon.jici.evaluator.type.LiteralType;
import ca.sapon.jici.evaluator.type.NullType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.SingleReferenceType;
import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.type.VoidType;
import ca.sapon.jici.evaluator.value.BooleanValue;
import ca.sapon.jici.evaluator.value.DoubleValue;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.ObjectValue;
import ca.sapon.jici.evaluator.value.ShortValue;
import ca.sapon.jici.evaluator.value.VoidValue;
import ca.sapon.jici.lexer.Identifier;
import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.parser.Parser;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.statement.Statement;
import org.junit.Assert;
import org.junit.Test;

public class EvaluatorTest {
    @Test
    public void testArithmetic() {
        final Environment environment = new Environment();
        assertReturns(5, "3 + 2", environment);
        assertReturns(1, "3 - 2", environment);
        assertReturns(6, "3 * 2", environment);
        assertReturns(1, "3 / 2", environment);
        assertReturns(1, "3 % 2", environment);

        assertReturns(5f, "3 + 2f", environment);
        assertReturns(1f, "3f - 2", environment);
        assertReturns(6f, "3 * 2f", environment);
        assertReturns(1.5f, "3f / 2", environment);
        assertReturns(1f, "3 % 2f", environment);

        assertReturns(5d, "3 + 2d", environment);
        assertReturns(1d, "3d - 2", environment);
        assertReturns(6d, "3 * 2d", environment);
        assertReturns(1.5d, "3d / 2", environment);
        assertReturns(1d, "3 % 2d", environment);

        assertReturns(5, "3 + new Integer(2)", environment);
        assertReturns(5, "new Integer(3) + 2", environment);
        assertReturns(5, "new Integer(3) + new Integer(2)", environment);

        assertReturns("s1", "\"s\" + 1", environment);
        assertReturns("snull", "\"s\" + null", environment);

        assertFails("2 + null", environment);
        assertFails("2 + true", environment);
    }

    @Test
    public void testSign() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("i", 0), PrimitiveType.THE_INT, IntValue.of(5));
        assertReturns(5, "+i", environment);
        assertReturns(-5, "-i", environment);
        assertReturns(5, "+ +i", environment);
        assertReturns(5, "- -i", environment);
        assertReturns(-5, "+-i", environment);
        assertReturns(-5, "-+i", environment);

        environment.declareVariable(Identifier.from("io", 0), LiteralReferenceType.of(Integer.class), ObjectValue.of(5));
        assertReturns(-5, "-io", environment);

        environment.declareVariable(Identifier.from("b", 0), PrimitiveType.THE_BOOLEAN, BooleanValue.of(true));
        assertFails("+b", environment);

        environment.declareVariable(Identifier.from("s", 0), LiteralReferenceType.THE_STRING, ObjectValue.of("t"));
        assertFails("+s", environment);
    }

    @Test
    public void testAssignment() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("i", 0), PrimitiveType.THE_INT, IntValue.of(0));
        assertReturns(5, "i = 5", environment);
        assertReturns(10, "i = new Integer(10)", environment);
        assertReturns(15, "i = new Short((short) 15)", environment);
        assertReturns(20, "i += 5", environment);
        assertReturns(25, "i += 5d", environment);
        assertFails("i = 5.0", environment);
        assertFails("i += true", environment);
        assertFails("i += \"\"", environment);
        assertFails("i = new Float(10)", environment);

        environment.declareVariable(Identifier.from("d", 0), PrimitiveType.THE_DOUBLE, DoubleValue.of(0));
        assertReturns(5d, "d = 5", environment);
        assertReturns(10d, "d = 10.0", environment);

        environment.declareVariable(Identifier.from("s", 0), PrimitiveType.THE_SHORT, ShortValue.of((short) 0));
        assertReturns((short) 5, "s = 5", environment);

        environment.declareVariable(Identifier.from("st", 0), LiteralReferenceType.THE_STRING, ObjectValue.of("t"));
        assertReturns("t5", "st += 5", environment);
        assertReturns("t55.0", "st += 5d", environment);
        assertReturns("t55.0true", "st += true", environment);
        assertReturns("t55.0true,", "st += \",\"", environment);
        assertReturns("t55.0true,null", "st += null", environment);

        environment.declareVariable(Identifier.from("cs", 0), LiteralReferenceType.of(CharSequence.class), ObjectValue.of(null));
        assertReturns("test", "cs = \"test\"", environment, CharSequence.class);
        assertReturns(null, "cs = null", environment, CharSequence.class);
        assertFails("cs = new Object()", environment);
    }

    @Test
    public void testPostIncrement() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("i", 0), PrimitiveType.THE_INT, IntValue.of(5));
        assertReturns(5, "i++", environment);
        assertReturns(6, "i", environment);
        assertReturns(6, "i--", environment);
        assertReturns(5, "i", environment);

        environment.declareVariable(Identifier.from("io", 0), LiteralReferenceType.of(Integer.class), ObjectValue.of(5));
        assertReturns(5, "io++", environment);
        assertReturns((Object) 6, "io", environment);

        environment.declareVariable(Identifier.from("b", 0), PrimitiveType.THE_BOOLEAN, BooleanValue.of(true));
        assertFails("b++", environment);

        environment.declareVariable(Identifier.from("s", 0), LiteralReferenceType.THE_STRING, ObjectValue.of("t"));
        assertFails("s++", environment);
    }

    @Test
    public void testPreIncrement() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("i", 0), PrimitiveType.THE_INT, IntValue.of(5));
        assertReturns(6, "++i", environment);
        assertReturns(6, "i", environment);
        assertReturns(5, "--i", environment);
        assertReturns(5, "i", environment);

        environment.declareVariable(Identifier.from("io", 0), LiteralReferenceType.of(Integer.class), ObjectValue.of(5));
        assertReturns(6, "++io", environment);
        assertReturns((Object) 6, "io", environment);

        environment.declareVariable(Identifier.from("b", 0), PrimitiveType.THE_BOOLEAN, BooleanValue.of(true));
        assertFails("++b", environment);

        environment.declareVariable(Identifier.from("s", 0), LiteralReferenceType.THE_STRING, ObjectValue.of("t"));
        assertFails("++s", environment);
    }

    @Test
    public void testAmbiguousCall() {
        final Environment environment = new Environment();
        assertReturns(2, "java.lang.Integer.bitCount(3)", environment);
        assertReturns(System.out.hashCode(), "java.lang.System.out.hashCode()", environment);
    }

    @Test
    public void testConstructorCall() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("chars", 0), LiteralReferenceType.of(char[].class), ObjectValue.of(new char[]{'a', 'b', 'c', 'd'}));
        assertReturns("", "new String()", environment);
        assertReturns("test", "new String(\"test\")", environment);
        assertReturns("bc", "new String(chars, 1, 2)", environment);
        assertReturns((Object) 6, "new Integer(6)", environment);
        assertReturns((Object) 6f, "new Float(6)", environment);
        assertFails("new Short(0)", environment);
        assertFails("new Integer(0d)", environment);

        environment.importClass(Varargs.class);
        assertReturns(new Varargs(), "new Varargs()", environment);
        assertReturns(new Varargs(1f, 2f), "new Varargs(1f, 2f)", environment);
        assertReturns(new Varargs(1f, 2f), "new Varargs(1, 2f)", environment);
        assertReturns(new Varargs(1f, 2f), "new Varargs(1, 2)", environment);
        assertFails("new Varargs(1, 2, 1d)", environment);
    }

    @Test
    public void testMethodCall() {
        final Environment environment = new Environment();
        assertReturns(false, "new Object().equals(null)", environment);
        assertReturns("es", "new String(\"test\").substring(1, 3)", environment);
        assertReturns(6d, "new Integer(6).doubleValue()", environment);
        assertReturns(2, "Integer.bitCount(3)", environment);
        assertFails("Float.intBitsToFloat(0f)", environment);

        environment.declareVariable(Identifier.from("v", 0), LiteralReferenceType.of(Varargs.class), ObjectValue.of(new Varargs()));
        assertSucceeds("v.varargs(\"1\", \"2\")", environment);
        assertSucceeds("v.varargs(1, 2)", environment);
        assertFails("v.varargs()", environment);

        environment.declareVariable(Identifier.from("chars", 0), LiteralReferenceType.of(char[].class), ObjectValue.of(new char[]{'a', 'b', 'c', 'd'}));
        assertReturns(new char[]{'a', 'b', 'c', 'd'}, "chars.clone()", environment);
    }

    @SuppressWarnings("unused")
    public static class Varargs {
        private final float[] args;

        public Varargs(float... is) {
            args = is;
        }

        public void varargs(float... is) {
        }

        public void varargs(CharSequence... cs) {
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && Arrays.equals(args, ((Varargs) o).args);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(args);
        }

        @Override
        public String toString() {
            return Arrays.toString(args);
        }
    }

    @Test
    public void testComparison() {
        final Environment environment = new Environment();
        assertReturns(false, "3 <= 2", environment);
        assertReturns(true, "2 <= 2", environment);
        assertReturns(true, "3 >= 2", environment);
        assertReturns(true, "2 >= 2", environment);
        assertReturns(false, "3 < 2", environment);
        assertReturns(true, "3 > 2", environment);

        assertFails("3 <= null", environment);
        assertFails("3 <= true", environment);
        assertFails("true <= null", environment);

        environment.declareVariable(Identifier.from("io", 0), LiteralReferenceType.of(Integer.class), ObjectValue.of(5));
        assertReturns(false, "io <= 3", environment);
        assertFails("io <= true", environment);

        environment.declareVariable(Identifier.from("s", 0), LiteralReferenceType.THE_STRING, ObjectValue.of("t"));
        assertFails("s <= 3", environment);
    }

    @Test
    public void testEqual() {
        final Environment environment = new Environment();
        assertReturns(false, "3 == 2", environment);
        assertReturns(true, "3 != 2", environment);

        assertReturns(true, "true == true", environment);
        assertReturns(false, "true != true", environment);

        assertReturns(true, "null == null", environment);

        environment.declareVariable(Identifier.from("io", 0), LiteralReferenceType.of(Integer.class), ObjectValue.of(5));
        assertReturns(true, "io == io", environment);
        assertReturns(false, "io == null", environment);
        assertReturns(true, "io == 5", environment);

        environment.declareVariable(Identifier.from("s", 0), LiteralReferenceType.THE_STRING, ObjectValue.of("t"));
        assertReturns(true, "s == s", environment);
        assertReturns(false, "s == null", environment);
        assertReturns(false, "s == \"e\"", environment);
        assertFails("s == 5", environment);

        assertFails("3 == null", environment);
        assertFails("3 == true", environment);
        assertFails("true == null", environment);
        assertFails("io == true", environment);
    }

    @Test
    public void testTypeCheck() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("i", 0), PrimitiveType.THE_INT, IntValue.of(5));
        environment.declareVariable(Identifier.from("io", 0), LiteralReferenceType.of(Integer.class), ObjectValue.of(5));
        environment.declareVariable(Identifier.from("v", 0), VoidType.THE_VOID, VoidValue.THE_VOID);
        environment.declareVariable(Identifier.from("c", 0), LiteralReferenceType.of(char[].class), ObjectValue.of(new char[]{'a', 'b', 'c', 'd'}));
        environment.declareVariable(Identifier.from("f", 0), LiteralReferenceType.of(Float[].class), ObjectValue.of(new Float[]{1f, 2f, 3f, 4f}));
        assertReturns(true, "io instanceof Integer", environment);
        assertReturns(true, "io instanceof Number", environment);
        assertReturns(true, "io instanceof Comparable", environment);
        assertReturns(false, "io instanceof CharSequence", environment);
        assertReturns(false, "io instanceof Float", environment);
        assertReturns(true, "c instanceof char[]", environment);
        assertReturns(true, "c instanceof Object", environment);
        assertReturns(false, "c instanceof CharSequence", environment);
        assertReturns(true, "f instanceof Float[]", environment);
        assertReturns(true, "f instanceof Number[]", environment);
        assertReturns(true, "f instanceof Object", environment);

        assertFails("i instanceof Object", environment);
        assertFails("io instanceof int", environment);
        assertFails("v instanceof Object", environment);
    }

    @Test
    public void testBitwiseLogic() {
        final Environment environment = new Environment();
        assertReturns(2, "3 & 2", environment);
        assertReturns(3, "3 | 2", environment);
        assertReturns(1, "3 ^ 2", environment);
        assertReturns(true, "true & true", environment);

        assertFails("true & 2", environment);
        assertFails("3.0 & 2", environment);
        assertFails("\"t\" & 2", environment);
        assertFails("true & \"t\"", environment);
        assertFails("null & true", environment);
        assertFails("true & null", environment);
    }

    @Test
    public void testBitwiseNot() {
        final Environment environment = new Environment();
        assertReturns(0xFFFF0000, "~0xFFFF", environment);

        assertFails("~true", environment);
        assertFails("~3.0", environment);
        assertFails("~\"t\"", environment);
        assertFails("~null", environment);
    }

    @Test
    public void testBooleanLogic() {
        final Environment environment = new Environment();
        assertReturns(true, "true && true", environment);
        assertReturns(true, "true || false", environment);

        assertFails("true && 2", environment);
        assertFails("3.0 && true", environment);
        assertFails("\"t\" && true", environment);
        assertFails("true && \"t\"", environment);
        assertFails("null && true", environment);
        assertFails("true && null", environment);
    }

    @Test
    public void testBooleanNot() {
        final Environment environment = new Environment();
        assertReturns(false, "!true", environment);

        assertFails("!2", environment);
        assertFails("!3.0", environment);
        assertFails("!\"t\"", environment);
        assertFails("!null", environment);
    }

    @Test
    public void testAmbiguousReference() {
        final Environment environment = new Environment();
        environment.importClass(Fields.class);
        final Fields f = new Fields();
        environment.declareVariable(Identifier.from("f", 0), LiteralReferenceType.of(Fields.class), ObjectValue.of(f));

        assertReturns(Integer.TYPE, "java.lang.Integer.TYPE", environment);
        assertReturns(-2, "f.nested.instance", environment);
        assertReturns(-1, "Fields.nested.static2", environment);
    }

    @Test
    public void testFieldAccess() {
        final Environment environment = new Environment();
        environment.importClass(Fields.class);
        final Fields f = new Fields();
        environment.declareVariable(Identifier.from("f", 0), LiteralReferenceType.of(Fields.class), ObjectValue.of(f));
        assertReturns(-1, "Fields._static", environment);
        assertReturns(-2, "f.instance", environment);
        assertReturns(-1, "f._static", environment);
        assertFails("Fields.instance", environment);

        assertReturns(-3, "Fields._static = -3", environment);
        Assert.assertEquals(Fields._static, -3);
        assertReturns(-4, "f.instance = -4", environment);
        Assert.assertEquals(f.instance, -4);
        assertReturns(-5, "f._static = -5", environment);
        Assert.assertEquals(Fields._static, -5);
        assertFails("Fields.instance = -6", environment);
        Assert.assertFalse(f.instance == -6);
        assertFails("Fields.staticReadonly = 2", environment);
        assertFails("f.instanceReadonly = 2", environment);

        environment.declareVariable(Identifier.from("c", 0), LiteralReferenceType.of(char[].class), ObjectValue.of(new char[]{'a', 'b', 'c', 'd'}));
        assertReturns(4, "c.length", environment);
        assertFails("c.length = 5", environment);
    }

    @SuppressWarnings("unused")
    public static class Fields {
        public static int _static = -1;
        public static int static2 = -1;
        public int instance = -2;
        public static final int staticReadonly = 1;
        public final int instanceReadonly = 1;
        public static final Fields nested;

        static {
            nested = new Fields();
        }
    }

    @Test
    public void testIndexAccess() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("c", 0), LiteralReferenceType.of(char[].class), ObjectValue.of(new char[]{'a', 'b', 'c', 'd'}));
        environment.declareVariable(Identifier.from("cc", 0), LiteralReferenceType.of(char[][].class), ObjectValue.of(new char[][]{{'a'}, {'b'}, {'c'}, {'d'}}));
        assertReturns('a', "c[0]", environment);
        assertReturns('b', "c[1]", environment);
        assertReturns('c', "c[2]", environment);
        assertReturns('d', "c[3]", environment);
        assertReturns('d', "c[(byte) 3]", environment);
        assertFails("c[-1]", environment);
        assertFails("c[4]", environment);
        assertFails("c[0.0]", environment);
        assertFails("c[null]", environment);
        assertFails("c[\"1\"]", environment);

        assertReturns('a', "cc[0][0]", environment);
        assertReturns('b', "cc[1][0]", environment);
        assertReturns('c', "cc[2][0]", environment);
        assertReturns('d', "cc[3][0]", environment);
    }

    @Test
    public void testVariableAccess() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("i", 0), PrimitiveType.THE_INT, IntValue.of(5));

        assertReturns(5, "i", environment);
        assertReturns(6, "i = 6", environment);
        assertFails("j", environment);
        assertFails("j = 6", environment);
    }

    @Test
    public void testArrayConstructor() {
        final Environment environment = new Environment();
        assertReturns(new int[1], "new int[1]", environment);
        assertReturns(new int[1][], "new int[1][]", environment);
        assertReturns(new int[1][2], "new int[1][2]", environment);
        assertReturns(new int[1][2][][], "new int[1][2][][]", environment);
        assertReturns(new Object[1], "new Object[1]", environment);
        assertReturns(new Object[1][], "new Object[1][]", environment);
        assertReturns(new Object[1][2], "new Object[1][2]", environment);
        assertReturns(new Object[1][2][][], "new Object[1][2][][]", environment);

        assertReturns(new int[]{1, 2}, "new int[]{1, 2}", environment);
        //noinspection UnnecessaryBoxing
        assertReturns(new float[]{1f, 2, new Float(3)}, "new float[]{1f, 2, new Float(3)}", environment);
        //noinspection UnnecessaryBoxing
        assertReturns(new Object[]{"1", new Integer(1)}, "new Object[]{\"1\", new Integer(1)}", environment);
        assertReturns(new int[][]{{1, 2}, {3, 4}}, "new int[][]{{1, 2}, {3, 4}}", environment);
        assertReturns(new int[][]{{1, 2}, null}, "new int[][]{{1, 2}, null}", environment);
        assertReturns(new int[][]{{1, 2}, new int[0]}, "new int[][]{{1, 2}, new int[0]}", environment);
    }

    @Test
    public void testCast() {
        final Environment environment = new Environment();
        environment.declareVariable(Identifier.from("i", 0), PrimitiveType.THE_INT, IntValue.of(5));
        environment.declareVariable(Identifier.from("io", 0), LiteralReferenceType.of(Integer.class), ObjectValue.of(5));
        environment.declareVariable(Identifier.from("d", 0), PrimitiveType.THE_DOUBLE, DoubleValue.of(6));
        environment.declareVariable(Identifier.from("b", 0), PrimitiveType.THE_BOOLEAN, BooleanValue.of(true));

        assertReturns((byte) 5, "(byte) i", environment);
        assertReturns((double) 5, "(double) i", environment);
        assertReturns((Object) 5, "(Integer) i", environment);
        assertReturns(5, "(int) io", environment);
        assertReturns(5d, "(double) io", environment);
        assertReturns(6d, "(Number) d", environment, Number.class);
        assertReturns(true, "(boolean) b", environment);
        assertReturns((Object) true, "(Boolean) b", environment);
        assertFails("(Integer) d", environment);
        assertFails("(int) b", environment);
        assertFails("(Number) b", environment);

        environment.importClass(Serializable.class);
        environment.declareVariable(Identifier.from("s", 0), LiteralReferenceType.of(CharSequence.class), ObjectValue.of("t"));
        assertReturns("t", "(Object) s", environment, Object.class);
        assertReturns("t", "(String) s", environment);
        assertReturns(null, "(String) null", environment, String.class);
        assertFails("(Integer) s", environment);
        assertFails("(int) s", environment);

        assertReturns("1", "(Object) (true ? \"1\" : new StringBuilder(\"2\"))", environment, Object.class);
        assertReturns("1", "(String) (true ? \"1\" : new StringBuilder(\"2\"))", environment, String.class);
        assertFails("(Number) (true ? \"1\" : new StringBuilder(\"2\"))", environment);

        final char[][] cc = {{'a'}, {'b'}, {'c'}, {'d'}};
        environment.declareVariable(Identifier.from("cc", 0), LiteralReferenceType.of(char[][].class), ObjectValue.of(cc));
        assertReturns(cc, "(Object) cc", environment, Object.class);
        assertReturns(cc, "(Serializable) cc", environment, Serializable.class);
        assertReturns(cc, "(Cloneable) cc", environment, Cloneable.class);
        assertReturns(cc, "(Object[]) cc", environment, Object[].class);
        assertReturns(cc, "(Serializable[]) cc", environment, Serializable[].class);
        assertReturns(cc, "(Cloneable[]) cc", environment, Cloneable[].class);
    }

    @Test
    public void testClassAccess() {
        final Environment environment = new Environment();
        assertReturns(Object.class, "Object.class", environment);
        assertReturns(int.class, "int.class", environment);
        assertReturns(void.class, "void.class", environment);
        assertReturns(int[].class, "int[].class", environment);
        assertReturns(Object[].class, "Object[].class", environment);

        environment.importClass(Map.class);
        assertReturns(Map.Entry.class, "Map.Entry.class", environment);
    }

    @Test
    public void testConditional() {
        final Environment environment = new Environment();
        environment.importClass(Serializable.class);

        assertReturns("1", "true ? \"1\" : \"1\"", environment);
        assertReturns(1f, "true ? 1f : 1", environment);
        assertReturns(1f, "true ? new Float(1f) : new Integer(1)", environment);
        assertReturns((Object) 1f, "true ? 1f : null", environment);
        assertReturns((Object) null, "true ? null : null", environment);
        assertReturns("1", "true ? \"1\" : new StringBuilder(\"2\")", environment, CharSequence.class, Serializable.class);
        assertReturns(new Integer[1], "true ? new Integer[1] : new Float[2]", environment, Number[].class, Comparable[].class);
        assertReturns((short) 1, "true ? (byte) 1 : (short) 2", environment);
        assertReturns((short) 1, "true ? (short) 1 : (byte) 2", environment);
        assertFails("12 ? \"1\" : \"1\"", environment);
    }

    @Test
    public void testShift() {
        final Environment environment = new Environment();
        assertReturns(32, "8 << 2", environment);
        assertReturns(2, "8 >> 2", environment);
        assertReturns(-2, "-8 >> 2", environment);
        assertReturns(2, "8 >>> 2", environment);
        assertReturns(0x3FFFFFFE, "-8 >>> 2", environment);

        assertReturns(12, "3 << new Integer(2)", environment);
        assertReturns(12, "3 << new Short((short) 2)", environment);
        assertReturns(12, "new Integer(3) << 2", environment);
        assertReturns(12, "new Integer(3) << new Integer(2)", environment);
        assertReturns(12l, "new Long(3) << new Integer(2)", environment);
        assertReturns(12, "new Integer(3) << new Long(2)", environment);

        assertFails("2 << null", environment);
        assertFails("2 << true", environment);
        assertFails("2 << 2f", environment);
    }

    @Test
    public void testDeclaration() {
        final Environment environment = new Environment();

        assertSucceeds("int i;", environment);
        assertHasVariable(int.class, "i", environment);

        assertSucceeds("int j = 0;", environment);
        assertHasVariable("j", 0, environment);

        assertSucceeds("int k, l = 2, m; ", environment);
        assertHasVariable(int.class, "k", environment);
        assertHasVariable("l", 2, environment);
        assertHasVariable(int.class, "m", environment);

        assertSucceeds("int[] ii = {0};", environment);
        assertHasVariable("ii", new int[]{0}, environment);

        assertSucceeds("int jj[] = {1};", environment);
        assertHasVariable("jj", new int[]{1}, environment);

        assertSucceeds("int[] kk, ll = {3}, mm[][];", environment);
        assertHasVariable(int[].class, "kk", environment);
        assertHasVariable("ll", new int[]{3}, environment);
        assertHasVariable(int[][][].class, "mm", environment);

        assertSucceeds("Object o;", environment);
        assertHasVariable(Object.class, "o", environment);

        assertSucceeds("byte b = 23;", environment);
        assertHasVariable("b", (byte) 23, environment);
    }

    @Test
    public void testEmpty() {
        final Environment environment = new Environment();
        assertSucceeds(";", environment);
    }

    @Test
    public void testImport() {
        final Environment environment = new Environment();

        assertHasClass(Integer.class, environment);

        assertSucceeds("import java.util.List;", environment);
        assertHasClass(List.class, environment);

        assertSucceeds("import java.util.Map.Entry;", environment);
        assertHasClass(Map.Entry.class, environment);
    }

    private Environment assertReturns(boolean expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), boolean.class);
            Assert.assertEquals(expected, expression.getValue(environment).asBoolean());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(byte expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), byte.class);
            Assert.assertEquals(expected, expression.getValue(environment).asByte());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(short expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), short.class);
            Assert.assertEquals(expected, expression.getValue(environment).asShort());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(char expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), char.class);
            Assert.assertEquals(expected, expression.getValue(environment).asChar());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(char[] expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), char[].class);
            Assert.assertArrayEquals(expected, (char[]) expression.getValue(environment).asObject());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(int expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), int.class);
            Assert.assertEquals(expected, expression.getValue(environment).asInt());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(long expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), long.class);
            Assert.assertEquals(expected, expression.getValue(environment).asLong());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(int[] expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), int[].class);
            Assert.assertArrayEquals(expected, (int[]) expression.getValue(environment).asObject());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(float expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), float.class);
            Assert.assertEquals(expected, expression.getValue(environment).asFloat(), 0);
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(float[] expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), float[].class);
            Assert.assertArrayEquals(expected, (float[]) expression.getValue(environment).asObject(), 0);
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(double expected, String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            assertTypeEquals(expression.getType(environment), double.class);
            Assert.assertEquals(expected, expression.getValue(environment).asDouble(), 0);
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(Object expected, String source, Environment environment, Class<?>... expectedTypes) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            if (expectedTypes.length > 0) {
                assertTypeEquals(expression.getType(environment), expectedTypes);
            } else {
                assertTypeEquals(expression.getType(environment), expected == null ? null : expected.getClass());
            }
            Assert.assertEquals(expected, expression.getValue(environment).asObject());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertReturns(Object[] expected, String source, Environment environment, Class<?>... expectedTypes) {
        final SourceMetadata metadata = new SourceMetadata(source);
        final Expression expression = parse(source, metadata);
        try {
            if (expectedTypes.length > 0) {
                assertTypeEquals(expression.getType(environment), expectedTypes);
            } else {
                assertTypeEquals(expression.getType(environment), expected == null ? null : expected.getClass());
            }
            Assert.assertArrayEquals(expected, (Object[]) expression.getValue(environment).asObject());
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertSucceeds(String source, Environment environment) {
        final SourceMetadata metadata = new SourceMetadata(source);
        try {
            source = Decoder.decode(source, metadata);
            final List<Token> tokens = Lexer.lex(source);
            if (tokens.get(tokens.size() - 1).getID() == TokenID.SYMBOL_SEMICOLON) {
                final List<Statement> statements = Parser.parse(tokens);
                for (Statement statement : statements) {
                    statement.execute(environment);
                }
            } else {
                final Expression expression = Parser.parseExpression(tokens);
                expression.getType(environment);
                expression.getValue(environment);
            }
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
        return environment;
    }

    private Environment assertFails(String source, Environment environment) {
        try {
            assertSucceeds(source, environment);
            Assert.fail("Expected evaluator exception");
        } catch (AssertionError expected) {
            if (!(expected.getCause() instanceof EvaluatorException)) {
                Assert.fail("Expected evaluator exception");
            }
        }
        return environment;
    }

    private void assertTypeEquals(Type type, Class<?>... expected) {
        if (type instanceof ReferenceIntersectionType) {
            final Set<SingleReferenceType> lowestUpperBound = ((ReferenceIntersectionType) type).getLowestUpperBound();
            final Set<Class<?>> actualSet = new HashSet<>();
            for (SingleReferenceType bound : lowestUpperBound) {
                actualSet.add(bound.getTypeClass());
            }
            final Set<Class<?>> expectedSet = new HashSet<>(Arrays.asList(expected));
            Assert.assertEquals(expectedSet, actualSet);
        } else {
            final Class<?> _class = type instanceof NullType ? null : ((LiteralType) type).getTypeClass();
            Assert.assertEquals(expected[0], _class);
        }
    }

    private Expression parse(String source, SourceMetadata metadata) {
        try {
            source = Decoder.decode(source, metadata);
            final List<Token> tokens = Lexer.lex(source);
            return Parser.parseExpression(tokens);
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
    }

    private void assertHasVariable(Class<?> type, String name, Environment environment) {
        Assert.assertEquals(type, environment.getVariableType(Identifier.from(name, 0)).getTypeClass());
    }

    private void assertHasVariable(String name, byte value, Environment environment) {
        Assert.assertEquals(byte.class, environment.getVariableType(Identifier.from(name, 0)).getTypeClass());
        Assert.assertEquals(value, environment.getVariable(Identifier.from(name, 0)).asByte());
    }

    private void assertHasVariable(String name, int value, Environment environment) {
        Assert.assertEquals(int.class, environment.getVariableType(Identifier.from(name, 0)).getTypeClass());
        Assert.assertEquals(value, environment.getVariable(Identifier.from(name, 0)).asInt());
    }

    private void assertHasVariable(String name, int[] value, Environment environment) {
        Assert.assertEquals(int[].class, environment.getVariableType(Identifier.from(name, 0)).getTypeClass());
        Assert.assertArrayEquals(value, (int[]) environment.getVariable(Identifier.from(name, 0)).asObject());
    }

    private void assertHasClass(Class<?> _class, Environment environment) {
        Assert.assertEquals(_class, environment.getClass(Identifier.from(_class.getSimpleName(), 0)));
    }
}
