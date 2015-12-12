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
