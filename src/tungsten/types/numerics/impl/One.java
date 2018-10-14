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
 * Abstract implementation of the number 1 (one, or unity).
 * Note that this is not exactly a singleton implementation &mdash;
 * one instance exists for each MathContext in use.
 * Note that {@link #equals(java.lang.Object) } may be inconsistent
 * with {@link #hashCode() }.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class One implements Numeric {
    private final MathContext mctx;
    
    private One(MathContext mctx) {
        this.mctx = mctx;
    }
    
    private static final Map<MathContext, One> instanceMap = new HashMap<>();
    private static final Lock instanceLock = new ReentrantLock();

    public static Numeric getInstance(MathContext mctx) {
        instanceLock.lock();
        try {
            One instance = instanceMap.get(mctx);
            if (instance == null) {
                instance = new One(mctx);
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

    private static final IntegerType INT_UNITY = new IntegerImpl(BigInteger.ONE);

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        Numeric retval;
        switch (htype) {
            case INTEGER:
                // we can get away with this because IntegerImpl doesn't maintain
                // a MathContext as state
                retval = INT_UNITY;
                break;
            case RATIONAL:
                retval = new RationalImpl(INT_UNITY);
                break;
            case REAL:
                retval = obtainRealUnity();
                break;
            case COMPLEX:
                retval = new ComplexRectImpl(obtainRealUnity(), obtainRealZero());
                break;
            default:
                throw new CoercionException("Cannot coerce unity to expected type", One.class, numtype);
        }
        OptionalOperations.setMathContext(retval, mctx);
        return retval;
    }
    
    private RealType obtainRealUnity() {
        return new RealImpl(BigDecimal.ONE);
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
        return INT_UNITY.negate();
    }
    
    private boolean isArgumentRealOrCplx(Numeric argument) {
        return NumericHierarchy.forNumericType(argument.getClass()).compareTo(NumericHierarchy.RATIONAL) > 0;
    }

    @Override
    public Numeric add(Numeric addend) {
        if (isArgumentRealOrCplx(addend)) {
            RealType unity = obtainRealUnity();
            OptionalOperations.setMathContext(unity, mctx);
            return unity.add(addend);
        }
        return INT_UNITY.add(addend);
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        if (isArgumentRealOrCplx(subtrahend)) {
            RealType unity = obtainRealUnity();
            OptionalOperations.setMathContext(unity, mctx);
            return unity.subtract(subtrahend);
        }
        return INT_UNITY.subtract(subtrahend);
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        return multiplier;
    }

    @Override
    public Numeric divide(Numeric divisor) {
        return divisor.inverse();
    }

    @Override
    public Numeric inverse() {
        return this;
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
     * <li>has a numeric value equivalent to unity (1),</li></ul>
     * then it is considered equal.
     * @param o the value to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof One) return true;
        if (o instanceof Numeric) {
            final Numeric that = (Numeric) o;
            
            if (!that.isExact()) return false;
            Class<? extends Numeric> clazz = that.getClass();
            try {
                Numeric temp = this.coerceTo(clazz);
                return temp.equals(o);
            } catch (CoercionException ex) {
                Logger.getLogger(One.class.getName()).log(Level.SEVERE, "Exception during test for equality with " + o, ex);
                return false;
            }
        }
        return false;
    }
    
    @Override
    public String toString() { return "1"; }
}
