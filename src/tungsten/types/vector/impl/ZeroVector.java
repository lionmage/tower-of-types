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
package tungsten.types.vector.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import tungsten.types.Vector;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.RealImpl;

/**
 * An implementation of the zero vector, which has the value of 0
 * for all of its elements.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class ZeroVector implements Vector<RealType> {
    private final long length;
    private static final RealType ZERO = new RealImpl(BigDecimal.ZERO);
    
    private ZeroVector(long length) {
        this.length = length;
    }
    
    public static ZeroVector getInstance(long length) {
        return new ZeroVector(length);
    }
    
    /**
     * Convenience factory method that returns a {@link ZeroVector} with
     * the same length as the supplied vector.
     * @param rvector the vector whose length must be matched
     * @return a zero vector
     */
    public static ZeroVector getInstance(Vector<RealType> rvector) {
        return getInstance(rvector.length());
    }

    @Override
    public long length() {
        return length();
    }

    @Override
    public RealType elementAt(long position) {
        if (position < length) {
            return ZERO;
        }
        throw new IndexOutOfBoundsException("Specified index is out of range.");
    }

    @Override
    public void setElementAt(RealType element, long position) {
        throw new UnsupportedOperationException("Zero vector is immutable.");
    }

    @Override
    public void append(RealType element) {
        throw new UnsupportedOperationException("Zero vector is immutable.");
    }

    @Override
    public Vector<RealType> add(Vector<RealType> addend) {
        return addend;
    }

    @Override
    public Vector<RealType> subtract(Vector<RealType> subtrahend) {
        return subtrahend.negate();
    }

    @Override
    public Vector<RealType> negate() {
        return this;
    }

    @Override
    public Vector<RealType> scale(RealType factor) {
        return this;
    }

    @Override
    public RealType magnitude() {
        return ZERO;
    }

    @Override
    public RealType dotProduct(Vector<RealType> other) {
        return ZERO;
    }

    @Override
    public Vector<RealType> crossProduct(Vector<RealType> other) {
        return this;
    }

    @Override
    public Vector<RealType> normalize() {
        throw new UnsupportedOperationException("Zero vector cannot be normalized.");
    }

    @Override
    public MathContext getMathContext() {
        return MathContext.UNLIMITED;
    }
}
