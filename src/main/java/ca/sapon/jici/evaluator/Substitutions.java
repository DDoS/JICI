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
package ca.sapon.jici.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.sapon.jici.evaluator.type.TypeArgument;
import ca.sapon.jici.evaluator.type.TypeVariable;

/**
 *
 */
public class Substitutions {
    public static final Substitutions NONE = new Substitutions(Collections.<String, TypeArgument>emptyMap());
    private final Map<String, TypeArgument> substitutions;
    private final List<String> order;

    public Substitutions(Map<String, TypeArgument> substitutions) {
        this.substitutions = substitutions;
        order = new ArrayList<>(substitutions.size());
        reduce();
    }

    private void reduce() {
        // For each variable to be substituted, get the variables that it depends on
        final Map<String, Set<String>> variableDependencies = new HashMap<>();
        for (Map.Entry<String, TypeArgument> substitution : substitutions.entrySet()) {
            final Set<String> dependentVariables = new HashSet<>();
            for (TypeVariable typeVariable : substitution.getValue().getTypeVariables()) {
                // If there's no actual dependencies then the variable isn't to be substituted
                final String name = typeVariable.getDeclaredName();
                if (substitutions.containsKey(name)) {
                    dependentVariables.add(name);
                }
            }
            variableDependencies.put(substitution.getKey(), dependentVariables);
        }
        // Try to substitute the dependent variables, solving dependencies from the bottom up
        final Set<String> solvedVariables = new HashSet<>();
        boolean progressed;
        do {
            progressed = false;
            for (Iterator<Map.Entry<String, Set<String>>> iterator = variableDependencies.entrySet().iterator(); iterator.hasNext(); ) {
                final Map.Entry<String, Set<String>> entry = iterator.next();
                final Set<String> dependentVariables = entry.getValue();
                if (solvedVariables.containsAll(dependentVariables)) {
                    // All dependent variables are solved, so solve this one and add it to the solved set
                    final String variableName = entry.getKey();
                    if (!dependentVariables.isEmpty()) {
                        substitutions.put(variableName, substitutions.get(variableName).substituteTypeVariables(this));
                    }
                    solvedVariables.add(variableName);
                    order.add(variableName);
                    iterator.remove();
                    progressed = true;
                }
            }
        } while (progressed);
    }

    public List<String> getOrder() {
        return order;
    }

    public Map<String, TypeArgument> getMap() {
        return substitutions;
    }

    public Set<Map.Entry<String, TypeArgument>> getEntries() {
        return substitutions.entrySet();
    }

    public TypeArgument forVariable(TypeVariable variable) {
        return forVariable(variable.getDeclaredName());
    }

    public TypeArgument forVariable(String name) {
        return substitutions.get(name);
    }

    public static Map<String, TypeArgument> toSubstitutionMap(TypeVariable[] parameters, TypeArgument[] arguments) {
        if (parameters.length != arguments.length) {
            throw new IllegalArgumentException("Expected the parameter and argument arrays to be the same length");
        }
        final HashMap<String, TypeArgument> substitutions = new HashMap<>();
        for (int i = 0; i < arguments.length; i++) {
            substitutions.put(parameters[i].getDeclaredName(), arguments[i]);
        }
        return substitutions;
    }
}
