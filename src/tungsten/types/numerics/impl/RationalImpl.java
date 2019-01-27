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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.NumericHierarchy;
import tungsten.types.numerics.RationalType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.Sign;
import tungsten.types.util.OptionalOperations;

/**
 *
 * @author tarquin
 */
public class RationalImpl implements RationalType {
    private boolean exact = true;
    private BigInteger numerator;
    private BigInteger denominator;
    private MathContext mctx = MathContext.UNLIMITED;
    
    public RationalImpl(BigInteger numerator, BigInteger denominator) {
        if (numerator == null || denominator == null) {
            throw new IllegalArgumentException("Numerator and denominator must be non-null");
        } else if (denominator.equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("Denominator must be non-zero");
        }
        // by convention, we want the denominator to be positive
        if (denominator.signum() < 0) {
            denominator = denominator.negate();
            numerator = numerator.negate();
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }
    
    public RationalImpl(String representation) {
        final int position = representation.indexOf('/');
        String numStr = representation.substring(0, position);
        String denomStr = representation.substring(position + 1);
        
        numerator = new BigInteger(numStr);
        denominator = new BigInteger(denomStr);
    }
    
    public RationalImpl(BigInteger numerator, BigInteger denominator, boolean exact) {
        this(numerator, denominator);
        this.exact = exact;
    }
    
    public RationalImpl(String representation, boolean exact) {
        this(representation);
        this.exact = exact;
    }
    
    /**
     * Convenience constructor which takes {@link IntegerType} arguments.
     * @param numerator the numerator of this fraction
     * @param denominator the denominator of this fraction
     */
    public RationalImpl(IntegerType numerator, IntegerType denominator) {
        this.numerator = numerator.asBigInteger();
        this.denominator = denominator.asBigInteger();
        this.exact = numerator.isExact() && denominator.isExact();
        mctx = pickBestMC(numerator.getMathContext(), denominator.getMathContext());
    }
    
    private MathContext pickBestMC(MathContext first, MathContext second) {
        int precision = Math.min(first.getPrecision(), second.getPrecision());
        return new MathContext(precision, first.getRoundingMode());
    }
    
    /**
     * Convenience constructor to convert {@link IntegerType}
     * to a rational.
     * @param val an integer value
     */
    public RationalImpl(IntegerType val) {
        numerator = val.asBigInteger();
        denominator = BigInteger.ONE;
        mctx = val.getMathContext();
    }
    
    public void setMathContext(MathContext nuCtx) {
        if (nuCtx != null) this.mctx = nuCtx;
    }

    @Override
    public RationalType magnitude() {
        final RationalType magnitude = new RationalImpl(numerator.abs(), denominator).reduce();
        OptionalOperations.setMathContext(magnitude, mctx);
        return magnitude;
    }

    @Override
    public RationalType negate() {
        final RationalImpl result = new RationalImpl(numerator.negate(), denominator, exact);
        result.setMathContext(mctx);
        return result;
    }

    @Override
    public IntegerType numerator() {
        return new IntegerImpl(numerator, exact);
    }

    @Override
    public IntegerType denominator() {
        return new IntegerImpl(denominator);
    }

    @Override
    public BigDecimal asBigDecimal() {
        BigDecimal decNum = new BigDecimal(numerator);
        BigDecimal decDenom = new BigDecimal(denominator);
        return decNum.divide(decDenom, mctx);
    }

    @Override
    public RationalType reduce() {
        BigInteger gcd = numerator.gcd(denominator);
        if (gcd.equals(BigInteger.ONE)) {
            // this fraction cannot be reduced any further
            return this;
        }
        RationalImpl reduced = new RationalImpl(numerator.divide(gcd), denominator.divide(gcd), exact);
        reduced.setMathContext(mctx);
        return reduced;
    }

    @Override
    public boolean isExact() {
        return exact;
    }

    @Override
    public boolean isCoercibleTo(Class<? extends Numeric> numtype) {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        switch (htype) {
            case INTEGER:
                return denominator.equals(BigInteger.ONE) || numerator.equals(BigInteger.ZERO);
            default:
                return htype != null;
        }
    }

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        switch (htype) {
            case INTEGER:
                if (denominator.equals(BigInteger.ONE)) {
                    return new IntegerImpl(numerator, exact);
                } else if (numerator.equals(BigInteger.ZERO)) {
                    return Zero.getInstance(mctx);
                } else {
                    throw new CoercionException("Cannot convert fraction to integer.", this.getClass(), numtype);
                }
            case RATIONAL:
                return this;
            case REAL:
                final RealImpl realval = new RealImpl(this, mctx);
//                realval.setMathContext(mctx);
                return realval;
            case COMPLEX:
                final RealType zero = (RealType) Zero.getInstance(mctx).coerceTo(RealType.class);
                final RealImpl creal = new RealImpl(this, mctx);
                return new ComplexRectImpl(creal, zero, exact);
            default:
                throw new CoercionException("Cannot convert rational to unknown type.", this.getClass(), numtype);
        }
    }

