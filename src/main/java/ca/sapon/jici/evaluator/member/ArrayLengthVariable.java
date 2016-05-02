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
package ca.sapon.jici.evaluator.member;

import java.lang.reflect.Array;

import ca.sapon.jici.evaluator.type.LiteralReferenceType;
import ca.sapon.jici.evaluator.type.PrimitiveType;
import ca.sapon.jici.evaluator.type.Type;
import ca.sapon.jici.evaluator.value.IntValue;
import ca.sapon.jici.evaluator.value.Value;

/**
 *
 */
public class ArrayLengthVariable implements ClassVariable {
    private final LiteralReferenceType declaror;

    private ArrayLengthVariable(LiteralReferenceType declaror) {
        this.declaror = declaror;
    }

    @Override
    public LiteralReferenceType getDeclaringType() {
        return declaror;
    }

    @Override
    public String getName() {
        return "length";
    }

    @Override
    public Type getType() {
        return PrimitiveType.THE_INT;
    }

    @Override
    public Type getTargetType() {
        return PrimitiveType.THE_INT;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public Value getValue(Value target) {
        try {
            return IntValue.of(Array.getLength(target.asObject()));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void setValue(Value target, Value value) {
        throw new UnsupportedOperationException("Array length field is final");
    }

    @Override
    public String toString() {
        return "length";
    }

    public static ArrayLengthVariable of(LiteralReferenceType declaror) {
        if (!declaror.isArray()) {
            throw new IllegalArgumentException("Cannot declaror an array cloning method on the non-array type " + declaror);
        }
        return new ArrayLengthVariable(declaror);
    }
}
