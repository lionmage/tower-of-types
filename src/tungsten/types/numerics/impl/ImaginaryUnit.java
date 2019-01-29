/*
 * The MIT License
 *
 * Copyright © 2019 Robert Poole <Tarquin.AZ@gmail.com>.
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
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tungsten.types.Numeric;
import tungsten.types.Set;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;

/**
 * A representation of the imaginary unit, or the unit imaginary number.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class ImaginaryUnit implements ComplexType {
    private static final RealType TWO = new RealImpl(BigDecimal.valueOf(2L));

    private final MathContext mctx;
    
    private ImaginaryUnit(MathContext mctx) {
        this.mctx = mctx;
    }

    private static final Map<MathContext, ImaginaryUnit> instanceMap = new HashMap<>();
    private static final Lock instanceLock = new ReentrantLock();

    public static Numeric getInstance(MathContext mctx) {
        instanceLock.lock();
        try {
            ImaginaryUnit instance = instanceMap.get(mctx);
            if (instance == null) {
                instance = new ImaginaryUnit(mctx);
                instanceMap.put(mctx, instance);
            }
            return instance;
        } finally {
            instanceLock.unlock();
        }
    }

    @Override
    public RealType magnitude() {
        final RealImpl one = new RealImpl(BigDecimal.ONE);
        one.setMathContext(mctx);
        return one;
    }

    @Override
    public ComplexType negate() {
        return new ComplexRectImpl(real().negate(), imaginary().negate());
    }

    @Override
    public ComplexType conjugate() {
        return new ComplexRectImpl(real(), imaginary().negate());
    }

    @Override
    public RealType real() {
        final RealImpl real = new RealImpl(BigDecimal.ZERO);
        real.setMathContext(mctx);
        return real;
    }

    @Override
    public RealType imaginary() {
        final RealImpl imag = new RealImpl(BigDecimal.ONE);
        imag.setMathContext(mctx);
        return imag;
    }

    @Override
    public RealType argument() {
        // since this value lies on the positive imaginary axis, the argument is pi/2
        return (RealType) Pi.getInstance(mctx).divide(TWO);
    }

    @Override
    public Set<ComplexType> nthRoots(IntegerType n) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isExact() {
        return true;
    }

    @Override
    public boolean isCoercibleTo(Class<? extends Numeric> numtype) {
        return numtype.isAssignableFrom(ComplexType.class);
    }

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        return new ComplexRectImpl(real(), imaginary());
    }

    @Override
    public Numeric add(Numeric addend) {
        if (addend instanceof ImaginaryUnit) {
            return new ComplexRectImpl(real(), (RealType) imaginary().multiply(TWO));
        }
        return addend.add(this);
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        if (subtrahend instanceof ImaginaryUnit) {
            return Zero.getInstance(mctx);
        }
        return subtrahend.negate().add(this);
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        if (multiplier instanceof ImaginaryUnit) {
            ComplexType self = new ComplexRectImpl(real(), imaginary());
            return self.multiply(self);
        }
        return multiplier.multiply(this);
    }

    @Override
    public Numeric divide(Numeric divisor) {
        if (divisor instanceof ImaginaryUnit) {
            return One.getInstance(mctx);
        }
        return divisor.inverse().multiply(this);
    }

    @Override
    public Numeric inverse() {
        return this.conjugate();
    }

    @Override
    public Numeric sqrt() {
        return new ComplexRectImpl(real(), imaginary()).sqrt();
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }
    
    @Override
    public String toString() {
        return "\u2148";
    }
}
