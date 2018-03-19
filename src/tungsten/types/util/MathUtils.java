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
package tungsten.types.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Numeric;
import tungsten.types.Range;
import static tungsten.types.Range.BoundType;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.NumericHierarchy;
import tungsten.types.numerics.RationalType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.Sign;
import tungsten.types.numerics.impl.Euler;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.RealImpl;

/**
 * A utility class to hold commonly used functions and algorithms.
 *
 * @author tarquin
 */
public class MathUtils {
    private static final BigInteger TWO = BigInteger.valueOf(2L);
    
    private static final Map<Long, BigInteger> factorialCache = new HashMap<>();
    
    public static IntegerType factorial(IntegerType n) {
        if (n.asBigInteger().equals(BigInteger.ZERO) || n.asBigInteger().equals(BigInteger.ONE)) {
            return new IntegerImpl(BigInteger.ONE);
        } else if (getCacheFor(n) != null) {
            return new IntegerImpl(getCacheFor(n));
        }
        
        Long m = findMaxKeyUnder(n);
        
        BigInteger accum = m != null ? factorialCache.get(m) : BigInteger.ONE;
        BigInteger intermediate = n.asBigInteger();
        BigInteger bailout = m != null ? BigInteger.valueOf(m + 1L) : TWO;
        while (intermediate.compareTo(bailout) >= 0) {
            accum = accum.multiply(intermediate);
            intermediate = intermediate.subtract(BigInteger.ONE);
        }
        cacheFact(n, accum);
        return new IntegerImpl(accum);
    }

    /**
     * If there's a cached factorial value, find the highest key that is less
     * than n.
     * @param n the upper bound of our search
     * @return the highest cache key given the search parameter
     */
    private static Long findMaxKeyUnder(IntegerType n) {
        try {
            final long ncmp = n.asBigInteger().longValueExact();
            return factorialCache.keySet().parallelStream().filter(x -> x < ncmp).max(Long::compareTo).orElse(null);
        } catch (ArithmeticException e) {
            Logger.getLogger(MathUtils.class.getName()).log(Level.FINER, "Attempt to find a max key < n outside Long range.", e);
            // return the biggest key we can find since the given upper bound is too large for the cache
            return factorialCache.keySet().parallelStream().max(Long::compareTo).orElse(null);
        }
    }
    
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    
    private static void cacheFact(BigInteger n, BigInteger value) {
        // these bounds should prevent an ArithmeticException from being thrown
        // if not, we want to fail fast to catch the problem
        if (n.compareTo(TWO) >= 0 && n.compareTo(MAX_LONG) < 0) {
            Long key = n.longValueExact();
            
            if (!factorialCache.containsKey(key)) factorialCache.put(key, value);
        }
    }
    
    private static void cacheFact(IntegerType n, BigInteger value) {
        cacheFact(n.asBigInteger(), value);
    }
    
    private static BigInteger getCacheFor(BigInteger n) {
        try {
            return factorialCache.get(n.longValueExact());
        } catch (ArithmeticException e) {
            Logger.getLogger(MathUtils.class.getName()).log(Level.FINER, "Attempt to cache a factorial value for n outside Long range.", e);
            return null; // this is the same as if we had a regular cache miss
        }
    }
    
    private static BigInteger getCacheFor(IntegerType n) {
        return getCacheFor(n.asBigInteger());
    }
    
    /**
     * Compute x<sup>n</sup>.
     * @param x the value to take the exponent of
     * @param n the integer exponent
     * @param mctx the {@link MathContext} for computing the exponent
     * @return x raised to the n<sup>th</sup> power
     */
    public static RealType computeIntegerExponent(RealType x, int n, MathContext mctx) {
        if (n == 0) return new RealImpl(BigDecimal.ONE);
        if (n == 1) return x;
        try {
            if (n == -1) {
                return (RealType) x.inverse().coerceTo(RealType.class);
            }
            
            Numeric intermediate = x.magnitude();
            Numeric factor = intermediate;
            OptionalOperations.setMathContext(x, mctx);
            for (int idx = 2; idx <= Math.abs(n); idx++) {
                intermediate = intermediate.multiply(factor);
                OptionalOperations.setMathContext(intermediate, mctx);
            }
            if (n < 0) intermediate = intermediate.inverse();
            // if n is odd, preserve original sign
            if (x.sign() == Sign.NEGATIVE && n % 2 != 0) intermediate = intermediate.negate();
            return (RealType) intermediate.coerceTo(RealType.class);
        } catch (CoercionException ex) {
            Logger.getLogger(MathUtils.class.getName()).log(Level.SEVERE, "Unrecoverable exception thrown while computing integer exponent.", ex);
            throw new ArithmeticException("Failure to coerce to RealType.");
        }
    }
    
