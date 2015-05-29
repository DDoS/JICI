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
package ca.sapon.jici.evaluator;

import ca.sapon.jici.SourceException;
import ca.sapon.jici.SourceIndexed;

/**
 *
 */
public class EvaluatorException extends SourceException {
    private static final long serialVersionUID = 1;

    public EvaluatorException(Throwable cause, SourceIndexed indexed) {
        this("EvaluatorException", cause, indexed);
    }

    public EvaluatorException(String error, Throwable cause, SourceIndexed indexed) {
        super(getMessage(error, cause), cause, null, indexed.getStart(), indexed.getEnd());
    }

    public EvaluatorException(String error, SourceIndexed indexed) {
        this(error, indexed.getStart(), indexed.getEnd());
    }

    public EvaluatorException(String error, int start, int end) {
        super(error, null, start, end);
    }

    private static String getMessage(String error, Throwable cause) {
        if (cause == null) {
            return error;
        }
        String causeMessage = cause.getMessage();
        if (causeMessage != null) {
            causeMessage = causeMessage.trim();
            if (causeMessage.isEmpty()) {
                causeMessage = null;
            }
        }
        error += " (" + cause.getClass().getSimpleName();
        if (causeMessage != null) {
            error += ": " + causeMessage;
        }
        return getMessage(error, cause.getCause()) + ")";
    }
}
