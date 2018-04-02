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

import java.math.BigInteger;
import java.math.MathContext;
import java.util.function.Function;
import tungsten.types.Numeric;
import tungsten.types.UnitType;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.units.Length;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class Inch extends Length {
    private static final Inch instance = new Inch();
    
    private Inch() { super(); }
    
    public Inch getInstance() { return instance; }

    @Override
    public String unitName() {
        return "inch";
    }

    @Override
    public String unitSymbol() {
        return "in";
    }

    @Override
    public String unitIntervalSymbol() {
        return "in";
    }

    @Override
    public <R extends UnitType> Function<Numeric, ? extends Numeric> getConversion(Class<R> clazz, MathContext mctx) {
        if (!isSubtypeOfBase(clazz)) throw new UnsupportedOperationException("Bad unit conversion.");

        if (Inch.class.isAssignableFrom(clazz)) {
            return x -> x.divide(new IntegerImpl(BigInteger.valueOf(12L)));
        }
        throw new UnsupportedOperationException("Cannot convert Inch to " + clazz.getSimpleName());
    }
    
}