    public static RealType computeIntegerExponent(RealType x, int n) {
        return computeIntegerExponent(x, n, x.getMathContext());
    }
    
    private static final BigDecimal decTWO = BigDecimal.valueOf(2L);
    private static final Range<RealType> newtonRange = new Range<RealType>(new RealImpl(BigDecimal.ZERO), new RealImpl(decTWO), BoundType.EXCLUSIVE);
    
    /**
     * Compute the natural logarithm, ln(x)
     * @param x the value for which to obtain the natural logarithm
     * @param mctx the {@link MathContext} to use for this operation
     * @return the natural logarithm of {@code x}
     */
    public static RealType ln(RealType x, MathContext mctx) {
        if (x.asBigDecimal().compareTo(BigDecimal.ONE) == 0) return new RealImpl(BigDecimal.ZERO);
        if (x.asBigDecimal().compareTo(BigDecimal.ZERO) <= 0) throw new ArithmeticException("ln is undefined for values <= 0");
        if (newtonRange.contains(x)) return lnNewton(x, mctx);
        
        if (x.asBigDecimal().compareTo(BigDecimal.TEN) > 0) {
            RealType mantissa = mantissa(x);
            IntegerType exponent = exponent(x);
            // use the identity ln(a*10^n) = ln(a) + n*ln(10)
            RealType ln10 = lnSeries(new RealImpl(BigDecimal.TEN), mctx);
            try {
                return (RealType) ln(mantissa, mctx).add((RealType) ln10.multiply(exponent).coerceTo(RealType.class));
            } catch (CoercionException ex) {
                Logger.getLogger(MathUtils.class.getName()).log(Level.SEVERE, "Failed to coerce ln(10)*n to RealType.", ex);
                throw new IllegalStateException("Multiplication of real and integer should give us a real back.");
            }
        }
        
        return lnSeries(x, mctx);
    }
    
    public static RealType ln(RealType x) {
        return ln(x, x.getMathContext());
    }
    
    private static RealType lnNewton(RealType x, MathContext mctx) {
        Euler e = Euler.getInstance(mctx);
        BigDecimal xval = x.asBigDecimal();
        BigDecimal y0 = BigDecimal.ONE;
        BigDecimal y1;
        while (true) {
            final BigDecimal expval = e.exp(new RealImpl(y0, false)).asBigDecimal();
            
            BigDecimal num = xval.subtract(expval, mctx);
            BigDecimal denom = xval.add(expval, mctx);
            y1 = y0.add(decTWO.multiply(num.divide(denom, mctx), mctx), mctx);
            if (y0.compareTo(y1) == 0) break;
            
            y0 = y1;
        }
        final RealImpl result = new RealImpl(y0, false);
        result.setIrrational(true);
        result.setMathContext(mctx);
        return result;
    }
    
    private static RealType lnSeries(RealType x, MathContext mctx) {
        MathContext compctx = new MathContext(mctx.getPrecision() + 4, mctx.getRoundingMode());
        BigDecimal xfrac = x.asBigDecimal().subtract(BigDecimal.ONE, compctx).divide(x.asBigDecimal(), compctx);
        BigDecimal sum = BigDecimal.ZERO;
        for (int n = 1; n < mctx.getPrecision() * 17; n++) {
            sum = sum.add(computeNthTerm_ln(xfrac, n, compctx), compctx);
        }
        RealImpl result = new RealImpl(sum.round(mctx), false);
        result.setIrrational(true);
        result.setMathContext(mctx);
        return result;
    }
    
    private static BigDecimal computeNthTerm_ln(BigDecimal frac, int n, MathContext mctx) {
        BigDecimal ninv = BigDecimal.ONE.divide(BigDecimal.valueOf((long) n), mctx);
        return ninv.multiply(computeIntegerExponent(new RealImpl(frac), n, mctx).asBigDecimal(), mctx);
    }
    
    /**
     * Computes the mantissa of a real value as expressed in scientific
     * notation, mantissa * 10<sup>exponent</sup>.
     * @param x the real value
     * @return the mantissa of {@code x}
     */
    public static RealType mantissa(RealType x) {
        BigDecimal mantissa = x.asBigDecimal().scaleByPowerOfTen(x.asBigDecimal().scale() + 1 - x.asBigDecimal().precision());
        RealImpl result = new RealImpl(mantissa, x.isExact());
        result.setMathContext(x.getMathContext());
        return result;
    }
    
