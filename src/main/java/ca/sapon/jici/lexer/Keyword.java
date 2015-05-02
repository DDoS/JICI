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
package ca.sapon.jici.lexer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Keyword extends Token {
    private static final Map<String, Keyword> KEYWORDS = new HashMap<>();

    static {
        // control flow
        add(TokenID.KEYWORD_ASSERT, "assert");
        add(TokenID.KEYWORD_IF, "if");
        add(TokenID.KEYWORD_ELSE, "else");
        add(TokenID.KEYWORD_WHILE, "while");
        add(TokenID.KEYWORD_DO, "do");
        add(TokenID.KEYWORD_FOR, "for");
        add(TokenID.KEYWORD_BREAK, "break");
        add(TokenID.KEYWORD_CONTINUE, "continue");
        add(TokenID.KEYWORD_SWITCH, "switch");
        add(TokenID.KEYWORD_CASE, "case");
        add(TokenID.KEYWORD_DEFAULT, "default");
        add(TokenID.KEYWORD_RETURN, "return");
        add(TokenID.KEYWORD_THROW, "throw");
        add(TokenID.KEYWORD_TRY, "try");
        add(TokenID.KEYWORD_CATCH, "catch");
        add(TokenID.KEYWORD_FINALLY, "finally");
        // void type
        add(TokenID.KEYWORD_VOID, "void");
        // primitive type
        add(TokenID.KEYWORD_BOOLEAN, "boolean");
        add(TokenID.KEYWORD_BYTE, "byte");
        add(TokenID.KEYWORD_SHORT, "short");
        add(TokenID.KEYWORD_CHAR, "char");
        add(TokenID.KEYWORD_INT, "int");
        add(TokenID.KEYWORD_LONG, "long");
        add(TokenID.KEYWORD_FLOAT, "float");
        add(TokenID.KEYWORD_DOUBLE, "double");
        // class type
        add(TokenID.KEYWORD_CLASS, "class");
        add(TokenID.KEYWORD_INTERFACE, "interface");
        add(TokenID.KEYWORD_ENUM, "enum");
        // inheritence
        add(TokenID.KEYWORD_EXTENDS, "extends");
        add(TokenID.KEYWORD_IMPLEMENTS, "implements");
        // identifier
        add(TokenID.KEYWORD_SUPER, "super");
        add(TokenID.KEYWORD_THIS, "this");
        // package
        add(TokenID.KEYWORD_PACKAGE, "package");
        // import
        add(TokenID.KEYWORD_IMPORT, "import");
        // access modifier
        add(TokenID.KEYWORD_PUBLIC, "public");
        add(TokenID.KEYWORD_PROTECTED, "protected");
        add(TokenID.KEYWORD_PRIVATE, "private");
        // exception modifier
        add(TokenID.KEYWORD_THROWS, "throws");
        // other modifier
        add(TokenID.KEYWORD_ABSTRACT, "abstract");
        add(TokenID.KEYWORD_STRICTFP, "strictfp");
        add(TokenID.KEYWORD_TRANSIENT, "transient");
        add(TokenID.KEYWORD_VOLATILE, "volatile");
        add(TokenID.KEYWORD_FINAL, "final");
        add(TokenID.KEYWORD_STATIC, "static");
        add(TokenID.KEYWORD_SYNCHRONIZED, "synchronized");
        add(TokenID.KEYWORD_NATIVE, "native");
        // operator
        add(TokenID.KEYWORD_NEW, "new");
        add(TokenID.KEYWORD_INSTANCEOF, "instanceof");
        // unused
        add(TokenID.KEYWORD_GOTO, "goto");
        add(TokenID.KEYWORD_CONST, "const");
    }

    private Keyword(TokenID id, String source) {
        super(id, source);
    }

    public static boolean is(String source) {
        return KEYWORDS.containsKey(source);
    }

    public static Keyword from(String source) {
        return KEYWORDS.get(source);
    }

    public static Collection<Keyword> all() {
        return Collections.unmodifiableCollection(KEYWORDS.values());
    }

    private static void add(TokenID id, String source) {
        KEYWORDS.put(source, new Keyword(id, source));
    }
}
