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
package ca.sapon.jici.util;

import ca.sapon.jici.evaluator.value.type.ObjectValueType;
import ca.sapon.jici.evaluator.value.type.PrimitiveValueType;

/**
 *
 */
public class ReflectionUtil {
    public static Class<?> lookupClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    public static boolean isNarrower(Class<?> a, Class<?> b) {
        if (a.isPrimitive()) {
            // a !=b and a <= b
            return a != b && PrimitiveValueType.convertibleTo(a, b);
        }
        // a < and !a.boxOf(b)
        return !a.isAssignableFrom(b) && ObjectValueType.unbox(a) != b;
    }
}
