/*
 * The MIT License
 *
 * Copyright © 2018 Robert Poole <Tarquin.AZ@gmail.com>.
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
package tungsten.types.set.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tungsten.types.Numeric;
import tungsten.types.Set;
import tungsten.types.exceptions.CoercionException;

/**
 * Implementation of {@link Set} for {@link Numeric} values.  If created using
 * the no-args constructor, this {@link Set} will attempt to preserve ordering
 * equivalent to the insertion order.  (This is useful if building a set of
 * {@link ComplexType} values, where insertion order may be critical but the
 * values themselves have no natural ordering.)  If the copy constructor is
 * used, there are no ordering guarantees.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class NumericSet implements Set<Numeric> {
    private final java.util.Set<Numeric> internal;
    
    public NumericSet() {
        internal = new LinkedHashSet<>();
    }
    
    public NumericSet(Collection<? extends Numeric> c) {
        if (c instanceof java.util.Set) {
            // no need for extra object copying in this case
            internal = (java.util.Set) c;
        } else {
            internal = new HashSet<>();
            internal.addAll(c);
        }
    }

    @Override
    public long cardinality() {
        return (long) internal.size();
    }

    @Override
    public boolean countable() {
        return true;
    }

    @Override
    public boolean contains(Numeric element) {
        return internal.contains(element);
    }

    @Override
    public void append(Numeric element) {
        if (!internal.add(element)) {
            Logger.getLogger(NumericSet.class.getName()).log(Level.FINER, "Attempted to append {0}, but set already contains this value.", element);
        }
    }

    @Override
    public void remove(Numeric element) {
        if (!internal.remove(element)) {
            Logger.getLogger(NumericSet.class.getName()).log(Level.FINER, "Attempted to renove {0}, but set does not contain this value.", element);
        }
    }

    @Override
    public Set<Numeric> union(Set<Numeric> other) {
        HashSet<Numeric> union = new HashSet<>(internal);
        
        Iterator<Numeric> iter = other.iterator();
        while (iter.hasNext()) {
            union.add(iter.next());
        }
        
        return new NumericSet(union);
    }

    @Override
    public Set<Numeric> intersection(Set<Numeric> other) {
        HashSet<Numeric> intersec = new HashSet<>();
        
        for (Numeric element : internal) {
            // only store values that exist in this and the other
            if (other.contains(element)) {
                intersec.add(element);
            }
        }
        
        return new NumericSet(intersec);
    }

    @Override
    public Set<Numeric> difference(Set<Numeric> other) {
        HashSet<Numeric> diff = new HashSet<>(internal); // create a copy
        
        Iterator<Numeric> iter = other.iterator();
        while (iter.hasNext()) {
            Numeric element = iter.next();
            // note we're checking the internal store, then removing from the copy
            if (internal.contains(element)) {
                diff.remove(element);
            }
        }
        
        return new NumericSet(diff);
    }
    
    /**
     * Coerce the elements of the parent set to type {@code T} and insert
     * them into a {@link Set<T>}, which is returned to the caller.
     * If the elements of the coerced type have a natural ordering, the
     * resulting set will be sorted according to that ordering, and the
     * {@link Set<T>#iterator() } will return elements in that order.
     * Otherwise, the returned set will attempt to preserve the insertion
     * order of the parent set.
     * @param <T> the desired subtype of {@link Numeric}
     * @param clazz the {@link Class} representing the desired subtype
     * @return an unmodifiable {@link Set} representing the elements of the parent set
     * @throws CoercionException if any elements in the parent set cannot be coerced to {@code T}
     */
    public <T extends Numeric> Set<T> coerceTo(Class<T> clazz) throws CoercionException {
        final java.util.Set<T> innerSet = Comparable.class.isAssignableFrom(clazz) ? new TreeSet<>() : new LinkedHashSet<>();
        for (Numeric element : internal) {
            if (!element.isCoercibleTo(clazz)) {
                throw new CoercionException("Element of NumericSet cannot be coerced to target type.", element.getClass(), clazz);
            }
            innerSet.add((T) element.coerceTo(clazz));
        }
        
        return new Set<T>() {
            private final java.util.Set<T> elements = Collections.unmodifiableSet(innerSet);
            
            @Override
            public long cardinality() {
                return (long) elements.size();
            }

            @Override
            public boolean countable() {
                return true;
            }

            @Override
            public boolean contains(T element) {
                return elements.contains(element);
            }

            @Override
            public void append(T element) {
                throw new UnsupportedOperationException("Cannot modify this view.");
            }

            @Override
            public void remove(T element) {
                throw new UnsupportedOperationException("Cannot modify this view.");
            }

            @Override
            public Set<T> union(Set<T> other) {
                if (other.countable()) {
                    LinkedHashSet<T> temp = new LinkedHashSet<>(elements);
                    other.forEach(temp::add);
                    return (Set<T>) new NumericSet(temp);
                }
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set<T> intersection(Set<T> other) {
                if (other.countable()) {
                    java.util.Set<T> temp = elements.stream().filter(other::contains).collect(Collectors.toSet());
                    return (Set<T>) new NumericSet(temp);
                }
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set<T> difference(Set<T> other) {
                if (other.countable()) {
                    LinkedHashSet<T> temp = new LinkedHashSet<>(elements);
                    temp.removeIf(other::contains);
                    return (Set<T>) new NumericSet(temp);
                }
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Iterator<T> iterator() {
                return elements.iterator();
            }
            
            public Stream<T> stream() {
                return elements.stream();
            }
            
            public Stream<T> parallelStream() {
                return elements.parallelStream();
            }
        };
    } 
    
    /**
     * Since ordering in sets isn't guaranteed (especially this implementation),
     * we really only need to expose a parallel {@link Stream}.
     * @return a parallel stream of this set's elements
     */
    public Stream<Numeric> parallelStream() {
        return internal.parallelStream();
    }

    @Override
    public Iterator<Numeric> iterator() {
        return internal.iterator();
    }
}
