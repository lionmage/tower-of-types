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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import tungsten.types.Set;
import tungsten.types.Numeric;

/**
 * Implementation of {@link Set} which is defined parametrically.
 * Since values are generated parametrically, there is an implicit
 * ordering of the values, which {@link #contains(java.lang.Object&tungsten.types.Numeric&java.lang.Comparable<T>) }
 * uses to do its thing.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the numeric type for this set
 */
public class ParametricSet<T extends Numeric & Comparable<T>> implements Set<T> {
    private final T seedValue;
    private final UnaryOperator<T> func;
    
    private ParametricSet() {
        seedValue = null;
        func = null;
    }
    
    /**
     * Create a new {@code ParametricSet} with an initial seed value and a
     * function to generate additional values.  Some underlying assumptions:
     * <ul>
     * <li>The value of {@code seed} is the lowest value contained in the set</li>
     * <li>The {@code function} produces no duplicate values</li>
     * <li>Further, {@code function} generates values that are monotonically increasing</li>
     * </ul>
     * @param seed an implementation of {@link Numeric} that is also {@link Comparable}
     * @param function a {@link UnaryOperator} that operates iteratively on {@link Numeric} values
     */
    public ParametricSet(T seed, UnaryOperator<T> function) {
        this.seedValue = seed;
        this.func = function;
    }
    
    protected UnaryOperator<T> getFunction() {
        return func;
    }
    
    protected T getSeed() {
        return seedValue;
    }

    @Override
    public long cardinality() {
        return -1L;
    }

    @Override
    public boolean countable() {
        return true;
    }

    @Override
    public boolean contains(T element) {
        Iterator<T> iter = iterator();
        while (iter.hasNext()) {
            T val = iter.next();
            if (val.compareTo(element) < 0) continue;
            if (val.compareTo(element) == 0) return true;
            // otherwise, bail out of the loop
            break;
        }
        return false;
    }

    @Override
    public void append(T element) {
        throw new UnsupportedOperationException("Cannot append to a ParametricSet.");
    }

    @Override
    public void remove(T element) {
        throw new UnsupportedOperationException("Cannot remove values from a ParametricSet.");
    }

    @Override
    public Set<T> union(Set<T> other) {
        if (other instanceof ParametricSet) {
            ParametricSet<T> that = (ParametricSet<T>) other;
            
            // construct a new ParametricSet that generates the union of these twp sets
            ParametricSet<T> first = this.seedValue.compareTo(that.getSeed()) < 0 ? this : that;
            ParametricSet<T> second = this.seedValue.compareTo(that.getSeed()) >= 0 ? that : this;
            final Supplier<T> merged = new Supplier<T>() {
                T firstNext = first.getSeed();
                T secondNext = second.getSeed();

                @Override
                public T get() {
                    T temp;
                    
                    if (firstNext.compareTo(secondNext) <= 0) {
                        // in case of a tie, first wins
                        temp = firstNext;
                        firstNext = first.getFunction().apply(firstNext);
                    } else {
                        temp = secondNext;
                        secondNext = second.getFunction().apply(secondNext);
                    }
                    return temp;
                }
            };
            return new ParametricSet() {
                @Override
                public Stream<T> stream() {
                    // TODO this needs to be seriously tested
                    return Stream.generate(merged).distinct().sorted();
                }
            };
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<T> intersection(Set<T> other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<T> difference(Set<T> other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public Stream<T> stream() {
        return Stream.iterate(seedValue, func);
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }
}
