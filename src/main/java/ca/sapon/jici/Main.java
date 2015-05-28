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

public class Main {
    public static void main(String[] args) {
        System.out.println("JICI\n\n");
        final Environment environment = new Environment();
        final Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            eval(environment, scanner.nextLine());
        }
        scanner.close();
    }

    private static void eval(Environment environment, String source) {
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
                final Type type = expression.getType(environment);
                final Value value = expression.getValue(environment);
                System.out.println("Type: " + type.getName());
                System.out.println("Value: " + value.asString());
            }
        } catch (SourceException exception) {
            System.out.println(metadata.generateErrorMessage(exception));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
