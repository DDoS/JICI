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
package ca.sapon.jici;

import ca.sapon.jici.lexer.Lexer;
import ca.sapon.jici.lexer.Lexer.LexerException;
import ca.sapon.jici.lexer.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("JICI\n\n");

        /*

          Object obj = new Object();
          obj.hashCode();

          String test = "this is \t a \"test\"";
          System.out.println(test);

          int num = 13;
          System.out.println(num);

        */

        final String source =
            "Object obj = new Object();\n" +
            "obj.hashCode();\n" +

            "String test = \"this is \\t a \\\"test\\\"\";\n" +
            "System.out.println(test);\n" +

            "int num = 13;\n" +
            "System.out.println(num);\n"
        ;

        System.out.println("Lexing:\n" + source);

        final List<Token> tokens;
        try {
            tokens = Lexer.lex(source);

            for (Token token : tokens) {
                System.out.println(token.getClass().getSimpleName() + ": " + token.getSource());
            }
        } catch (LexerException exception) {
            exception.printStackTrace();
        }
    }
}
