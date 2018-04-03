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
package tungsten.types.units.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Function;
import tungsten.types.Numeric;
import tungsten.types.UnitType;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.Pi;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.units.Angle;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class Degree extends Angle {
    private static final Degree instance = new Degree();
    
    private Degree() { super(); }
    
    public Degree getInstance() {
        return instance;
    }

    @Override
    public String unitName() {
        return "degree";
    }

    @Override
    public String unitSymbol() {
        return "\u00B0";
    }

    @Override
    public String unitIntervalSymbol() {
        return "\u00B0";
    }
    
    public static RealType dmsToDecimal(IntegerType degrees, IntegerType minutes, IntegerType seconds, MathContext mctx) {
        final BigDecimal MINUTES_PER_DEGREE = new BigDecimal("60", mctx);
        final BigDecimal SECONDS_PER_DEGREE = new BigDecimal("3600", mctx);
        BigDecimal decDegrees = new BigDecimal(degrees.asBigInteger(), mctx);
        BigDecimal decMinutes = new BigDecimal(minutes.asBigInteger(), mctx);
        BigDecimal decSeconds = new BigDecimal(seconds.asBigInteger(), mctx);
        decDegrees = decDegrees.add(decMinutes.divide(MINUTES_PER_DEGREE, mctx), mctx);
        decDegrees = decDegrees.add(decSeconds.divide(SECONDS_PER_DEGREE, mctx), mctx);
        RealImpl result = new RealImpl(decDegrees, true);
        result.setMathContext(mctx);
        return result;
    }

    @Override
    public <R extends UnitType> Function<Numeric, ? extends Numeric> getConversion(Class<R> clazz, MathContext mctx) {
        if (!isSubtypeOfBase(clazz)) throw new UnsupportedOperationException("Bad unit conversion.");
        
        if (Radian.class.isAssignableFrom(clazz)) {
            final Pi pi = Pi.getInstance(mctx);
            final RealImpl halfCircDegrees = new RealImpl("180.0", true);
            return x -> x.multiply(pi).divide(halfCircDegrees);
        }
        
        throw new UnsupportedOperationException("Cannot convert Degree to " + clazz.getSimpleName());
    }
    
}
