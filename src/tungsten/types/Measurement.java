/*
 * The MIT License
 *
 * Copyright 2018 Robert Poole.
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
package tungsten.types;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.units.ScalePrefix;
import tungsten.types.util.OptionalOperations;

/**
 * Encapsulates a physical measurement of some type.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <V> the measurement value type
 * @param <U> the measurement unit type
 */
public class Measurement<V extends Numeric, U extends UnitType> {
    private final V value;
    private final U unit;
    private MathContext mctx;
    
    public Measurement(V value, U unit) {
        this.value = value;
        this.unit = unit;
        mctx = value.getMathContext();
    }
    
    public V getValue() {
        return value;
    }
    
    public U getUnit() {
        return unit;
    }
    
    public Measurement rescale(ScalePrefix prefix) {
        if (prefix == unit.getScalePrefix()) {
            return this;
        }
        BigDecimal oldScale = unit.getScale();
        BigDecimal newScale = prefix.getScale();
        try {
            RealType realValue = (RealType) value.coerceTo(RealType.class);
            BigDecimal newValue = realValue.asBigDecimal().multiply(oldScale, mctx).divide(newScale, mctx);
            RealImpl result = new RealImpl(newValue, value.isExact());
            result.setMathContext(mctx);
            UnitType newUnit = unit.obtainScaledUnit(prefix);
            return new Measurement(result, newUnit);
        } catch (CoercionException ce) {
            throw new ArithmeticException("Unable to rescale measurement from " + unit + " to " + prefix.getName());
        }
    }
    
    public <T extends UnitType> Measurement convertTo(T targetUnit) {
        BigDecimal oldScale = unit.getScale();
        BigDecimal newScale = targetUnit.getScale();
        try {
            RealType realValue = (RealType) value.coerceTo(RealType.class);
            RealImpl intermediate = new RealImpl(realValue.asBigDecimal().multiply(oldScale), value.isExact());
            intermediate.setMathContext(mctx);
            Function<Numeric, ? extends Numeric> func = unit.getConversion(targetUnit.getClass(), mctx);
            RealType converted = (RealType) func.andThen(x -> x.divide(new RealImpl(newScale))).apply(intermediate).coerceTo(RealType.class);
            return new Measurement(converted, targetUnit);
        } catch (CoercionException ce) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unable to coerce measurement unit from " + unit + " to " + targetUnit, ce);
            throw new ArithmeticException("Unanle to coerce measurement from " + unit + " to " + targetUnit);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(value).append(' ').append(unit);
        return buf.toString();
    }
}
