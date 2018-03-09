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
import java.math.RoundingMode;
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

/**
 *
 * @author tarquin
 */
public class RealImpl implements RealType, Comparable<RealType> {
    private boolean irrational = false;
    private boolean exact = true;
    private BigDecimal val;
    private MathContext mctx = MathContext.UNLIMITED;
    
    private static final BigDecimal TWO = BigDecimal.valueOf(2L);
    
    public RealImpl(BigDecimal init) {
        val = init;
    }
    
    public RealImpl(BigDecimal init, boolean exact) {
        this(init);
        this.exact = exact;
    }
    
    public RealImpl(String representation) {
        val = new BigDecimal(representation);
    }
    
    public RealImpl(String representation, boolean exact) {
        this(representation);
        this.exact = exact;
    }
    
    /**
     * Convenience constructor to convert a rational to a real.
     * @param init the rational value to convert
     */
    public RealImpl(RationalType init) {
        this(init.asBigDecimal(), init.isExact());
        this.setMathContext(init.getMathContext());
        irrational = false;
    }
    
    /**
     * Convenience constructor to convert an integer to a real.
     * @param init the integer value to convert
     */
    public RealImpl(IntegerType init) {
        val = new BigDecimal(init.asBigInteger());
        exact = init.isExact();
        irrational = false;
    }
    
    public void setIrrational(boolean irrational) {
        if (irrational && this.exact) {
            throw new IllegalStateException("There cannot be an exact representation of an irrational number.");
        }
        this.irrational = irrational;
    }
    
    public void setMathContext(MathContext mctx) {
        if (mctx == null) {
            throw new IllegalArgumentException("MathContext must not be null.");
        }
        this.mctx = mctx;
    }

    @Override
    public boolean isIrrational() {
        return irrational;
    }

    @Override
    public RealType magnitude() {
        return new RealImpl(val.abs(mctx));
    }

    @Override
    public RealType negate() {
        return new RealImpl(val.negate(mctx), exact);
    }

    @Override
    public BigDecimal asBigDecimal() {
        return val;
    }

    @Override
    public Sign sign() {
        return Sign.fromValue(val);
    }

    @Override
    public boolean isExact() {
        return exact;
    }

