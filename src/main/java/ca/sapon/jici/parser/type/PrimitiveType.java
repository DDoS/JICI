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
package ca.sapon.jici.parser.type;

import ca.sapon.jici.evaluator.Environment;
import ca.sapon.jici.evaluator.EvaluatorException;
import ca.sapon.jici.evaluator.value.type.PrimitiveValueType;
import ca.sapon.jici.evaluator.value.type.ValueType;
import ca.sapon.jici.evaluator.value.type.VoidValueType;
import ca.sapon.jici.lexer.Keyword;

public class PrimitiveType implements Type {
    private final Keyword type;
    private ValueType valueType = null;

    public PrimitiveType(Keyword type) {
        this.type = type;
    }

    @Override
    public ValueType getValueType(Environment environment) {
        if (valueType == null) {
            final Class<?> _class;
            switch (type.getID()) {
                case KEYWORD_BOOLEAN:
                    _class = boolean.class;
                    break;
                case KEYWORD_BYTE:
                    _class = byte.class;
                    break;
                case KEYWORD_SHORT:
                    _class = short.class;
                    break;
                case KEYWORD_CHAR:
                    _class = char.class;
                    break;
                case KEYWORD_INT:
                    _class = int.class;
                    break;
                case KEYWORD_LONG:
                    _class = long.class;
                    break;
                case KEYWORD_FLOAT:
                    _class = float.class;
                    break;
                case KEYWORD_DOUBLE:
                    _class = double.class;
                    break;
                case KEYWORD_VOID:
                    _class = void.class;
                    break;
                default:
                    throw new EvaluatorException("Not a primitive type: " + type, getStart(), getEnd());
            }
            valueType = _class == void.class ? VoidValueType.THE_VOID : PrimitiveValueType.of(_class);
        }
        return valueType;
    }

    @Override
    public int getStart() {
        return type.getStart();
    }

    @Override
    public int getEnd() {
        return type.getEnd();
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