    @Override
    public Numeric add(Numeric addend) {
        if (addend instanceof RationalType) {
            RationalType that = (RationalType) addend;
            BigInteger denomnew = this.denominator.multiply(that.denominator().asBigInteger());
            BigInteger numleft = this.numerator.multiply(that.denominator().asBigInteger());
            BigInteger numright = that.numerator().asBigInteger().multiply(this.denominator);
            boolean exactness = this.isExact() && that.isExact();
            return new RationalImpl(numleft.add(numright), denomnew, exactness).reduce();
        } else if (addend instanceof IntegerType) {
            IntegerType that = (IntegerType) addend;
            BigInteger scaled = this.denominator.multiply(that.asBigInteger());
            boolean exactness = this.isExact() && that.isExact();
            return new RationalImpl(this.numerator.add(scaled), this.denominator, exactness);
        } else {
            try {
                return this.coerceTo(addend.getClass()).add(addend);
            } catch (CoercionException ex) {
                Logger.getLogger(RationalImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during rational add.", ex);
            }
        }
        throw new UnsupportedOperationException("Addition operation unsupported.");
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        if (subtrahend instanceof RationalType) {
            RationalType that = (RationalType) subtrahend;
            BigInteger denomnew = this.denominator.multiply(that.denominator().asBigInteger());
            BigInteger numleft = this.numerator.multiply(that.denominator().asBigInteger());
            BigInteger numright = that.numerator().asBigInteger().multiply(this.denominator);
            boolean exactness = this.isExact() && that.isExact();
            return new RationalImpl(numleft.subtract(numright), denomnew, exactness).reduce();
        } else if (subtrahend instanceof IntegerType) {
            IntegerType that = (IntegerType) subtrahend;
            BigInteger scaled = this.denominator.multiply(that.asBigInteger());
            boolean exactness = this.isExact() && that.isExact();
            return new RationalImpl(this.numerator.subtract(scaled), this.denominator, exactness);
        } else {
            try {
                return this.coerceTo(subtrahend.getClass()).subtract(subtrahend);
            } catch (CoercionException ex) {
                Logger.getLogger(RationalImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during rational subtract.", ex);
            }
        }
        throw new UnsupportedOperationException("Subtraction operation unsupported.");
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        if (multiplier instanceof RationalType) {
            RationalType that = (RationalType) multiplier;
            return new RationalImpl(numerator.multiply(that.numerator().asBigInteger()),
                    denominator.multiply(that.denominator().asBigInteger()),
                    exact && that.isExact()).reduce();
        } else if (multiplier instanceof IntegerType) {
            IntegerType that = (IntegerType) multiplier;
            final RationalType intermediate = new RationalImpl(numerator.multiply(that.asBigInteger()),
                    denominator, exact && that.isExact()).reduce();
            if (intermediate.isCoercibleTo(IntegerType.class)) {
                return numerator();
            }
            return intermediate;
        } else {
            try {
                return this.coerceTo(multiplier.getClass()).multiply(multiplier);
            } catch (CoercionException ex) {
                Logger.getLogger(RationalImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during rational multiply.", ex);
            }
        }
        throw new UnsupportedOperationException("Multiplication operation unsupported.");
    }

    @Override
    public Numeric divide(Numeric divisor) {
        return this.multiply(divisor.inverse());
    }

    @Override
    public Numeric inverse() {
        if (numerator.equals(BigInteger.ONE)) {
            return new IntegerImpl(denominator);
        }
        final RationalImpl inverse = new RationalImpl(denominator, numerator, exact);
        inverse.setMathContext(mctx);
        return inverse;
    }

    /**
     * This implementation of square root relies on the identity
     * sqrt(a/b) = sqrt(a)/sqrt(b).
     * @return an approximation of the square root of this fraction
     * @see IntegerImpl#sqrt()  
     */
    @Override
    public RationalType sqrt() {
        RationalType reduced = this.reduce();
        IntegerType numroot = reduced.numerator().sqrt();
        IntegerType denomroot = reduced.denominator().sqrt();
        final RationalImpl root = new RationalImpl(numroot, denomroot);
        root.setMathContext(mctx);
        return root;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Zero) {
            return this.isExact() && this.numerator.equals(BigInteger.ZERO);
        } else if (other instanceof One) {
            return this.isExact() && this.numerator.equals(this.denominator);
        }
        if (other instanceof RationalType) {
            RationalType that = (RationalType) other;
            if (this.isExact() != that.isExact()) {
                return false;
            }
            return numerator.equals(that.numerator().asBigInteger()) &&
                    denominator.equals(that.denominator().asBigInteger());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.exact ? 1 : 0);
        hash = 47 * hash + Objects.hashCode(this.numerator);
        hash = 47 * hash + Objects.hashCode(this.denominator);
        return hash;
    }

    @Override
    public int compareTo(RationalType o) {
        BigInteger lhs = numerator.multiply(o.denominator().asBigInteger());
        BigInteger rhs = o.numerator().asBigInteger().multiply(denominator);
        return lhs.compareTo(rhs);
    }

    @Override
    public Sign sign() {
        // the denominator is always positive; numerator contains sign information
        return Sign.fromValue(numerator);
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }
    
    @Override public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(numerator).append('/').append(denominator);
        return buf.toString();
    }

    @Override
    public IntegerType floor() {
        switch (this.sign()) {
            case POSITIVE:
                return new IntegerImpl(numerator.divide(denominator));
            case NEGATIVE:
                BigInteger[] result = numerator.divideAndRemainder(denominator);
                if (result[1].equals(BigInteger.ZERO)) {
                    return new IntegerImpl(result[0]);
                } else {
                    return new IntegerImpl(result[0].subtract(BigInteger.ONE));
                }
            default:
                return new IntegerImpl(BigInteger.ZERO);
        }
    }

    @Override
    public IntegerType ceil() {
        switch (this.sign()) {
            case POSITIVE:
                BigInteger[] result = numerator.divideAndRemainder(denominator);
                if (result[1].equals(BigInteger.ZERO)) {
                    return new IntegerImpl(result[0]);
                } else {
                    return new IntegerImpl(result[0].add(BigInteger.ONE));
                }
            case NEGATIVE:
                return new IntegerImpl(numerator.divide(denominator));
            default:
                return new IntegerImpl(BigInteger.ZERO);
        }
    }
}
