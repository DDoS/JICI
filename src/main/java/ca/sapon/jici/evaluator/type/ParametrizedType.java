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
package ca.sapon.jici.evaluator.type;

import java.util.List;

import ca.sapon.jici.util.StringUtil;
import ca.sapon.jici.util.TypeUtil;

/**
 *
 */
public class ParametrizedType extends SingleClassType {
    private final SingleClassTypeLiteral raw;
    private final List<TypeParameter> parameters;

    private ParametrizedType(SingleClassTypeLiteral raw, List<TypeParameter> parameters) {
        this.raw = raw;
        this.parameters = parameters;
    }

    @Override
    public String getName() {
        Class<?> _class = raw.getTypeClass();
        int dimensions = 0;
        while (_class.isArray()) {
            _class = _class.getComponentType();
            dimensions++;
        }
        return _class.getCanonicalName() + '<' + StringUtil.toString(parameters, ", ") + '>' + StringUtil.repeat("[]", dimensions);
    }

    @Override
    public Class<?> getTypeClass() {
        return raw.getTypeClass();
    }

    public SingleClassTypeLiteral getRaw() {
        return raw;
    }

    public List<TypeParameter> getParameters() {
        return parameters;
    }

    @Override
    public SingleClassType asArray(int dimensions) {
        return new ParametrizedType(raw.asArray(dimensions), parameters);
    }

    @Override
    public boolean convertibleTo(Type to) {
        return TypeUtil.convertibleTo(this, to);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ParametrizedType) {
            final ParametrizedType that = (ParametrizedType) other;
            return raw.equals(that.raw) && parameters.equals(that.parameters);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + parameters.hashCode();
    }

    public static ParametrizedType of(Class<?> raw, List<TypeParameter> parameters) {
        return new ParametrizedType(SingleClassTypeLiteral.of(raw), parameters);
    }
}
