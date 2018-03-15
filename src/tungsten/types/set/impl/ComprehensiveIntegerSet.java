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

import java.math.BigInteger;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import tungsten.types.Range;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.impl.IntegerImpl;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> any instance of {@link IntegerType}
 */
public class ComprehensiveIntegerSet<T extends IntegerType> extends ComprehensiveSet<T> {
    private final Range<T> range;
    private Predicate<T> additional = null;
    
    /**
     * Construct a comprehensive set of integers that satisfy the given
     * {@link Range}.
     * @param range the range of values describing the elements of this set
     */
    public ComprehensiveIntegerSet(Range<T> range) {
        super(range.getPredicate());
        this.range = range;
    }
    
    /**
     * Construct a comprehensive set of integers that fall within the given
     * {@link Range} and the {@code additional} predicate.
     * @param range the range of values describing the elements of this set
     * @param additional an additional predicate further constraining the values in this set
     */
    public ComprehensiveIntegerSet(Range<T> range, Predicate<T> additional) {
        this(range);
        this.additional = additional;
    }
    
    @Override
    protected Predicate<T> getPredicate() {
        if (additional == null) {
            return super.getPredicate();
        }
        return super.getPredicate().and(additional);
    }
    
    @Override
    public boolean countable() {
        return true;
    }
    
    @Override
    public long cardinality() {
        return StreamSupport.stream(this.spliterator(), false).count();
    }
    
    @Override
    public boolean contains(T element) {
        boolean c = super.contains(element);
        if (additional != null) {
            return c && additional.test(element);
        }
        return c;
    }
    
    private static final IntegerType ONE = new IntegerImpl(BigInteger.ONE);
    
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            final T lower = range.isLowerClosed() ? range.getLowerBound() : (T) range.getLowerBound().add(ONE);
            final T upper = range.isUpperClosed() ? range.getUpperBound() : (T) range.getUpperBound().subtract(ONE);
            T current = lower;
            
            @Override
            public boolean hasNext() {
                return current.compareTo(upper) <= 0;
            }

            @Override
            public T next() {
                T temp = current;
                do {
                    current = (T) current.add(ONE);
                } while (additional != null && additional.test(current));
                return temp;
            }
        };
    }
}
