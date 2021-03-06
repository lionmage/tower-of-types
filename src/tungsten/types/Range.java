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
package tungsten.types;

import java.util.function.Predicate;

/**
 * A class to represent a numeric range.  Any numeric type that can be
 * meaningfully compared can be used in a range.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> a class or interface that extends {@link Numeric} and {@link Comparable}
 */
public class Range<T extends Numeric & Comparable<? super T>> {
    public enum BoundType { INCLUSIVE, EXCLUSIVE };
    public class Bound implements Comparable<T> {
        private final BoundType type;
        private final T value;
        
        public Bound(T value, BoundType type) {
            this.value = value;
            this.type  = type;
        }

        @Override
        public int compareTo(T o) {
            return value.compareTo(o);
        }
        
        public boolean matchesLower(T o) {
            switch (type) {
                case INCLUSIVE:
                    return this.compareTo(o) <= 0;
                case EXCLUSIVE:
                    return this.compareTo(o) < 0;
                default:
                    throw new IllegalStateException("Unknown bound of type " + type);
            }
        }
        
        public boolean matchesUpper(T o) {
            switch (type) {
                case INCLUSIVE:
                    return this.compareTo(o) >= 0;
                case EXCLUSIVE:
                    return this.compareTo(o) > 0;
                default:
                    throw new IllegalStateException("Unknown bound of type " + type);
            }
        }
        
        public boolean isInclusive() { return type == BoundType.INCLUSIVE; }
        public T getValue() { return value; }
    }
    
    private final Bound lowerBound, upperBound;
    
    /**
     * A convenience constructor which generates an instance where both
     * upper and lower bounds are of the same type.
     * @param lowerVal the lower bound
     * @param upperVal the upper bound
     * @param type the desired type
     */
    public Range(T lowerVal, T upperVal, BoundType type) {
        this.lowerBound = new Bound(lowerVal, type);
        this.upperBound = new Bound(upperVal, type);
    }
    
    public Range(T lowerVal, BoundType lowerType, T upperVal, BoundType upperType) {
        this.lowerBound = new Bound(lowerVal, lowerType);
        this.upperBound = new Bound(upperVal, upperType);
    }
    
    public boolean contains(T val) {
        return lowerBound.matchesLower(val) && upperBound.matchesUpper(val);
    }
    
    /**
     * Test whether the given value is below the lower bound of this range.
     * @param val
     * @return 
     */
    public boolean isBelow(T val) {
        switch (lowerBound.type) {
            case INCLUSIVE:
                return val.compareTo(lowerBound.getValue()) < 0;
            case EXCLUSIVE:
                return val.compareTo(lowerBound.getValue()) <= 0;
            default:
                throw new IllegalStateException("Unknown bound of type " + lowerBound.type);
        }
    }
    
    /**
     * Test whether the given value is above the upper bound of this range.
     * @param val
     * @return 
     */
    public boolean isAbove(T val) {
        switch (upperBound.type) {
            case INCLUSIVE:
                return val.compareTo(upperBound.getValue()) > 0;
            case EXCLUSIVE:
                return val.compareTo(upperBound.getValue()) >= 0;
            default:
                throw new IllegalStateException("Unknown bound of type " + upperBound.type);
        }
    }
    
    public T getLowerBound() {
        return lowerBound.getValue();
    }
    
    public boolean isLowerClosed() {
        return lowerBound.isInclusive();
    }
    
    public T getUpperBound() {
        return upperBound.getValue();
    }
    
    public boolean isUpperClosed() {
        return upperBound.isInclusive();
    }
    
    public Predicate<T> getPredicate() {
        return (T t) -> this.contains(t);
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(lowerBound.isInclusive() ? '[' : '(');
        buf.append(lowerBound.getValue()).append(", ");
        buf.append(upperBound.getValue());
        buf.append(upperBound.isInclusive() ? ']' : ')');
        return buf.toString();
    }
}
