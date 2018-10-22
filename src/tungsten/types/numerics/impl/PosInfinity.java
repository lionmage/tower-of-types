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

import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.Sign;
import tungsten.types.util.OptionalOperations;

/**
 * A representation of positive infinity.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class PosInfinity implements Numeric, Comparable<Numeric> {
    private MathContext mctx;

    private PosInfinity(MathContext mctx) {
        this.mctx = mctx;
    }
    
    private static final Map<MathContext, PosInfinity> instanceMap = new HashMap<>();
    private static final Lock instanceLock = new ReentrantLock();
    
    public static Numeric getInstance(MathContext mctx) {
        instanceLock.lock();
        try {
            PosInfinity instance = instanceMap.get(mctx);
            if (instance == null) {
                instance = new PosInfinity(mctx);
                instanceMap.put(mctx, instance);
            }
            return instance;
        } finally {
            instanceLock.unlock();
        }
    }
    
    @Override
    public boolean isExact() {
        return false;
    }

    @Override
    public boolean isCoercibleTo(Class<? extends Numeric> numtype) {
        return false;
    }

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        throw new CoercionException("Can't coerce infinity to any other Numeric type.", this.getClass(), numtype);
    }

    @Override
    public Numeric magnitude() {
        return this;
    }

    @Override
    public Numeric negate() {
        return NegInfinity.getInstance(mctx);
    }

    @Override
    public Numeric add(Numeric addend) {
        if (addend instanceof NegInfinity) return Zero.getInstance(mctx);
        return this;
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        if (subtrahend instanceof PosInfinity) return Zero.getInstance(mctx);
        return this;
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        if (multiplier instanceof NegInfinity) return NegInfinity.getInstance(mctx);
        if (OptionalOperations.sign(multiplier) == Sign.NEGATIVE) {
            return NegInfinity.getInstance(mctx);
        }
        return this;
    }

    @Override
    public Numeric divide(Numeric divisor) {
        if (divisor instanceof NegInfinity) return NegZero.getInstance(mctx);
        if (divisor instanceof PosInfinity) return Zero.getInstance(mctx);
        if (OptionalOperations.sign(divisor) == Sign.NEGATIVE) {
            return NegInfinity.getInstance(mctx);
        }
        return this;
    }

    @Override
    public Numeric inverse() {
        return Zero.getInstance(mctx);
    }

    @Override
    public Numeric sqrt() {
        return this;
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }

    @Override
    public int compareTo(Numeric o) {
        if (o instanceof Comparable) {
            // positive infinity is always greater than any other value
            return 1;
        } else {
            throw new UnsupportedOperationException("Comparison to " + o.getClass().getTypeName() + " is not supported");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof PosInfinity;
    }
    
    @Override
    public int hashCode() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    public String toString() {
        return "\u221E";
    }
}