    /**
     * Computes the exponent of a real value as expressed in scientific
     * notation, mantissa * 10<sup>exponent</sup>.
     * @param x the real value
     * @return the exponent of {@code x}
     */
    public static IntegerType exponent(RealType x) {
        int exponent = x.asBigDecimal().precision() - x.asBigDecimal().scale() - 1;
        return new IntegerImpl(BigInteger.valueOf((long) exponent));  // the exponent should always be exact
    }
    
    /**
     * Compute the general case of x<sup>y</sup>, where x and y are both real numbers.
     * @param base the value to raise to a given power
     * @param exponent the power to which we want to raise {@code base}
     * @param mctx the {@link MathContext} to use for this calculation
     * @return the value of base<sup>exponent</sup>
     */
    public static RealType generalizedExponent(RealType base, Numeric exponent, MathContext mctx) {
        NumericHierarchy htype = NumericHierarchy.forNumericType(exponent.getClass());
        switch (htype) {
            case INTEGER:
                int n = ((IntegerType) exponent).asBigInteger().intValueExact();
                return computeIntegerExponent(base, n, mctx);
            case REAL:
                if (exponent.isCoercibleTo(IntegerType.class)) {
                    try {
                        IntegerType integer = (IntegerType) exponent.coerceTo(IntegerType.class);
                        return generalizedExponent(base, integer, mctx);
                    } catch (CoercionException ex) {
                        Logger.getLogger(MathUtils.class.getName()).log(Level.SEVERE, "Failed to coerce real to integer.", ex);
                        throw new IllegalStateException("Should have been able to coerce RealType to IntegerType.", ex);
                    }
                }
                // approximate with a rational
                try {
                    RationalType ratexponent = (RationalType) exponent.coerceTo(RationalType.class);
                    return generalizedExponent(base, ratexponent, mctx);
                } catch (CoercionException ex) {
                    Logger.getLogger(MathUtils.class.getName()).log(Level.SEVERE, "Failed to coerce real to rational.", ex);
                    throw new IllegalStateException("Should have been able to coerce RealType to RationalType.", ex);
                }
            case RATIONAL:
                // use the identity b^(u/v) = vth root of b^u
                RationalType ratexponent = (RationalType) exponent;
                final int n_num = ratexponent.numerator().asBigInteger().intValueExact();
                RealType intermediate = computeIntegerExponent(base, n_num, mctx);
                return nthRoot(intermediate, ratexponent.denominator(), mctx);
            default:
                throw new ArithmeticException("Currently generalizedExponent() has no support for exponents of type " + exponent.getClass().getTypeName());
        }
    }
    
    /**
     * Compute the nth root of a real value a.  The result is the principal
     * root of the equation x<sup>n</sup> = a.  Note that the {@link MathContext}
     * is inferred from the argument {@code a}.
     * @param a the value for which we want to find a root
     * @param n the degree of the root
     * @return the {@code n}th root of {@code a}
     */
    public static RealType nthRoot(RealType a, IntegerType n) {
        return nthRoot(a, n, a.getMathContext());
    }
    
    /**
     * Compute the nth root of a real value a.  The result is the principal
     * root of the equation x<sup>n</sup> = a.  The {@link MathContext}
     * is explicitly supplied.
     * @param a the value for which we want to find a root
     * @param n the degree of the root
     * @param mctx the {@link MathContext} to use for this calculation
     * @return the {@code n}th root of {@code a}
     */
    public static RealType nthRoot(RealType a, IntegerType n, MathContext mctx) {
        BigDecimal A = a.asBigDecimal();
        if (A.compareTo(BigDecimal.ZERO) == 0) return new RealImpl(BigDecimal.ZERO);
        
        int nint = n.asBigInteger().intValueExact();
        BigDecimal ncalc = new BigDecimal(n.asBigInteger());
        BigDecimal nminus1 = ncalc.subtract(BigDecimal.ONE);
        BigDecimal x0;
        BigDecimal x1 = A.divide(new BigDecimal(n.asBigInteger())); // initial estimate
        
        while (true) {
            x0 = x1;
            x1 = nminus1.multiply(x0, mctx).add(A.divide(x0.pow(nint - 1, mctx), mctx), mctx).divide(ncalc, mctx);
            if (x0.compareTo(x1) == 0) break;
        }
        final RealImpl result = new RealImpl(x1, a.isExact() && x1.stripTrailingZeros().scale() <= 0);
        result.setIrrational(!result.isCoercibleTo(IntegerType.class));
        result.setMathContext(mctx);
        return result;
    }
}
