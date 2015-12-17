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

import ca.sapon.jici.SourceException;
import ca.sapon.jici.SourceMetadata;
import ca.sapon.jici.decoder.Decoder;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.parser.Parser;
import ca.sapon.jici.parser.statement.Statement;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class GenericsTest {
    @Test
    public void testWildcards() {
        // Subtype assignable to upper-bound
        assertSucceeds("import java.util.List;" +
                "List<String> l1 = null;" +
                "List<? extends CharSequence> l2;" +
                "l2 = l1;");
        assertFails("import java.util.List;" +
                "List<CharSequence> l1 = null;" +
                "List<? extends String> l2;" +
                "l2 = l1;");
        // Supertype assignable to lower-bound
        assertSucceeds("import java.util.List;" +
                "List<CharSequence> l1 = null;" +
                "List<? super String> l2;" +
                "l2 = l1;");
        assertFails("import java.util.List;" +
                "List<String> l1 = null;" +
                "List<? super CharSequence> l2;" +
                "l2 = l1;");
        // Unbounded assignable to unbounded
        assertSucceeds("import java.util.List;" +
                "List<?> l1 = null;" +
                "List<?> l2;" +
                "l2 = l1;");
        // Upper-bounded assignable to higher upper-bounded
        assertSucceeds("import java.util.List;" +
                "List<? extends String> l1 = null;" +
                "List<? extends CharSequence> l2;" +
                "l2 = l1;");
        assertFails("import java.util.List;" +
                "List<? extends CharSequence> l1 = null;" +
                "List<? extends String> l2;" +
                "l2 = l1;");
        // Lower-bounded assignable to lower lower-bounded
        assertSucceeds("import java.util.List;" +
                "List<? super CharSequence> l1 = null;" +
                "List<? super String> l2;" +
                "l2 = l1;");
        assertFails("import java.util.List;" +
                "List<? super String> l1 = null;" +
                "List<? super CharSequence> l2;" +
                "l2 = l1;");
        // Similar as a above but with bounded and unbounded
        assertSucceeds("import java.util.List;" +
                "List<? extends String> l1 = null;" +
                "List<?> l2;" +
                "l2 = l1;");
        assertSucceeds("import java.util.List;" +
                "List<? super String> l1 = null;" +
                "List<?> l2;" +
                "l2 = l1;");
        assertFails("import java.util.List;" +
                "List<?> l1 = null;" +
                "List<? extends String> l2;" +
                "l2 = l1;");
        assertFails("import java.util.List;" +
                "List<?> l1 = null;" +
                "List<? super String> l2;" +
                "l2 = l1;");
    }

    private static void assertSucceeds(String source) {
        final Environment environment = new Environment();
        final SourceMetadata metadata = new SourceMetadata(source);
        try {
            source = Decoder.decode(source, metadata);
            final List<Token> tokens = Lexer.lex(source);
            final List<Statement> statements = Parser.parse(tokens);
            for (Statement statement : statements) {
                statement.execute(environment);
            }
        } catch (Exception exception) {
            if (exception instanceof SourceException) {
                System.out.println(metadata.generateErrorInformation((SourceException) exception));
            }
            throw new AssertionError(exception);
        }
    }

    private static void assertFails(String source) {
        try {
            assertSucceeds(source);
            Assert.fail("Expected evaluator exception");
        } catch (AssertionError expected) {
            if (!(expected.getCause() instanceof EvaluatorException)) {
                Assert.fail("Expected evaluator exception");
            }
        }
    }
}
