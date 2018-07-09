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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import tungsten.types.Numeric;
import tungsten.types.UnitType;
import static tungsten.types.UnitType.obtainScaledUnit;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.units.ScalePrefix;
import tungsten.types.units.Volume;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class CubicMeter extends Volume {
    private static final CubicMeter instance = new CubicMeter();
    
    private CubicMeter() {
        super();
        composeFromLength(Meter.getInstance());
    }
    
    private CubicMeter(ScalePrefix prefix) {
        super(prefix);
        composeFromLength(Meter.getInstance(prefix));
    }
    
    public CubicMeter getInstance() { return instance; }
    
    private static Lock instanceLock = new ReentrantLock();
    
    public static CubicMeter getInstance(ScalePrefix scalePrefix) {
        instanceLock.lock();
        try {
            CubicMeter result = (CubicMeter) obtainScaledUnit(scalePrefix);
            if (result == null) {
                result = new CubicMeter(scalePrefix);
                cacheInstance(scalePrefix, result);
            }
            
            return result;
        } finally {
            instanceLock.unlock();
        }
    }

    @Override
    public String unitName() {
        return "cubic meter";
    }

    @Override
    public String unitSymbol() {
        return getCompositionAsString();
    }

    @Override
    public String unitIntervalSymbol() {
        return getCompositionAsString();
    }

    @Override
    public <R extends UnitType> Function<Numeric, ? extends Numeric> getConversion(Class<R> clazz, MathContext mctx) {
        if (!isSubtypeOfBase(clazz)) throw new UnsupportedOperationException("Bad unit conversion.");
        
        if (Liter.class.isAssignableFrom(clazz)) {
            final Numeric litersPerCubicMeter = new IntegerImpl("1000");
            return x -> x.multiply(litersPerCubicMeter);
        }
        throw new UnsupportedOperationException("Cannot convert CubicMeter to " + clazz.getSimpleName());
    }
    
}
