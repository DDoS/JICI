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
package ca.sapon.jici;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import ca.sapon.jici.decoder.Decoder;
import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.Value;
import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.Token;
import ca.sapon.jici.lexer.TokenID;
import ca.sapon.jici.parser.Parser;
import ca.sapon.jici.parser.expression.Expression;
import ca.sapon.jici.parser.statement.Statement;

/**
 *
 */
public final class JICI {
    private final Thread thread;
    private final JICIREPL repl;
    private final Environment environment;

    private JICI(Thread thread, JICIREPL repl, Environment environment) {
        this.thread = thread;
        this.repl = repl;
        this.environment = environment;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void kill() {
        repl.stop();
    }

    public void join() {
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static JICI breakInto() {
        return breakInto(System.in, System.out);
    }

    public static JICI breakInto(InputStream input, OutputStream output) {
        final Environment environment = new Environment();
        final JICIREPL repl = new JICIREPL(environment, input, output);
        repl.run();
        return new JICI(Thread.currentThread(), repl, environment);
    }

    public static JICI spawn() {
        return spawn(System.in, System.out);
    }

    public static JICI spawn(InputStream input, OutputStream output) {
        final Environment environment = new Environment();
        final JICIREPL repl = new JICIREPL(environment, input, output);
        final Thread thread = new Thread(repl);
        thread.start();
        return new JICI(thread, repl, environment);
    }

    private static class JICIREPL implements Runnable {
        private final Scanner input;
        private final PrintStream output;
        private final Environment environment;
        private volatile boolean running = false;

        private JICIREPL(Environment environment, InputStream input, OutputStream output) {
            this.environment = environment;
            this.input = new Scanner(input);
            this.output = output instanceof PrintStream ? (PrintStream) output : new PrintStream(output);
        }

        public void stop() {
            this.running = false;
        }

        @Override
        public void run() {
            running = true;
            while (running && input.hasNextLine()) {
                eval(environment, input.nextLine());
            }
            input.close();
        }

        private void eval(Environment environment, String source) {
            final SourceMetadata metadata = new SourceMetadata(source);
            try {
                source = Decoder.decode(source, metadata);
                final List<Token> tokens = Lexer.lex(source);
                if (tokens.size() <= 0) {
                    return;
                }
                if (tokens.get(tokens.size() - 1).getID() == TokenID.SYMBOL_SEMICOLON) {
                    final List<Statement> statements = Parser.parse(tokens);
                    for (Statement statement : statements) {
                        statement.execute(environment);
                    }
                } else {
                    final Expression expression = Parser.parseExpression(tokens);
                    final Type type = expression.getType(environment);
                    final Value value = expression.getValue(environment);
                    output.println("Type: " + type.getName());
                    output.println("Value: " + value.asString());
                }
            } catch (SourceException exception) {
                output.println(metadata.generateErrorInformation(exception));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
