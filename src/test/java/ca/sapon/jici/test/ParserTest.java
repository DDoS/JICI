/**
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

import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.LexerException;
import ca.sapon.jici.parser.Parser;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.statement.Statement;
import org.junit.Assert;
import org.junit.Test;

public class ParserTest {
    @Test
    public void testParseAtom() throws LexerException {
        testParseExpression("1", "1");
        testParseExpression("1L", "1L");
        testParseExpression("1f", "1f");
        testParseExpression("1d", "1d");

        testParseExpression("test", "test");
    }

    @Test
    public void testParseAccess() throws LexerException {
        testParseExpression("FieldAccess(\"f\".m)", "\"f\".m");
        testParseExpression("ClassAccess(test.class)", "test.class");
        testParseExpression("ClassAccess(int.class)", "int.class");
        testParseExpression("IndexAccess(test[m])", "test[m]");
        testParseExpression("MethodCall(test())", "test()");
        testParseExpression("MethodCall(test(0, 1, 2))", "test(0, 1, 2)");
        testParseExpression("ConstructorCall(new Test())", "new Test()");
        testParseExpression("ConstructorCall(new Test(0, 1, 2))", "new Test(0, 1, 2)");

        testParseExpression("MethodCall(FieldAccess(IndexAccess(ConstructorCall(new M()).t[1]).m).k())", "new M().t[1].m.k()");
    }

    @Test
    public void testParseUnary() throws LexerException {
        testParseExpression("Sign(+test)", "+test");
        testParseExpression("Sign(-test)", "-test");

        testParseExpression("BooleanLogic(!test)", "!test");
        testParseExpression("BitwiseLogic(~test)", "~test");

        testParseExpression("Increment(++test)", "++test");
        testParseExpression("Increment(--test)", "--test");
        testParseExpression("Increment(test++)", "test++");
        testParseExpression("Increment(test--)", "test--");

        testParseExpression("Cast((int) test)", "(int) test");
        testParseExpression("Cast((Object) test)", "(Object) test");

        testParseExpression("Cast((Object) BooleanLogic(!Increment(--Increment(++Increment(Increment(test--)++)))))", "(Object) !--++test--++");
    }

    @Test
    public void testParseMultiply() throws LexerException {
        testParseExpression("Multiply(l * r)", "l * r");
        testParseExpression("Multiply(l / r)", "l / r");
        testParseExpression("Multiply(l % r)", "l % r");

        testParseExpression("Multiply(Multiply(Multiply(a * b) / c) % d)", "a * b / c % d");
    }

    @Test
    public void testParseAdd() throws LexerException {
        testParseExpression("Add(l - r)", "l - r");
        testParseExpression("Add(l + r)", "l + r");

        testParseExpression("Add(Add(a - b) + c)", "a - b + c");
    }

    @Test
    public void testParseShift() throws LexerException {
        testParseExpression("Shift(l << r)", "l << r");
        testParseExpression("Shift(l >> r)", "l >> r");
        testParseExpression("Shift(l >>> r)", "l >>> r");

        testParseExpression("Shift(Shift(Shift(a << b) >> c) >>> d)", "a << b >> c >>> d");
    }

    @Test
    public void testParseComparison() throws LexerException {
        testParseExpression("Comparison(l < r)", "l < r");
        testParseExpression("Comparison(l > r)", "l > r");
        testParseExpression("Comparison(l <= r)", "l <= r");
        testParseExpression("Comparison(l >= r)", "l >= r");

        testParseExpression("TypeCheck(l instanceof Object)", "l instanceof Object");

        testParseExpression("TypeCheck(Comparison(Comparison(Comparison(a < b) > c) <= d) instanceof e)", "a < b > c <= d instanceof e");
    }

    @Test
    public void testParseEqual() throws LexerException {
        testParseExpression("Comparison(l == r)", "l == r");
        testParseExpression("Comparison(l != r)", "l != r");

        testParseExpression("Comparison(Comparison(a == b) != c)", "a == b != c");
    }

    @Test
    public void testParseBitwiseAND() throws LexerException {
        testParseExpression("BitwiseLogic(l & r)", "l & r");

        testParseExpression("BitwiseLogic(BitwiseLogic(a & b) & c)", "a & b & c");
    }

    @Test
    public void testParseBitwiseXOR() throws LexerException {
        testParseExpression("BitwiseLogic(l ^ r)", "l ^ r");

        testParseExpression("BitwiseLogic(BitwiseLogic(a ^ b) ^ c)", "a ^ b ^ c");
    }

    @Test
    public void testParseBitwiseOR() throws LexerException {
        testParseExpression("BitwiseLogic(l | r)", "l | r");

        testParseExpression("BitwiseLogic(BitwiseLogic(a | b) | c)", "a | b | c");
    }

    @Test
    public void testParseBooleanAND() throws LexerException {
        testParseExpression("BooleanLogic(l && r)", "l && r");

        testParseExpression("BooleanLogic(BooleanLogic(a && b) && c)", "a && b && c");
    }

    @Test
    public void testParseBooleanOR() throws LexerException {
        testParseExpression("BooleanLogic(l || r)", "l || r");

        testParseExpression("BooleanLogic(BooleanLogic(a || b) || c)", "a || b || c");
    }

    @Test
    public void testParseConditional() throws LexerException {
        testParseExpression("Conditional(t ? l : r)", "t ? l : r");

        testParseExpression("Conditional(r ? Conditional(s ? u : v) : Conditional(t ? k : l))", "r ? s ? u : v : t ? k : l");
    }

    @Test
    public void testParseAssignment() throws LexerException {
        testParseExpression("Assignment(l = r)", "l = r");
        testParseExpression("Assignment(l += r)", "l += r");
        testParseExpression("Assignment(l -= r)", "l -= r");
        testParseExpression("Assignment(l *= r)", "l *= r");
        testParseExpression("Assignment(l /= r)", "l /= r");
        testParseExpression("Assignment(l %= r)", "l %= r");
        testParseExpression("Assignment(l <<= r)", "l <<= r");
        testParseExpression("Assignment(l >>= r)", "l >>= r");
        testParseExpression("Assignment(l >>>= r)", "l >>>= r");
        testParseExpression("Assignment(l &= r)", "l &= r");
        testParseExpression("Assignment(l ^= r)", "l ^= r");
        testParseExpression("Assignment(l |= r)", "l |= r");

        testParseExpression("Assignment(a = Assignment(b = c))", "a = b = c");
    }

    @Test
    public void testParseEmpty() throws LexerException {
        testParseStatement("Empty()", ";");
    }

    @Test
    public void testParsePriority() throws LexerException {
        testParseExpression("Assignment(x = " +
                        "Conditional(v ? y : " +
                        "BooleanLogic(a || " +
                        "BooleanLogic(d && " +
                        "BitwiseLogic(z | " +
                        "BitwiseLogic(g ^ " +
                        "BitwiseLogic(q & " +
                        "Comparison(t == " +
                        "Comparison(r >= " +
                        "Shift(p >> " +
                        "Add(n + " +
                        "Multiply(j * " +
                        "BooleanLogic(!" +
                        "MethodCall(m.k()" +
                        "))))))))))))))",
                "x = v ? y : a || d && z | g ^ q & t == r >= p >> n + j * !m.k()");
    }

    @Test
    public void testParseDeclaration() throws LexerException {
        testParseStatement("Declaration(Object m)", "Object m;");
        testParseStatement("Declaration(Object m, k, j)", "Object m, k, j;");

        testParseStatement("Declaration(Object m = 1)", "Object m = 1;");
        testParseStatement("Declaration(Object m = 1, k = 2, j = 3)", "Object m = 1, k = 2, j = 3;");
    }

    @Test
    public void testParseImport() throws LexerException {
        testParseStatement("Import(Object)", "import Object;");
        testParseStatement("Import(java.lang.Object)", "import java.lang.Object;");
        testParseStatement("Import(java.lang.*)", "import java.lang.*;");
    }

    @Test
    public void testParseStatementExpression() throws LexerException {
        testParseStatement("Assignment(m = 2)", "m = 2;");
        testParseStatement("Increment(m++)", "m++;");
        testParseStatement("ConstructorCall(new Test())", "new Test();");
        testParseStatement("MethodCall(test())", "test();");
    }

    private void testParseExpression(String expected, String source) throws LexerException {
        assertEqual(expected, Parser.parseExpression(Lexer.lex(source)));
    }

    private void testParseStatement(String expected, String source) throws LexerException {
        assertEqual(expected, Parser.parseStatement(Lexer.lex(source)));
    }

    private void assertEqual(String expected, Expression actual) {
        Assert.assertEquals(expected, actual.toString());
    }

    private void assertEqual(String expected, Statement actual) {
        Assert.assertEquals(expected, actual.toString());
    }
}
