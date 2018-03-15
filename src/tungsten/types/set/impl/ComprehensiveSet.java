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

import java.util.Iterator;
import java.util.function.Predicate;
import tungsten.types.Set;
import tungsten.types.Numeric;

/**
 * Allows the construction of a set by comprehension.  In other words,
 * allows the creation of a {@link Set} of the form "the set of all x
 * such that (some predicate) is true."  Such sets are not countable
 * by default, and therefore not iterable.  Subclasses which are restricted
 * to {@link tungsten.types.numerics.IntegerType} may override this
 * behavior.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the numeric type of this set
 */
public class ComprehensiveSet<T extends Numeric> implements Set<T> {
    private final Predicate<T> predicate;
    
    public ComprehensiveSet(Predicate<T> predicate) {
        this.predicate = predicate;
    }
    
    protected Predicate<T> getPredicate() {
        return predicate;
    }

    @Override
    public long cardinality() {
        return -1L;
    }

    @Override
    public boolean countable() {
        return false;
    }

    @Override
    public boolean contains(T element) {
        return predicate.test(element);
    }

    @Override
    public void append(T element) {
        throw new UnsupportedOperationException("Cannot add individual elements to this set.");
    }

    @Override
    public void remove(T element) {
        throw new UnsupportedOperationException("Cannot remove elements from this set.");
    }

    @Override
    public Set<T> union(Set<T> other) {
        if (other instanceof ComprehensiveSet) {
            final Predicate<T> original = this.getPredicate();
            ComprehensiveSet<T> that = (ComprehensiveSet<T>) other;
            final Predicate<T> composed = (T t) -> original.test(t) || that.getPredicate().test(t);
            return new ComprehensiveSet(composed);
        }
        throw new UnsupportedOperationException("Cannot take union with " + other.getClass().getTypeName());
    }

    @Override
    public Set<T> intersection(Set<T> other) {
        if (other instanceof ComprehensiveSet) {
            final Predicate<T> original = this.getPredicate();
            ComprehensiveSet<T> that = (ComprehensiveSet<T>) other;
            final Predicate<T> composed = (T t) -> original.test(t) && that.getPredicate().test(t);
            return new ComprehensiveSet(composed);
        }
        throw new UnsupportedOperationException("Cannot take intersection with " + other.getClass().getTypeName());
    }

    @Override
    public Set<T> difference(Set<T> other) {
        if (other instanceof ComprehensiveSet) {
            final Predicate<T> original = this.getPredicate();
            ComprehensiveSet<T> that = (ComprehensiveSet<T>) other;
            final Predicate<T> composed = (T t) -> original.test(t) && !that.getPredicate().test(t);
            return new ComprehensiveSet(composed);
        }
        throw new UnsupportedOperationException("Cannot take difference with " + other.getClass().getTypeName());
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("Cannot iterate over this ComprehensiveSet."); //To change body of generated methods, choose Tools | Templates.
    }
}
