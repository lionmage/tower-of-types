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

import ch.obermuhlner.math.big.BigDecimalMath;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Numeric;
import tungsten.types.Range;
import tungsten.types.Set;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.NumericHierarchy;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.Sign;
import tungsten.types.set.impl.NumericSet;
import tungsten.types.util.MathUtils;
import tungsten.types.util.OptionalOperations;
import tungsten.types.util.RangeUtils;

/**
 *
 * @author tarquin
 */
public class ComplexPolarImpl implements ComplexType {
    private RealType modulus;
    private RealType argument;
    private MathContext mctx;
    private boolean exact = true;

    private static final RealType TWO = new RealImpl(BigDecimal.valueOf(2L));
    
    public ComplexPolarImpl(RealType modulus, RealType argument) {
        if (modulus.sign() == Sign.NEGATIVE) {
            throw new IllegalArgumentException("Complex modulus should be positive");
        }
        this.modulus = modulus;
        this.argument = argument;
        this.mctx = MathUtils.inferMathContext(Arrays.asList(modulus, argument));
    }
    
    public ComplexPolarImpl(RealType modulus, RealType argument, boolean exact) {
        this(modulus, argument);
        this.exact = exact;
    }

    public void setMathContext(MathContext mctx) {
        this.mctx = mctx;
        OptionalOperations.setMathContext(modulus, mctx);
        OptionalOperations.setMathContext(argument, mctx);
    }
    
    @Override
    public RealType magnitude() {
        return modulus;
    }

    @Override
    public ComplexType negate() {
        Pi pi = Pi.getInstance(mctx);
        final ComplexPolarImpl negval = new ComplexPolarImpl(modulus, (RealType) argument.add(pi), false);
        negval.setMathContext(mctx);
        return negval;
    }

    @Override
    public ComplexType conjugate() {
        ComplexPolarImpl conj = new ComplexPolarImpl(modulus, argument.negate(), exact);
        conj.setMathContext(mctx);
        return conj;
    }

    @Override
    public RealType real() {
        final BigDecimal cosval = BigDecimalMath.cos(argument.asBigDecimal(), mctx);
        RealImpl real = new RealImpl(modulus.asBigDecimal().multiply(cosval, mctx).stripTrailingZeros(), false);
        real.setMathContext(mctx);
        return real;
    }

    @Override
    public RealType imaginary() {
        final Pi pi = Pi.getInstance(mctx);
        final BigDecimal normalizedArgument = this.normalizeArgument().asBigDecimal().stripTrailingZeros();
        if (normalizedArgument.compareTo(BigDecimal.ZERO) == 0 || normalizedArgument.compareTo(pi.asBigDecimal()) == 0) {
            final RealImpl zero = new RealImpl(BigDecimal.ZERO, true);
            zero.setIrrational(false);
            zero.setMathContext(mctx);
            return zero;
        }
        final BigDecimal sinval = BigDecimalMath.sin(argument.asBigDecimal(), mctx);
        RealImpl imag = new RealImpl(modulus.asBigDecimal().multiply(sinval, mctx).stripTrailingZeros(), false);
        imag.setMathContext(mctx);
        return imag;
    }