    @Override
    public boolean isCoercibleTo(Class<? extends Numeric> numtype) {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        switch (htype) {
            case COMPLEX:
            case REAL:
                return true;
            case INTEGER:
                return this.isIntegralValue();
            case RATIONAL:
                return !this.isIrrational();
            default:
                return false;
        }
    }

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        switch (htype) {
            case REAL:
                return this;
            case COMPLEX:
                RealType zero = new RealImpl(BigDecimal.ZERO);
                ComplexRectImpl cplx = new ComplexRectImpl(this, zero, exact);
                cplx.setMathContext(mctx);
                return cplx;
            case RATIONAL:
                if (!irrational) {
                    return rationalize();
                }
                break;
            case INTEGER:
                if (this.isIntegralValue()) {
                    return new IntegerImpl(val.toBigIntegerExact(), exact);
                }
                break;
        }
        throw new CoercionException("Failed to coerce real value.", this.getClass(), numtype);
    }
    
    protected RationalType rationalize() {
        if (isIntegralValue()) {
            return new RationalImpl(val.toBigIntegerExact(), BigInteger.ONE, exact);
        }
        final BigDecimal stripped = val.stripTrailingZeros();
        IntegerImpl num = new IntegerImpl(stripped.unscaledValue(), exact);
        IntegerImpl denom = new IntegerImpl(BigInteger.TEN.pow(stripped.scale()));
        RationalImpl ratl = new RationalImpl(num, denom);
        ratl.setMathContext(mctx);
        return ratl.reduce();
    }

    @Override
    public Numeric add(Numeric addend) {
        if (addend instanceof RealType) {
            RealType that = (RealType) addend;
            return new RealImpl(val.add(that.asBigDecimal(), mctx), this.exact && that.isExact());
        } else if (addend instanceof RationalType) {
            RationalType that = (RationalType) addend;
            return new RealImpl(val.add(that.asBigDecimal(), mctx), this.exact && that.isExact());
        } else if (addend instanceof IntegerType) {
            RealType that = new RealImpl((IntegerType) addend);
            return that;
        } else {
            try {
                return this.coerceTo(addend.getClass()).add(addend);
            } catch (CoercionException ex) {
                Logger.getLogger(RealImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during real add.", ex);
            }
        }
        throw new UnsupportedOperationException("Addition operation unsupported.");
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        if (subtrahend instanceof RealType) {
            // corner case where both operands are real, to avoid intermediate object creation
            RealType that = (RealType) subtrahend;
            return new RealImpl(val.subtract(that.asBigDecimal(), mctx), this.exact && that.isExact());
        }
        return add(subtrahend.negate());
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        if (multiplier instanceof RealType) {
            RealType remult = (RealType) multiplier;
            return new RealImpl(val.multiply(remult.asBigDecimal(), mctx));
        } else if (multiplier instanceof RationalType) {
            RationalType ratmult = (RationalType) multiplier;
            BigDecimal num = new BigDecimal(ratmult.numerator().asBigInteger());
            BigDecimal denom = new BigDecimal(ratmult.denominator().asBigInteger());
            return new RealImpl(val.multiply(num).divide(denom, mctx));
        } else if (multiplier instanceof IntegerType) {
            IntegerType intmult = (IntegerType) multiplier;
            if (isIntegralValue()) {
                return new IntegerImpl(val.toBigIntegerExact().multiply(intmult.asBigInteger()));
            } else {
                BigDecimal decmult = new BigDecimal(intmult.asBigInteger());
                return new RealImpl(val.multiply(decmult, mctx));
            }
        } else {
            try {
                return this.coerceTo(multiplier.getClass()).multiply(multiplier);
            } catch (CoercionException ex) {
                Logger.getLogger(RealImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during real multiply.", ex);
            }
        }
        throw new UnsupportedOperationException("Multiplication operation unsupported.");
    }

    @Override
    public Numeric divide(Numeric divisor) {
        if (divisor instanceof RealType) {
            RealType redivisor = (RealType) divisor;
            return new RealImpl(val.divide(redivisor.asBigDecimal(), mctx));
        } else if (divisor instanceof RationalType) {
            RationalType ratdivisor = (RationalType) divisor;
            return new RealImpl(val.divide(ratdivisor.asBigDecimal(), mctx));
        } else if (divisor instanceof IntegerType) {
            IntegerType intdivisor = (IntegerType) divisor;
            if (isIntegralValue()) {
                return new RationalImpl(val.toBigIntegerExact(), intdivisor.asBigInteger());
            } else {
                BigDecimal decdivisor = new BigDecimal(intdivisor.asBigInteger());
                return new RealImpl(val.divide(decdivisor, mctx));
            }
        } else {
            try {
                this.coerceTo(divisor.getClass()).divide(divisor);
            } catch (CoercionException ex) {
                Logger.getLogger(RealImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during real divide.", ex);
            }
        }
        throw new UnsupportedOperationException("Division operation unsupported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Numeric inverse() {
        boolean exactness = isExact();
        if (exactness) {
            if (isIntegralValue()) {
                return new RationalImpl(BigInteger.ONE, val.toBigIntegerExact());
            }
            // TODO if this is not an integer, add a check to see if the
            // division will result in an infinitely repeating sequence.
            // If so, set exactness = false.
        }
        System.out.println("RealImpl MC precision = " + mctx.getPrecision());
        return new RealImpl(BigDecimal.ONE.divide(val, mctx), exactness);
    }

    @Override
    public Numeric sqrt() {
        if (isIntegralValue() && sign().compareTo(Sign.ZERO) >= 0) {
            IntegerImpl intval = new IntegerImpl(val.toBigIntegerExact(), exact);
            if (intval.isPerfectSquare()) {
                return intval.sqrt();
            }
        }
        if (sign() == Sign.NEGATIVE) {
            RealType zero = new RealImpl(BigDecimal.ZERO, true);
            ComplexRectImpl cplx = new ComplexRectImpl(this, zero, exact);
            cplx.setMathContext(mctx);
            return cplx.sqrt();
        }

        BigDecimal x0 = BigDecimal.ZERO;
        BigDecimal x1 = val.divide(TWO, RoundingMode.FLOOR); // initial estimate
        while (!x0.equals(x1)) {
            x0 = x1;
            x1 = val.divide(x0, mctx);
            x1 = x1.add(x0, mctx);
            x1 = x1.divide(TWO, mctx);
        }
        
        x1 = x1.stripTrailingZeros(); // ensure this representation is as compact as possible
        final boolean lendiff = fractionalLengthDifference(x1) == 0;
        RealImpl result = new RealImpl(x1, exact && !lendiff);
        result.setIrrational(lendiff);
        result.setMathContext(mctx); // inherit the MathContext from this object
        return result;
    }
    
    private int fractionalLengthDifference(BigDecimal num) {
        IntegerType nonFractionPart = new IntegerImpl(num.toBigInteger());
        int reducedDigitLength = mctx.getPrecision() - (int) nonFractionPart.numberOfDigits();
        return reducedDigitLength - num.scale();
    }
    
    protected boolean isIntegralValue() {
        if (val.scale() > 0) {
            return val.stripTrailingZeros().scale() <= 0;
        }
        return true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof RealType) {
            RealType that = (RealType) o;
            if (this.isExact() != that.isExact()) return false;
//            return val.equals(that.asBigDecimal());
            return val.compareTo(that.asBigDecimal()) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.irrational ? 1 : 0);
        hash = 79 * hash + (this.exact ? 1 : 0);
        hash = 79 * hash + Objects.hashCode(this.val);
        return hash;
    }
    
    @Override
    public String toString() {
        return val.toPlainString();
    }

    @Override
    public int compareTo(RealType o) {
        return val.compareTo(o.asBigDecimal());
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }
}
