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
package ca.sapon.jici.evaluator.value.type;

/**
 *
 */
public class ObjectUnionValueType implements ValueType {
    private final ObjectValueType type1;
    private final ObjectValueType type2;
    private Class<?> upperBound;

    public ObjectUnionValueType(ObjectValueType type1, ObjectValueType type2) {
        this.type1 = type1;
        this.type2 = type2;
        upperBound = Object.class;
    }

    public void setUpperBound(Class<?> upperBound) {
        this.upperBound = upperBound;
    }

    @Override
    public Class<?> getClassType() {
        return upperBound;
    }

    @Override
    public String getName() {
        return upperBound.getCanonicalName();
    }

    @Override
    public boolean is(Class<?> type) {
        return upperBound == type;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isLogical() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public ValueType unbox() {
        return this;
    }

    @Override
    public boolean canNarrowTo(int value) {
        return false;
    }

    @Override
    public PrimitiveValueType unaryWiden() {
        return PrimitiveValueType.of(ObjectValueType.unaryWiden(upperBound));
    }

    @Override
    public PrimitiveValueType binaryWiden(Class<?> with) {
        return PrimitiveValueType.of(ObjectValueType.binaryWiden(upperBound, with));
    }

    @Override
    public boolean convertibleTo(Class<?> to) {
        return ObjectValueType.convertibleTo(upperBound, to);
    }
}
