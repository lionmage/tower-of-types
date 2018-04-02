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
package tungsten.types.units.impl;

import java.math.MathContext;
import java.util.function.Function;
import tungsten.types.Numeric;
import tungsten.types.UnitType;
import tungsten.types.numerics.RationalType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.RationalImpl;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.units.Temperature;
import tungsten.types.util.OptionalOperations;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class Fahrenheit extends Temperature {
    private static final Fahrenheit instance = new Fahrenheit();
    
    private Fahrenheit() {
        super();
    }
    
    public static Fahrenheit getInstance() {
        return instance;
    }

    @Override
    public boolean isAbsolute() {
        return false;
    }

    @Override
    public <R extends UnitType> Function<Numeric, ? extends Numeric> getConversion(Class<R> clazz, MathContext mctx) {
        if (!isSubtypeOfBase(clazz)) throw new UnsupportedOperationException("Bad unit conversion.");
        
        if (Celsius.class.isAssignableFrom(clazz)) {
            final RationalType ratio = new RationalImpl("5/9");
            OptionalOperations.setMathContext(ratio, mctx);
            final RealType offset = new RealImpl("32.0");
            OptionalOperations.setMathContext(offset, mctx);
            return x -> x.subtract(offset).multiply(ratio);
        }
        
        throw new UnsupportedOperationException("Cannot convert Fahrenheit to " + clazz.getSimpleName());
    }

    @Override
    public String unitName() {
        return "Fahrenheit";
    }

    @Override
    public String unitSymbol() {
        return "\u00B0F";
    }

    @Override
    public String unitIntervalSymbol() {
        return "\u00B0F";
    }
    
}
