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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
     * <li>The value of {@code seed} is either the lowest or highest value contained in the set</li>
     * <li>The {@code function} produces no duplicate values</li>
     * <li>Further, {@code function} generates values that are monotonic</li>
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
        int direction = monotonicity();
        if (direction == 0 && seedValue != null) {
            return seedValue.compareTo(element) == 0;
        }
        // the stream is infinite, and since takeWhile and dropWhile are only
        // implemented in JDK 9 and we're using Java 8, we'll use an iterator
        
        Iterator<T> iter = iterator();
        while (iter.hasNext()) {
            T val = iter.next();
            if (Integer.signum(val.compareTo(element)) == -direction) continue;
            if (val.compareTo(element) == 0) return true;
            // otherwise, bail out of the loop
            break;
        }
        return false;
    }
    
    /**
     * Returns -1 if the elements of this set are monotonically decreasing,
     * 1 if they are monotonically increasing, or 0 if the set contains
     * repeating elements.
     * 
     * @return the monotonicity of this set's elements
     */
    public int monotonicity() {
        T x0 = seedValue;
        T x1 = func.apply(x0);
        
        return Integer.signum(x1.compareTo(x0));
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
            ParametricSet<T> parent = this;
            ParametricSet<T> that = (ParametricSet<T>) other;
            if (this.monotonicity() != that.monotonicity()) {
                throw new UnsupportedOperationException("Cannot merge two sets with different monotonicity.");
            }
            final int direction = this.monotonicity();
            
            // construct a new ParametricSet that generates the union of these twp sets
            return new ParametricSet<T>() {
                @Override
                public Stream<T> stream() {
                    MergeSupplier merged = new MergeSupplier(parent, that, direction);
                    return Stream.generate(merged);
                }
                @Override
                public int monotonicity() {
                    return direction;
                }
            };
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class MergeSupplier implements Supplier<T> {
        final Iterator<T> iter1;
        final Iterator<T> iter2;
        Optional<T> cache1;
        Optional<T> cache2;
        final int direction;

        public MergeSupplier(ParametricSet<T> set1, ParametricSet<T> set2, int direction) {
            cache1 = Optional.empty();
            cache2 = Optional.empty();
            iter1 = set1.iterator();
            iter2 = set2.iterator();
            this.direction = direction;
        }

        @Override
        public T get() {
            // both are infinite sets, so we never have to test with hasNext()
            T h1 = cache1.isPresent() ? cache1.get() : iter1.next();
            T h2 = cache2.isPresent() ? cache2.get() : iter2.next();
            if (Integer.signum(h1.compareTo(h2)) == -direction) {
                if (!cache2.isPresent()) cache2 = Optional.of(h2);
                cache1 = Optional.empty();
                return h1;
            } else if (Integer.signum(h2.compareTo(h1)) == -direction) {
                if (!cache1.isPresent()) cache1 = Optional.of(h1);
                cache2 = Optional.empty();
                return h2;
            } else {
                // values are equal, so return a single value
                cache1 = Optional.empty();
                cache2 = Optional.empty();
                return h1;
            }
        }
    }
    
    /**
     * Perform an intersection operation between this parametric set
     * and another set.  Note that this implementation assumes that
     * {@code other} is a finite set, otherwise the implementation would
     * be a nightmare.
     * @param other the {@link Set} we wish to take an intersection with
     * @return the intersection of two sets
     */
    @Override
    public Set<T> intersection(Set<T> other) {
        final ParametricSet<T> parent = this;
        final int direction = monotonicity();
        final T maxElement = StreamSupport.stream(other.spliterator(), false).max(T::compareTo).get();
        final T minElement = StreamSupport.stream(other.spliterator(), false).min(T::compareTo).get();

        return new ParametricSet<T>() {
            @Override
            public boolean contains(T element) {
                switch (direction) {
                    case 0:
                        return other.contains(parent.getSeed());
                    case -1:
                        if (element.compareTo(minElement) < 0) return false;
                        break;
                    case 1:
                        if (element.compareTo(maxElement) > 0) return false;
                        break;
                }
                return super.contains(element);
            }
            @Override
            public Stream<T> stream() {
                return parent.stream().filter(x -> other.contains(x));
            }
            @Override
            public int monotonicity() {
                return direction;
            }
        };
    }

    /**
     * Take the difference between this set and the other.  The
     * result should be all the elements in this set but not in the {@code other}.
     * @param other the set with which to take the difference
     * @return the difference between this set and the other
     */
    @Override
    public Set<T> difference(Set<T> other) {
        final ParametricSet<T> parent = this;
        final int direction = monotonicity();

        return new ParametricSet<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Iterator<T> fromParent = parent.stream().filter(x -> !other.contains(x)).iterator();
                    
                    @Override
                    public boolean hasNext() {
                        return fromParent.hasNext();
                    }

                    @Override
                    public T next() {
                        return fromParent.next();
                    }
                    
                };
            }
            @Override
            public Stream<T> stream() {
                Iterable<T> iterable = () -> iterator();
                return StreamSupport.stream(iterable.spliterator(), false);
//                return parent.stream().filter(x -> !other.contains(x));
            }
            @Override
            public int monotonicity() {
                return direction;
            }
        };
    }
    
    public Stream<T> stream() {
        return Stream.iterate(seedValue, func);
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }
}
