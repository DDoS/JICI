/*
 * This file is part of JICI, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015-2016 Aleksi Sapon <http://sapon.ca/jici/>
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

import org.junit.Assert;
import org.junit.Test;

import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.parser.Parser;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.statement.Statement;

public class ParserTest {
    @Test
    public void testParseAtom() {
        testParseExpression("1", "1");
        testParseExpression("1L", "1L");
        testParseExpression("1f", "1f");
        testParseExpression("1d", "1d");

        testParseExpression("true", "true");
        testParseExpression("false", "false");

        testParseExpression("null", "null");

        testParseExpression("'a'", "'a'");

        testParseExpression("\"a\"", "\"a\"");

        testParseExpression("test", "test");
        testParseExpression("test.stuff", "test.stuff");

        testParseExpression("ConstructorCall(new ca.sapon.Test())", "new ca.sapon.Test()");
        testParseExpression("ConstructorCall(new Test())", "new Test()");
        testParseExpression("ConstructorCall(new Test(0, 1, 2))", "new Test(0, 1, 2)");
        testParseExpression("ConstructorCall(new Test<String>())", "new Test<String>()");
        testParseExpression("ConstructorCall(new Test<String, Object>())", "new Test<String, Object>()");
        testParseExpression("ConstructorCall(new Test<Test<String>, Test<Object>>())", "new Test<Test<String>, Test<Object>>()");
        testParseExpression("ConstructorCall(new Test<Test<Test<Object>>>())", "new Test<Test<Test<Object>>>()");
        testParseExpression("ConstructorCall(new Test<Test<Test<Test<Object>>>>())", "new Test<Test<Test<Test<Object>>>>()");
        testParseExpression("ConstructorCall(new <String>Test())", "new <String>Test()");

        testParseExpression("ConstructorCall(new ca.sapon.Test<>())", "new ca.sapon.Test<>()");
        testParseExpression("ConstructorCall(new Test<>())", "new Test<>()");
        testParseExpression("ConstructorCall(new <String>Test<>())", "new <String>Test<>()");
        testParseExpression("ConstructorCall(new <Integer>Test<String>())", "new <Integer>Test<String>()");

        testParseExpression("ConstructorCall(new Inner<String>.Outer<Integer>())", "new Inner<String>.Outer<Integer>()");

        testParseExpression("QualifiedConstructorCall(\"1\".new Test())", "\"1\".new Test()");
        testParseExpression("QualifiedConstructorCall(\"1\".new Test<String>())", "\"1\".new Test<String>()");
        testParseExpression("QualifiedConstructorCall(\"1\".new <String>Test())", "\"1\".new <String>Test()");
        testParseExpression("QualifiedConstructorCall(\"1\".new <String>Test<>())", "\"1\".new <String>Test<>()");
        testParseExpression("QualifiedConstructorCall(\"1\".new <Integer>Test<String>())", "\"1\".new <Integer>Test<String>()");

        testParseExpression("QualifiedConstructorCall(QualifiedConstructorCall(a.new Test()).new More())", "a.new Test().new More()");
        testParseExpression("QualifiedConstructorCall(QualifiedConstructorCall(ConstructorCall(new Crazy()).new Test()).new More())", "new Crazy().new Test().new More()");

        testParseExpression("ArrayConstructor(new int[1])", "new int[1]");
        testParseExpression("ArrayConstructor(new Object[1])", "new Object[1]");
        testParseExpression("ArrayConstructor(new Object[1][2])", "new Object[1][2]");
        testParseExpression("ArrayConstructor(new Object[1][])", "new Object[1][]");
        testParseExpression("ArrayConstructor(new Object[]{})", "new Object[]{}");
        testParseExpression("ArrayConstructor(new Object[][]{})", "new Object[][]{}");

        testParseExpression("ArrayConstructor(new ca.sapon.Test[1])", "new ca.sapon.Test[1]");
        testParseExpression("ArrayConstructor(new ca.sapon.Test<?>[1])", "new ca.sapon.Test<?>[1]");

        testParseExpression("ArrayConstructor(new Inner<String>.Outer<Integer>[1])", "new Inner<String>.Outer<Integer>[1]");

        testParseExpression("ClassAccess(test.class)", "test.class");
        testParseExpression("ClassAccess(int.class)", "int.class");
        testParseExpression("ClassAccess(void.class)", "void.class");
        testParseExpression("ClassAccess(int[].class)", "int[].class");
        testParseExpression("ClassAccess(test[].class)", "test[].class");

        testParseExpression("test", "(test)");
    }

    @Test
    public void testParseAccess() {
        testParseExpression("FieldAccess(\"f\".m)", "\"f\".m");

        testParseExpression("MethodCall(\"f\".m())", "\"f\".m()");
        testParseExpression("MethodCall(\"f\".m(1))", "\"f\".m(1)");
        testParseExpression("MethodCall(\"f\".<Test>m(1))", "\"f\".<Test>m(1)");

        testParseExpression("IndexAccess(test[m])", "test[m]");

        testParseExpression("MethodCall(FieldAccess(IndexAccess(FieldAccess(ConstructorCall(new M()).t)[1]).m).k())", "new M().t[1].m.k()");
    }

    @Test
    public void testParseUnary() {
        testParseExpression("+1", "+1");
        testParseExpression("-1", "-1");
        testParseExpression("+-1", "+-1");
        testParseExpression("-+1", "-+1");
        testParseExpression("- -1", "- -1");
        testParseExpression("+ +1", "+ +1");

        testParseExpression("Sign(+test)", "+test");
        testParseExpression("Sign(-test)", "-test");

        testParseExpression("BooleanNot(!test)", "!test");
        testParseExpression("BitwiseNot(~test)", "~test");

        testParseExpression("PreIncrement(++test)", "++test");
        testParseExpression("PreIncrement(--test)", "--test");
        testParseExpression("PostIncrement(test++)", "test++");
        testParseExpression("PostIncrement(test--)", "test--");

        testParseExpression("Cast((int) test)", "(int) test");
        testParseExpression("Cast((Object) test)", "(Object) test");
        testParseExpression("Cast((Object[]) test)", "(Object[]) test");
        testParseExpression("Cast((int[]) test)", "(int[]) test");
        testParseExpression("Cast((int[][]) test)", "(int[][]) test");

        testParseExpression("Cast((Object) BooleanNot(!Sign(-Sign(+PostIncrement(test++)))))", "(Object) !-+test++");
    }

    @Test
    public void testParseMultiply() {
        testParseExpression("Arithmetic(l * r)", "l * r");
        testParseExpression("Arithmetic(l / r)", "l / r");
        testParseExpression("Arithmetic(l % r)", "l % r");

        testParseExpression("Arithmetic(Arithmetic(Arithmetic(a * b) / c) % d)", "a * b / c % d");
    }

    @Test
    public void testParseAdd() {
        testParseExpression("Arithmetic(l - r)", "l - r");
        testParseExpression("Arithmetic(l + r)", "l + r");

        testParseExpression("Arithmetic(Arithmetic(a - b) + c)", "a - b + c");
    }

    @Test
    public void testParseShift() {
        testParseExpression("Shift(l << r)", "l << r");
        testParseExpression("Shift(l >> r)", "l >> r");
        testParseExpression("Shift(l >>> r)", "l >>> r");

        testParseExpression("Shift(Shift(Shift(a << b) >> c) >>> d)", "a << b >> c >>> d");
    }

    @Test
    public void testParseComparison() {
        testParseExpression("Comparison(l < r)", "l < r");
        testParseExpression("Comparison(l > r)", "l > r");
        testParseExpression("Comparison(l <= r)", "l <= r");
        testParseExpression("Comparison(l >= r)", "l >= r");

        testParseExpression("TypeCheck(l instanceof Object)", "l instanceof Object");
        testParseExpression("TypeCheck(l instanceof Object[])", "l instanceof Object[]");
        testParseExpression("TypeCheck(l instanceof int[])", "l instanceof int[]");

        testParseExpression("TypeCheck(Comparison(Comparison(Comparison(a < b) > c) <= d) instanceof e)", "a < b > c <= d instanceof e");
    }

    @Test
    public void testParseEqual() {
        testParseExpression("Equal(l == r)", "l == r");
        testParseExpression("Equal(l != r)", "l != r");

        testParseExpression("Equal(Equal(a == b) != c)", "a == b != c");
    }

    @Test
    public void testParseBitwiseAND() {
        testParseExpression("BitwiseLogic(l & r)", "l & r");

        testParseExpression("BitwiseLogic(BitwiseLogic(a & b) & c)", "a & b & c");
    }

    @Test
    public void testParseBitwiseXOR() {
        testParseExpression("BitwiseLogic(l ^ r)", "l ^ r");

        testParseExpression("BitwiseLogic(BitwiseLogic(a ^ b) ^ c)", "a ^ b ^ c");
    }

    @Test
    public void testParseBitwiseOR() {
        testParseExpression("BitwiseLogic(l | r)", "l | r");

        testParseExpression("BitwiseLogic(BitwiseLogic(a | b) | c)", "a | b | c");
    }

    @Test
    public void testParseBooleanAND() {
        testParseExpression("BooleanLogic(l && r)", "l && r");

        testParseExpression("BooleanLogic(BooleanLogic(a && b) && c)", "a && b && c");
    }

    @Test
    public void testParseBooleanOR() {
        testParseExpression("BooleanLogic(l || r)", "l || r");

        testParseExpression("BooleanLogic(BooleanLogic(a || b) || c)", "a || b || c");
    }

    @Test
    public void testParseConditional() {
        testParseExpression("Conditional(t ? l : r)", "t ? l : r");

        testParseExpression("Conditional(r ? Conditional(s ? u : v) : Conditional(t ? k : l))", "r ? s ? u : v : t ? k : l");
    }

    @Test
    public void testParseAssignment() {
        testParseExpression("Assignment(l = r)", "l = r");
        testParseExpression("Assignment(l = Arithmetic(l + r))", "l += r");
        testParseExpression("Assignment(l = Arithmetic(l - r))", "l -= r");
        testParseExpression("Assignment(l = Arithmetic(l * r))", "l *= r");
        testParseExpression("Assignment(l = Arithmetic(l / r))", "l /= r");
        testParseExpression("Assignment(l = Arithmetic(l % r))", "l %= r");
        testParseExpression("Assignment(l = Shift(l << r))", "l <<= r");
        testParseExpression("Assignment(l = Shift(l >> r))", "l >>= r");
        testParseExpression("Assignment(l = Shift(l >>> r))", "l >>>= r");
        testParseExpression("Assignment(l = BitwiseLogic(l & r))", "l &= r");
        testParseExpression("Assignment(l = BitwiseLogic(l ^ r))", "l ^= r");
        testParseExpression("Assignment(l = BitwiseLogic(l | r))", "l |= r");

        testParseExpression("Assignment(a = Assignment(b = c))", "a = b = c");
    }

    @Test
    public void testParseEmpty() {
        testParseStatement("Empty()", ";");
    }

    @Test
    public void testParsePriority() {
        testParseExpression("Assignment(x = " +
                        "Conditional(v ? y : " +
                        "BooleanLogic(a || " +
                        "BooleanLogic(d && " +
                        "BitwiseLogic(z | " +
                        "BitwiseLogic(g ^ " +
                        "BitwiseLogic(q & " +
                        "Equal(t == " +
                        "Comparison(r >= " +
                        "Shift(p >> " +
                        "Arithmetic(n + " +
                        "Arithmetic(j * " +
                        "BooleanNot(!" +
                        "MethodCall(m.k()" +
                        "))))))))))))))",
                "x = v ? y : a || d && z | g ^ q & t == r >= p >> n + j * !m.k()");
    }

    @Test
    public void testParseDeclaration() {
        testParseStatement("Declaration(Object m)", "Object m;");
        testParseStatement("Declaration(Object m, k, j)", "Object m, k, j;");
        testParseStatement("Declaration(Object<?> m)", "Object<?> m;");
        testParseStatement("Declaration(Object<? extends String> m)", "Object<? extends String> m;");
        testParseStatement("Declaration(Object<? super String> m)", "Object<? super String> m;");
        testParseStatement("Declaration(Object<? super String, Double> m)", "Object<? super String, Double> m;");

        testParseStatement("Declaration(Object.Nested m)", "Object.Nested m;");
        testParseStatement("Declaration(Object.Nested<Integer> m)", "Object.Nested<Integer> m;");
        testParseStatement("Declaration(Object<Double>.Nested m)", "Object<Double>.Nested m;");
        testParseStatement("Declaration(Object<Double>.Nested<Integer> m)", "Object<Double>.Nested<Integer> m;");
        testParseStatement("Declaration(Object<Triple>.Nested<Integer>.Classes<String> m)", "Object<Triple>.Nested<Integer>.Classes<String> m;");

        testParseStatement("Declaration(Object m = 1)", "Object m = 1;");
        testParseStatement("Declaration(Object m = 1, k = 2, j = 3)", "Object m = 1, k = 2, j = 3;");

        testParseStatement("Declaration(Object[] m)", "Object[] m;");
        testParseStatement("Declaration(Object m[])", "Object m[];");
        testParseStatement("Declaration(Object m[], k, j[][])", "Object m[], k, j[][];");
        testParseStatement("Declaration(Object[] m[], k, j[][])", "Object[] m[], k, j[][];");

        testParseStatement("Declaration(Object[] m = {})", "Object[] m = {};");
        testParseStatement("Declaration(Object[] m = {})", "Object[] m = {,};");
        testParseStatement("Declaration(Object[] m = {null})", "Object[] m = {null};");
        testParseStatement("Declaration(Object[] m = {null})", "Object[] m = {null,};");
        testParseStatement("Declaration(Object[] m = {null, null})", "Object[] m = {null, null};");
        testParseStatement("Declaration(Object[][] m = {{}})", "Object[][] m = {{}};");
        testParseStatement("Declaration(Object[][] m = {{}, {}})", "Object[][] m = {{}, {}};");
        testParseStatement("Declaration(Object[][] m = {{}, null, {}})", "Object[][] m = {{}, null, {}};");
    }

    @Test
    public void testParseImport() {
        testParseStatement("Import(Object)", "import Object;");
        testParseStatement("Import(java.lang.Object)", "import java.lang.Object;");
        testParseStatement("Import(java.lang.*)", "import java.lang.*;");
    }

    @Test
    public void testParseStatementExpression() {
        testParseStatement("Assignment(m = 2)", "m = 2;");
        testParseStatement("PostIncrement(m++)", "m++;");
        testParseStatement("ConstructorCall(new Test())", "new Test();");
        testParseStatement("MethodCall(m.test())", "m.test();");
    }

    private void testParseExpression(String expected, String source) {
        assertEqual(expected, Parser.parseExpression(Lexer.lex(source)));
    }

    private void testParseStatement(String expected, String source) {
        assertEqual(expected, Parser.parseStatement(Lexer.lex(source)));
    }

    private void assertEqual(String expected, Expression actual) {
        Assert.assertEquals(expected, actual.toString());
    }

    private void assertEqual(String expected, Statement actual) {
        Assert.assertEquals(expected, actual.toString());
    }
}
