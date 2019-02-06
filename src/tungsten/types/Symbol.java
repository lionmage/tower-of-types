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
package tungsten.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.numerics.impl.Euler;
import tungsten.types.numerics.impl.ImaginaryUnit;
import tungsten.types.numerics.impl.One;
import tungsten.types.numerics.impl.Pi;
import tungsten.types.numerics.impl.Zero;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class Symbol {
    private final String name;
    private final String representation;
    private Class<? extends Numeric> valueClass;
    private Numeric concreteValue;  // TODO can this be made to handle objects like matrix, vector, function, etc.?
    
    private static final ConcurrentMap<String, Symbol> cache = new ConcurrentHashMap<>();
    
    public Symbol(String name, String representation) {
        this.name = name;
        this.representation = representation;
        cacheThis(name);
    }
    
    /**
     * Some symbols have a name that is the same as the representation.
     * @param name 
     */
    public Symbol(String name) {
        this.name = name;
        this.representation = name;
        cacheThis(name);
    }
    
    public Symbol(String name, String representation, Class<? extends Numeric> valueClass) {
        this(name, representation);
        this.valueClass = valueClass;
    }
    
    private void cacheThis(String key) {
        Symbol old = cache.put(key, this);
        if (old != null) {
            Logger.getLogger(Symbol.class.getName()).log(Level.WARNING,
                    "Symbol named {} redefined from {} to {}", new Object[] {key, old, this});
        }
    }
    
    public static java.util.Set<String> getAllSymbols() {
        return cache.keySet();
    }
    
    public static Symbol getForName(String name) {
        return cache.get(name);
    }
    
    public static Symbol getOrCreate(String name, String representation) {
        return cache.getOrDefault(name, new Symbol(name, representation));
    }

    public String getName() {
        return name;
    }

    public String getRepresentation() {
        return representation;
    }
    
    /**
     * Returns the {@link Class} of the numeric value represented by this
     * symbol if and only if this symbol represents a constant.
     * 
     * @return an {@link Optional} which contains the {@link Class} of a numeric constant, if present
     */
    public Optional<Class<? extends Numeric>> getValueClass() {
        return Optional.ofNullable(valueClass);
    }
    
    public void setConcreteValue(Numeric value) {
        if (getValueClass().isPresent()) {
            throw new UnsupportedOperationException("Symbol " + name + " is a constant.");
        }
        if (concreteValue != null) {
            final Class<? extends Numeric> oldClass = concreteValue.getClass();
            final Class<? extends Numeric> newClass = value.getClass();
            if (!oldClass.isAssignableFrom(newClass)) {
                Logger.getLogger(Symbol.class.getName()).log(Level.WARNING,
                        "Symbol {} changing type of bound value from {} to {}.",
                        new Object[] {name, oldClass.getTypeName(), newClass.getTypeName()});
            }
        }
        concreteValue = Objects.requireNonNull(value, "Cannot undefine symbol value.");
    }
    
    public Optional<? extends Numeric> getConcreteValue() {
        return Optional.ofNullable(concreteValue);
    }
    
    public Optional<? extends Numeric> getValueInstance(MathContext mctx) {
        Optional<Class<? extends Numeric>> clazz = getValueClass();
        if (!clazz.isPresent()) return Optional.empty();
        
        // I wanted to use Optional.ifPresent() here, but lambdas actually got
        // in the way.
        try {
            Method m = clazz.get().getMethod("getInstance", MathContext.class);
            return Optional.of(clazz.get().cast(m.invoke(null, mctx)));  // m is a static method
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(Symbol.class.getName()).log(Level.SEVERE,
                    "Unable to obtain an instance of " + clazz.get().getTypeName(), ex);
        }
        return Optional.empty();
    }

    private static DecimalFormat obtainDecimalFormat() {
        NumberFormat format = DecimalFormat.getInstance();
        if (format instanceof DecimalFormat) {
            return (DecimalFormat) format;
        } else {
            Logger.getLogger(Symbol.class.getName()).log(Level.WARNING,
                    "Tried to obtain a {} instance, but received {} instead.",
                    new Object[] {DecimalFormat.class.getTypeName(), format.getClass().getTypeName()});
            return new DecimalFormat();
        }
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.name);
        hash = 53 * hash + Objects.hashCode(this.representation);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Symbol other = (Symbol) obj;
        return Objects.equals(this.name, other.name);
    }
    
    @Override
    public String toString() {
        return representation;
    }
    
    public static final Symbol theta = new Symbol("theta", "\u0398");
    public static final Symbol phi   = new Symbol("phi", "\u03C6");
    public static final Symbol zero  = new Symbol("zero",
            String.valueOf(obtainDecimalFormat().getDecimalFormatSymbols().getZeroDigit()),
            Zero.class);
    public static final Symbol pi    = new Symbol("pi", "\uD835\uDF0B", Pi.class);
    public static final Symbol euler = new Symbol("euler", "\u212F", Euler.class);
    public static final Symbol one   = new Symbol("one",
            String.valueOf(Character.forDigit(1, 10)), One.class);
    public static final Symbol imaginary_unit =
            new Symbol("imaginary unit", "\u2148", ImaginaryUnit.class);
}
