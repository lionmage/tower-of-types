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
package tungsten.types.numerics.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.NumericHierarchy;
import tungsten.types.numerics.RealType;
import tungsten.types.util.OptionalOperations;

/**
 * A universal representation of zero (0).
 * Note that this is not exactly a singleton implementation &mdash;
 * one instance exists for each {@link MathContext} in use.
 * Note that {@link #equals(java.lang.Object) } may be inconsistent
 * with {@link #hashCode() }.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class Zero implements Numeric, Comparable<Numeric> {
    protected final MathContext mctx;
    
    protected Zero(MathContext mctx) {
        this.mctx = mctx;
    }
    
    private static final Map<MathContext, Zero> instanceMap = new HashMap<>();
    private static final Lock instanceLock = new ReentrantLock();
    
    public static Numeric getInstance(MathContext mctx) {
        instanceLock.lock();
        try {
            Zero instance = instanceMap.get(mctx);
            if (instance == null) {
                instance = new Zero(mctx);
                instanceMap.put(mctx, instance);
            }
            return instance;
        } finally {
            instanceLock.unlock();
        }
    }

    @Override
    public boolean isExact() {
        return true;
    }

    @Override
    public boolean isCoercibleTo(Class<? extends Numeric> numtype) {
        return true;
    }
    
    private static final IntegerType INT_ZERO = new IntegerImpl(BigInteger.ZERO);

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        Numeric retval;
        switch (htype) {
            case INTEGER:
                // we can get away with this because IntegerImpl doesn't keep
                // math context state
                retval = INT_ZERO;
                break;
            case RATIONAL:
                retval = new RationalImpl(INT_ZERO);
                break;
            case REAL:
                retval = obtainRealZero();
                break;
            case COMPLEX:
                retval = new ComplexRectImpl(obtainRealZero(), obtainRealZero());
                break;
            default:
                throw new CoercionException("Cannot coerce zero to expected type", Zero.class, numtype);
        }
        OptionalOperations.setMathContext(retval, mctx);
        return retval;
    }
    
    private RealType obtainRealZero() {
        return new RealImpl(BigDecimal.ZERO);
    }

    @Override
    public Numeric magnitude() {
        return this;
    }

    @Override
    public Numeric negate() {
        return this;
    }

    @Override
    public Numeric add(Numeric addend) {
        return addend;
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        return subtrahend.negate();
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        return this;
    }

    @Override
    public Numeric divide(Numeric divisor) {
        return this;
    }

    @Override
    public Numeric inverse() {
//        throw new ArithmeticException("Cannot divide by zero");
        return PosInfinity.getInstance(mctx);
    }

    @Override
    public Numeric sqrt() {
        return this;
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }
    
    /**
     * Test for equality with a given value.  If the given value is:
     * <ul><li>an implementation of {@link Numeric}</li>
     * <li>is exact, and</li>
     * <li>has a numeric value equivalent to zero (0),</li></ul>
     * then it is considered equal.
     * @param o the value to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Zero) return true;
        if (o instanceof Numeric) {
            final Numeric that = (Numeric) o;
            
            if (!that.isExact()) return false;
            Class<? extends Numeric> clazz = that.getClass();
            try {
                Numeric temp = this.coerceTo(clazz);
                return temp.equals(o);
            } catch (CoercionException ex) {
                Logger.getLogger(Zero.class.getName()).log(Level.SEVERE, "Exception during test for equality with " + o, ex);
                return false;
            }
        }
        return false;
    }
    
    @Override
    public String toString() { return "0"; }

    @Override
    public int compareTo(Numeric o) {
        // Negative 0 is less than 0
        if (o instanceof NegZero) return 1;
        if (o instanceof Zero) return 0;
        if (o instanceof One) return -1;
        if (o instanceof PosInfinity) return -1;
        if (o instanceof NegInfinity) return 1;
        if (o instanceof Comparable) {
            try {
                return ((Comparable) this.coerceTo(o.getClass())).compareTo(o);
            } catch (CoercionException ex) {
                Logger.getLogger(Zero.class.getName()).log(Level.SEVERE, "Exception during comparison with " + o, ex);
            }
        }
        throw new IllegalArgumentException("Non-comparable value of type " + o.getClass().getTypeName());
    }
}
