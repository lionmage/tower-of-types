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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.ComplexRectImpl;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.util.OptionalOperations;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class ComplexVector implements Vector<ComplexType> {
    private List<ComplexType> elements;
    private MathContext mctx = MathContext.UNLIMITED;
    
    /**
     * Creates a new empty instance of {@link ComplexVector} with room for
     * {@code initialCapacity} elements.
     * @param initialCapacity the desired initial capacity for this vector
     */
    public ComplexVector(long initialCapacity) {
        if (initialCapacity > (long) Integer.MAX_VALUE) {
            throw new IllegalArgumentException("This implementation of Vector cannot store " + initialCapacity + " elements.");
        }
        this.elements = new ArrayList<>((int) initialCapacity);
    }
    
    public ComplexVector(List<ComplexType> elements) {
        this.elements = elements;
    }
    
    public ComplexVector(ComplexType[] cplxArray, MathContext mctx) {
        this.mctx = mctx;
        this.elements = Arrays.stream(cplxArray).sequential().peek(x -> OptionalOperations.setMathContext(x, mctx)).collect(Collectors.toList());
    }
    
    public void setMathContext(MathContext mctx) {
        if (mctx == null) {
            throw new IllegalArgumentException("MathContext must not be null");
        }
        this.mctx = mctx;
    }

    @Override
    public long length() {
        return (long) elements.size();
    }

    @Override
    public ComplexType elementAt(long position) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setElementAt(ComplexType element, long position) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void append(ComplexType element) {
        elements.add(element);
    }

    @Override
    public Vector<ComplexType> add(Vector<ComplexType> addend) {
        if (this.length() != addend.length()) {
            throw new ArithmeticException("Cannot add vectors of different length");
        }
        ComplexVector result = new ComplexVector(new ArrayList<>(elements.size()));
        for (long idx = 0L; idx < length(); idx++) {
            ComplexType sum = (ComplexType) this.elementAt(idx).add(addend.elementAt(idx));
            result.setElementAt(sum, idx);
        }
        OptionalOperations.setMathContext(result, mctx);
        return result;
    }

    @Override
    public Vector<ComplexType> subtract(Vector<ComplexType> subtrahend) {
        if (this.length() != subtrahend.length()) {
            throw new ArithmeticException("Cannot add vectors of different length");
        }
        ComplexVector result = new ComplexVector(new ArrayList<>(elements.size()));
        for (long idx = 0L; idx < length(); idx++) {
            ComplexType difference = (ComplexType) this.elementAt(idx).subtract(subtrahend.elementAt(idx));
            result.setElementAt(difference, idx);
        }
        OptionalOperations.setMathContext(result, mctx);
        return result;
    }

    @Override
    public Vector<ComplexType> negate() {
        List<ComplexType> list = elements.stream().sequential().map(x -> x.negate()).collect(Collectors.toList());
        final ComplexVector result = new ComplexVector(list);
        result.setMathContext(mctx);
        return result;
    }

    @Override
    public Vector<ComplexType> scale(ComplexType factor) {
        // TODO ensure we don't need to coerce to ComplexType, which would require handling an exception
        List<ComplexType> list = elements.stream().sequential().map(x -> (ComplexType) x.multiply(factor)).collect(Collectors.toList());
        final ComplexVector result = new ComplexVector(list);
        result.setMathContext(mctx);
        return result;
    }

    @Override
    public ComplexType magnitude() {
        Numeric result = this.dotProduct(this).sqrt();
        OptionalOperations.setMathContext(result, mctx);
        if (result instanceof ComplexType) {
            ComplexType cplx = (ComplexType) result;
            assert(cplx.imaginary().asBigDecimal().compareTo(BigDecimal.ZERO) == 0);
            return cplx;
        } else if (result instanceof RealType) {
            RealType real = (RealType) result;
            RealType zero = new RealImpl(BigDecimal.ZERO, true);
            return new ComplexRectImpl(real, zero);
        } else {
            try {
                return (ComplexType) result.coerceTo(ComplexType.class);
            } catch (CoercionException ex) {
                Logger.getLogger(ComplexVector.class.getName()).log(Level.SEVERE, "Could not coerce magnitude to complex type.", ex);
                throw new IllegalStateException("Failed coercion while computing magnitude.", ex);
            }
        }
    }

    @Override
    public ComplexType dotProduct(Vector<ComplexType> other) {
        if (this.length() != other.length()) {
            throw new ArithmeticException("Cannot compute dot product for vectors of different length");
        }
        final RealType zero = new RealImpl(BigDecimal.ZERO);
        ComplexType accum = new ComplexRectImpl(zero, zero, true);
        for (long idx = 0L; idx < this.length(); idx++) {
            accum = (ComplexType) accum.add(this.elementAt(idx).multiply(other.elementAt(idx).conjugate()));
        }
        OptionalOperations.setMathContext(accum, mctx);
        return accum;
    }

    @Override
    public Vector<ComplexType> crossProduct(Vector<ComplexType> other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Compute the angle &theta; between this vector and the given vector.
     * @param other the other vector
     * @return the angle &theta; between this and {@code other}
     */
    public RealType computeAngle(Vector<ComplexType> other) {
        try {
            RealType cosine = (RealType) this.dotProduct(other).real()
                    .divide(this.magnitude().coerceTo(RealType.class)
                            .multiply(other.magnitude().coerceTo(RealType.class)));
            BigDecimal angle = BigDecimalMath.acos(cosine.asBigDecimal(), mctx);
            final RealImpl radangle = new RealImpl(angle);
            radangle.setMathContext(mctx);
            return radangle;
        } catch (CoercionException ex) {
            Logger.getLogger(ComplexVector.class.getName()).log(Level.SEVERE, "Could not coerce vector magnitude to real.", ex);
            throw new ArithmeticException("Unable to compute angle between two complex vectors");
        }
    }

    @Override
    public Vector<ComplexType> normalize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }
}
