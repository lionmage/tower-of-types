/*
 * The MIT License
 *
 * Copyright 2018 Robert Poole <Tarquin.AZ@gmail.com>.
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
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.NumericHierarchy;
import tungsten.types.numerics.RealType;
import tungsten.types.util.OptionalOperations;

/**
 * A universal representation of zero.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class Zero implements Numeric {
    private final MathContext mctx;
    
    private Zero(MathContext mctx) {
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
    
    private static final IntegerType INTZERO = new IntegerImpl(BigInteger.ZERO);
    private static final RealType REALZERO = new RealImpl(BigDecimal.ZERO);

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        Numeric retval;
        switch (htype) {
            case INTEGER:
                retval = INTZERO;
                break;
            case RATIONAL:
                retval = new RationalImpl(INTZERO);
                break;
            case REAL:
                retval = REALZERO;
                break;
            case COMPLEX:
                retval = new ComplexRectImpl(REALZERO, REALZERO);
                break;
            default:
                throw new CoercionException("Cannot coerce zero to expected type", Zero.class, numtype);
        }
        OptionalOperations.setMathContext(retval, mctx);
        return retval;
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
        throw new ArithmeticException("Cannot divide by zero");
    }

    @Override
    public Numeric sqrt() {
        return this;
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }
}
