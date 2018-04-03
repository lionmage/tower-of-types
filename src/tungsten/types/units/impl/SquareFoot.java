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

import java.math.MathContext;
import java.util.function.Function;
import tungsten.types.Numeric;
import tungsten.types.UnitType;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.units.Area;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class SquareFoot extends Area {
    private static final SquareFoot instance = new SquareFoot();
    
    private SquareFoot() {
        super();
        composeFromLength(Foot.getInstance());
    }

    @Override
    public String unitName() {
        return "square foot";
    }

    @Override
    public String unitSymbol() {
        return "sqft";
    }

    @Override
    public String unitIntervalSymbol() {
        return "sqft";
    }

    @Override
    public <R extends UnitType> Function<Numeric, ? extends Numeric> getConversion(Class<R> clazz, MathContext mctx) {
        if (!isSubtypeOfBase(clazz)) throw new UnsupportedOperationException("Bad unit conversion.");
        
        if (SquareInch.class.isAssignableFrom(clazz)) {
            final Numeric sqInPerSqFoot = new IntegerImpl("12").pow(new IntegerImpl("2"));
            return x -> x.multiply(sqInPerSqFoot);
        } else if (SquareMeter.class.isAssignableFrom(clazz)) {
            final RealImpl factor = new RealImpl("10.7639", false);
            factor.setMathContext(mctx);
            return x -> x.divide(factor);
        }
        throw new UnsupportedOperationException("Cannot convert SquareFoot to " + clazz.getSimpleName());
    }
    
}
