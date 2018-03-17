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
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import tungsten.types.Numeric;
import tungsten.types.Set;

/**
 * Implementation of {@link Set} for {@link Numeric} values.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class NumericSet implements Set<Numeric> {
    private final java.util.Set<Numeric> internal;
    
    public NumericSet() {
        internal = new HashSet<>();
    }
    
    public NumericSet(Collection<Numeric> c) {
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