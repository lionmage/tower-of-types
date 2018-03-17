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
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.NumericHierarchy;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.Sign;
import tungsten.types.util.MathUtils;

/**
 * This class provides a representation of the mathematical constant &#x212f; &mdash;
 * also known as Euler's number.
 * The class is not publicly instantiable; it provides a factory method
 * that will give you back an instance of itself for a given {@link MathContext},
 * and keeps a cache of instances that have been generated so that the value
 * of &#x212f; only needs to be calculated once for a given precision and
 * {@link RoundingMode}.
 * 
 * Internally, this class uses Brothers' formula for deriving &#x212f; to an
 * arbitrary precision.
 *
 * @author tarquin
 * @see <a href="https://www.intmath.com/exponential-logarithmic-functions/calculating-e.php">an article at Interactive Mathematics about ways to calculate &#x212f;</a>
 * @see <a href="https://en.wikipedia.org/wiki/E_(mathematical_constant)">the wikipedia article about this constant</a>
 */
public class Euler implements RealType {
    private final MathContext mctx;
    private BigDecimal value;

    private Euler(MathContext mctx) {
        this.mctx = mctx;
        calculate();
    }
    
    private static final Lock instanceLock = new ReentrantLock();
    private static final Map<MathContext, Euler> instanceMap = new HashMap<>();
    
    /**
     * Factory method for obtaining an instance of &#x212f; at a given precision.
     * @param mctx provides the desired precision and {@link RoundingMode} used for internal calculations
     * @return an instance of &#x212f; to the specified precision
     */
    public static Euler getInstance(MathContext mctx) {
        instanceLock.lock();
        try {
            Euler instance = instanceMap.get(mctx);
            if (instance == null) {
                instance = new Euler(mctx);
                instanceMap.put(mctx, instance);
            }
            return instance;
        } finally {
            instanceLock.unlock();
        }
    }

    @Override
    public boolean isIrrational() {
        return true;
    }

    @Override
    public RealType magnitude() {
        RealImpl magnitude = new RealImpl(value, false);
        magnitude.setMathContext(mctx);
        magnitude.setIrrational(true);
        return magnitude;
    }

    @Override
    public RealType negate() {
        return magnitude().negate();
    }

    @Override
    public BigDecimal asBigDecimal() {
        return value;
    }

    @Override
    public Sign sign() {
        return Sign.POSITIVE;
    }

    @Override
    public boolean isExact() {
        return false;
    }

    @Override
    public boolean isCoercibleTo(Class<? extends Numeric> numtype) {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        // can be coerced to real or complex
        return htype.compareTo(NumericHierarchy.REAL) >= 0;
    }

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        switch (htype) {
            case REAL:
                return this;  // it's already a real
            case COMPLEX:
                return new ComplexRectImpl(this, new RealImpl(BigDecimal.ZERO));
            default:
                throw new CoercionException("Euler can only be coerced to real or complex",
                        this.getClass(), numtype);
        }
    }

    @Override
    public Numeric add(Numeric addend) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Numeric divide(Numeric divisor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Numeric inverse() {
        RealImpl inverse = new RealImpl(BigDecimal.ONE.divide(value, mctx), false);
        inverse.setMathContext(mctx);
        inverse.setIrrational(true);
        return inverse;
    }

    @Override
    public Numeric sqrt() {
        return magnitude().sqrt();
    }
    
    public long numberOfDigits() {
        return (long) mctx.getPrecision();
    }
    
    private void calculate() {
        BigDecimal value = BigDecimal.ZERO;
        // compute a few extra digits so we can round off later
        MathContext compctx = new MathContext(mctx.getPrecision() + 4, mctx.getRoundingMode());
        for (int k = 0; k < mctx.getPrecision() / 2; k++) {
            value = value.add(computeKthTerm(k, compctx), compctx);
        }
        this.value = value.round(mctx);
    }
    
    private static final BigDecimal TWO = BigDecimal.valueOf(2L);
    private static final BigInteger iTWO = BigInteger.valueOf(2L);
    
    private BigDecimal computeKthTerm(int n, MathContext ctx) {
        BigDecimal numerator = TWO.multiply(BigDecimal.valueOf((long) n), ctx).add(TWO, ctx);
        BigInteger innerDenom = iTWO.multiply(BigInteger.valueOf((long) n)).add(BigInteger.ONE);
        IntegerType denominator = MathUtils.factorial(new IntegerImpl(innerDenom));
        
        return numerator.divide(new BigDecimal(denominator.asBigInteger(), ctx), ctx);
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }

    @Override
    public int compareTo(RealType o) {
        return this.value.compareTo(o.asBigDecimal());
    }

    @Override
    public String toString() {
        return "\u212F[" + this.numberOfDigits() + "]";
    }
}