    @Override
    public RealType argument() {
        return argument;
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
                return true;
            case REAL:
                Pi pi = Pi.getInstance(mctx);
                final BigDecimal argNormalized = normalizeArgument().asBigDecimal();
                return argNormalized.compareTo(BigDecimal.ZERO) == 0 ||
                        argNormalized.compareTo(pi.asBigDecimal()) == 0;
            default:
                return false;
        }
    }
    
    protected RealType normalizeArgument() {
        if (argument instanceof Pi) {
            // special case where argument is exactly pi
            return argument;
        }
        RealImpl twopi = (RealImpl) Pi.getInstance(mctx).multiply(TWO);
        RealImpl reimpl = (RealImpl) argument;
        Range atan2range = RangeUtils.getAngularInstance(mctx);
        
        if (atan2range.contains(reimpl)) {
            // already in the range (-pi, pi]
            return argument;
        } else {
            // reduce values > pi
            while (atan2range.isAbove(reimpl)) {
                reimpl = (RealImpl) reimpl.subtract(twopi);
            }
            // increase values < -pi
            while (atan2range.isBelow(reimpl)) {
                reimpl = (RealImpl) reimpl.add(twopi);
            }
            return reimpl;
        }
    }

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        NumericHierarchy htype = NumericHierarchy.forNumericType(numtype);
        switch (htype) {
            case COMPLEX:
                return this;
            case REAL:
                BigDecimal normalArg = normalizeArgument().asBigDecimal();
                if (normalArg.compareTo(BigDecimal.ZERO) == 0) {
                    return modulus;
                } else if (normalArg.compareTo(Pi.getInstance(mctx).asBigDecimal()) == 0) {
                    return modulus.negate();
                } else {
                    throw new CoercionException("Argument must be 0 or pi", this.getClass(), numtype);
                }
            default:
                throw new CoercionException("Unsupported coercion", this.getClass(), numtype);
        }
    }

    @Override
    public Numeric add(Numeric addend) {
        if (addend instanceof ComplexType) {
            ComplexType cadd = (ComplexType) addend;
            return new ComplexRectImpl((RealType) this.real().add(cadd.real()),
                    (RealType) this.imaginary().add(cadd.imaginary()),
                    this.isExact() && cadd.isExact());
        } else if (addend.isCoercibleTo(RealType.class)) {
            try {
                RealType realval = (RealType) addend.coerceTo(RealType.class);
                return new ComplexRectImpl((RealType) this.real().add(realval), this.imaginary(), exact && realval.isExact());
            } catch (CoercionException ex) {
                // we should never get here
                Logger.getLogger(ComplexPolarImpl.class.getName()).log(Level.SEVERE, "Failed to coerce addend to RealType", ex);
            }
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        if (subtrahend instanceof ComplexType) {
            ComplexType csub = (ComplexType) subtrahend;
            return new ComplexRectImpl((RealType) this.real().subtract(csub.real()),
                    (RealType) this.imaginary().subtract(csub.imaginary()),
                    this.isExact() && csub.isExact());
        } else if (subtrahend.isCoercibleTo(RealType.class)) {
            try {
                RealType realval = (RealType) subtrahend.coerceTo(RealType.class);
                return new ComplexRectImpl((RealType) this.real().subtract(realval), this.imaginary(), exact && realval.isExact());
            } catch (CoercionException ex) {
                // we should never get here
                Logger.getLogger(ComplexPolarImpl.class.getName()).log(Level.SEVERE, "Failed to coerce subtrahend to RealType", ex);
            }
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        if (multiplier instanceof ComplexType) {
            ComplexType cmult = (ComplexType) multiplier;
            RealType modnew = (RealType) modulus.multiply(cmult.magnitude());
            RealType argnew = (RealType) argument.add(cmult.argument());
            ComplexPolarImpl result = new ComplexPolarImpl(modnew, argnew, exact && cmult.isExact());
            result.setMathContext(mctx);
            return result;
        } else if (multiplier.isCoercibleTo(RealType.class)) {
            try {
                RealType scalar = (RealType) multiplier.coerceTo(RealType.class);
                switch (scalar.sign()) {
                    case NEGATIVE:
                        Pi pi = Pi.getInstance(mctx);
                        RealType absval = scalar.magnitude();
                        return new ComplexPolarImpl((RealType) modulus.multiply(absval), (RealType) argument.add(pi), false);
                    default:
                        return new ComplexPolarImpl((RealType) modulus.multiply(scalar), (RealType) argument, exact && scalar.isExact());
                }
            } catch (CoercionException ex) {
                // we should never get here
                Logger.getLogger(ComplexPolarImpl.class.getName()).log(Level.SEVERE, "Failed to coerce multiplier to RealType.", ex);
            }
        }
        throw new UnsupportedOperationException("Unsupported type of multiplier.");
    }

    @Override
    public Numeric divide(Numeric divisor) {
        if (divisor instanceof ComplexType) {
            ComplexType cdiv = (ComplexType) divisor;
            RealType modnew = (RealType) modulus.divide(cdiv.magnitude());
            RealType argnew = (RealType) argument.subtract(cdiv.argument());
            ComplexPolarImpl result = new ComplexPolarImpl(modnew, argnew, exact && cdiv.isExact());
            result.setMathContext(mctx);
            return result;
        } else if (divisor.isCoercibleTo(RealType.class)) {
            try {
                RealType scalar = (RealType) divisor.coerceTo(RealType.class);
                switch (scalar.sign()) {
                    case ZERO:
                        throw new IllegalArgumentException("Division by zero not allowed.");
                    case NEGATIVE:
                        Pi pi = Pi.getInstance(mctx);
                        RealType absval = scalar.magnitude();
                        return new ComplexPolarImpl((RealType) modulus.divide(absval), (RealType) argument.subtract(pi), false);
                    default:
                        return new ComplexPolarImpl((RealType) modulus.divide(scalar), (RealType) argument);
                }
            } catch (CoercionException ex) {
                // we should never get here
                Logger.getLogger(ComplexPolarImpl.class.getName()).log(Level.SEVERE, "Failed to coerce divisor to RealType.", ex);
            }
        }
        throw new UnsupportedOperationException("Unsupported type of divisor.");
    }

    @Override
    public Numeric inverse() {
        // special case of division where the numerator has a modulus of 1
        // and an argument of 0
        return new ComplexPolarImpl((RealType) modulus.inverse(), argument.negate(), exact);
    }

    @Override
    public Numeric sqrt() {
        assert(modulus.sign() != Sign.NEGATIVE);
        // TODO add coercion since sqrt() can return an integer for perfect squares
        RealType modnew = (RealType) modulus.sqrt();
        RealType argnew = (RealType) argument.divide(TWO);
        return new ComplexPolarImpl(modnew, argnew, exact);
    }
    
    @Override
    public Set<ComplexType> nthRoots(IntegerType n) {
        ComplexType principalRoot;
        final long nLong = n.asBigInteger().longValueExact();
        if (nLong == 2L) {
            principalRoot = (ComplexType) sqrt();
        } else {
            try {
                principalRoot = new ComplexPolarImpl(MathUtils.nthRoot(modulus, n, mctx),
                        (RealType) argument.divide(n).coerceTo(RealType.class), exact);
            } catch (CoercionException ex) {
                Logger.getLogger(ComplexPolarImpl.class.getName()).log(Level.SEVERE, "Division by IntegerType n yielded non-coercible result.", ex);
                throw new ArithmeticException("Error computing principal root");
            }
        }
        Set<ComplexType> rootsOfUnity = MathUtils.rootsOfUnity(nLong, mctx);
        NumericSet result = new NumericSet();
        for (ComplexType root : rootsOfUnity) {
            result.append(principalRoot.multiply(root));
        }
        try {
            return result.coerceTo(ComplexType.class);
        } catch (CoercionException ex) {
            Logger.getLogger(ComplexPolarImpl.class.getName()).log(Level.SEVERE, "Error coercing all roots to ComplexType", ex);
            throw new IllegalStateException("Coercing one of n roots failed", ex);
        }
    }
    
    public Set<ComplexType> nthRoots(long n) {
        return nthRoots(new IntegerImpl(BigInteger.valueOf(n), true));
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ComplexType) {
            ComplexType that = (ComplexType) o;
            if (this.isExact() != that.isExact()) return false;
            return this.magnitude().equals(that.magnitude()) && this.argument.equals(that.argument());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.modulus);
        hash = 79 * hash + Objects.hashCode(this.argument);
        hash = 79 * hash + (this.exact ? 1 : 0);
        return hash;
    }
    
    @Override
    public String toString() {
        // returns this complex number in angle notation
        return modulus.toString() + " \u2220" + argument.toString();
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }
}
