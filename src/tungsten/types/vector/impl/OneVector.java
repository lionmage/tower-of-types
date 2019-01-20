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

import ch.obermuhlner.math.big.BigDecimalMath;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.One;
import tungsten.types.numerics.impl.RealImpl;

/**
 * An implementation of the one vector, which has the value of 1
 * for all of its elements.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class OneVector implements Vector<Numeric> {
    private final long length;
    private final Numeric one;
    private final MathContext mctx;
    
    private OneVector(long length, MathContext mctx) {
        this.length = length;
        this.mctx = mctx;
        this.one = One.getInstance(mctx);
    }
    
    private static final Lock instanceLock = new ReentrantLock();
    private static final Map<Long, OneVector> instanceMap = new HashMap<>();
    
    public static OneVector getInstance(long length, MathContext ctx) {
        instanceLock.lock();
        try {
            final Long key = computeKey(length, ctx);
            OneVector instance = instanceMap.get(key);
            if (instance == null) {
                instance = new OneVector(length, ctx);
                instanceMap.put(key, instance);
            }
            return instance;
        } finally {
            instanceLock.unlock();
        }
    }
    
    private static Long computeKey(long length, MathContext ctx) {
        return Long.valueOf(length * 31L + (long) ctx.hashCode());
    }
    
    /**
     * Convenience factory method that returns a {@link OneVector} with
     * the same length as the supplied vector.
     * @param vector the vector whose length must be matched
     * @return a vector of 1 values
     */
    public static OneVector getInstance(Vector<? extends Numeric> vector) {
        return getInstance(vector.length(), vector.getMathContext());
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public Numeric elementAt(long position) {
        if (position >= 0L && position < length) {
            return one;
        }
        throw new IndexOutOfBoundsException("Specified index is out of range.");
    }

    @Override
    public void setElementAt(Numeric element, long position) {
        throw new UnsupportedOperationException("One vector is immutable.");
    }

    @Override
    public void append(Numeric element) {
        throw new UnsupportedOperationException("One vector is immutable.");
    }

    @Override
    public Vector<Numeric> add(Vector<Numeric> addend) {
        if (addend.length() != length) throw new IllegalArgumentException("Vectors must be of same length.");
        Numeric[] results = new Numeric[(int) length];
        for (int index = 0; index < length; index++) {
            results[index] = addend.elementAt(index).add(one);
        }
        return new RowVector(results);
    }

    @Override
    public Vector<Numeric> subtract(Vector<Numeric> subtrahend) {
        if (subtrahend.length() != length) throw new IllegalArgumentException("Vectors must be of same length.");
        Numeric[] results = new Numeric[(int) length];
        for (int index = 0; index < length; index++) {
            results[index] = one.subtract(subtrahend.elementAt(index));
        }
        return new RowVector(results);
    }

    @Override
    public Vector<Numeric> negate() {
        Numeric[] results = new Numeric[(int) length];
        for (int index = 0; index < length; index++) {
            results[index] = one.negate();
        }
        return new RowVector(results);
    }

    @Override
    public Vector<Numeric> scale(Numeric factor) {
        Numeric[] results = new Numeric[(int) length];
        for (int index = 0; index < length; index++) {
            results[index] = factor;
        }
        return new RowVector(results);
    }

    @Override
    public Numeric magnitude() {
        return new RealImpl(BigDecimal.valueOf(length)).sqrt();
    }

    @Override
    public Numeric dotProduct(Vector<Numeric> other) {
        Numeric accum = other.elementAt(0L);
        for (long index = 1L; index < other.length(); index++) {
            accum = accum.add(other.elementAt(index));
        }
        return accum;
    }

    @Override
    public Vector<Numeric> crossProduct(Vector<Numeric> other) {
        return this;
    }

    @Override
    public Vector<Numeric> normalize() {
        final Numeric[] results = new Numeric[(int) length];
        final Numeric value = this.magnitude().inverse();
        
        for (int index = 0; index < length; index++) {
            results[index] = value;
        }
        return new RowVector(results);
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }

    @Override
    public RealType computeAngle(Vector<Numeric> other) {
        Numeric dotprod = this.dotProduct(other);
        Numeric divisor = this.magnitude().multiply(other.magnitude());
        try {
            RealType intermediate = (RealType) dotprod.divide(divisor).coerceTo(RealType.class);
            return new RealImpl(BigDecimalMath.acos(intermediate.asBigDecimal(), mctx));
        } catch (CoercionException ex) {
            Logger.getLogger(OneVector.class.getName()).log(Level.SEVERE, "Failed to coerce result to RealType.", ex);
            throw new ArithmeticException("Coercion to real value failed.");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Vector) {
            Vector<? extends Numeric> that = (Vector<? extends Numeric>) o;
            if (that.length() != this.length()) return false;
            for (long k = 0L; k < that.length(); k++) {
                if (!one.equals(that.elementAt(k))) return false;
            }
            // we must have matched all elements
            return true;
        }
        return false;
    }
}
