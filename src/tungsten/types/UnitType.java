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
package tungsten.types;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.units.ScalePrefix;
import tungsten.types.util.UnicodeTextEffects;

/**
 * A fundamental class representing types of units of measurement.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public abstract class UnitType {
    protected List<CompositionElement> elements;
    protected final ScalePrefix scalePrefix;
    protected static Map<ScalePrefix, UnitType> instanceMap = new EnumMap<>(ScalePrefix.class);
    
    public class CompositionElement {
        private final UnitType subunit;
        private int exponent;
        
        public CompositionElement(UnitType type, int exponent) {
            this.subunit = type;
            this.exponent = exponent;
        }
        
        public UnitType getType() { return subunit; }
        public int exponent() { return exponent; }
        public void addExponent(int exponent) { this.exponent += exponent; }
        
        @Override
        public int hashCode() {
            return Objects.hash(subunit, exponent);
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
            final CompositionElement other = (CompositionElement) obj;
            if (this.exponent != other.exponent) {
                return false;
            }
            if (!Objects.equals(this.subunit, other.subunit)) {
                return false;
            }
            return true;
        }
    }
    
    protected UnitType() {
        scalePrefix = null;
    }
    
    protected UnitType(ScalePrefix prefix) {
        scalePrefix = prefix;
    }
    
    protected static void cacheInstance(ScalePrefix prefix, UnitType t) {
        if (instanceMap.containsKey(prefix)) {
            Logger.getLogger(UnitType.class.getName()).log(Level.WARNING, "There is already a cached instance of {0} for {1}", new Object[]{t.getClass(), prefix.getName()});
        }
        instanceMap.put(prefix, t);
    }
    
    public List<CompositionElement> getComposition() {
        return elements == null ? Collections.emptyList() : elements;
    }
    
    public String getCompositionAsString() {
        StringBuilder buf = new StringBuilder();
        for (CompositionElement e : getComposition()) {
            buf.append(e.getType().unitSymbol());
            int exp = e.exponent();
            if (exp != 1) {
                buf.append(UnicodeTextEffects.numericSuperscript(exp));
            }
            buf.append(DOTMULT);
        }
        // now remove the last appended dot
        int index = buf.lastIndexOf(DOTMULT);
        int count = Character.charCount(buf.codePointAt(index));
        for (int k = 0; k < count; k++) buf.deleteCharAt(index);
        return buf.toString();
    }
    private static final String DOTMULT = "\u22C5"; // dot multiplier symbol
    
    protected void compose(UnitType other, int exponent) {
        if (elements == null) {
            elements = new ArrayList();
        }
        if (elements.stream().map(x -> x.getType()).anyMatch(x -> x.equals(other))) {
            elements.stream().filter(x -> x.getType().equals(other)).findFirst().ifPresent(x -> x.addExponent(exponent));
        } else {
            elements.add(new CompositionElement(other, exponent));
        }
        // if any exponents cancel out, remove the composition element
        elements.removeIf(x -> x.exponent() == 0);
    }
    
    public abstract String unitName();
    public abstract String unitSymbol();
    public abstract String unitIntervalSymbol();
    public abstract <R extends UnitType> Class<R> baseType();
    
    public abstract <R extends UnitType> Function<? extends Numeric, ? extends Numeric> getConversion(Class<R> clazz, MathContext mctx);
    
    protected boolean isSubtypeOfBase(Class<? extends UnitType> clazz) {
        final boolean assignable = baseType().isAssignableFrom(clazz);
        
        if (!assignable) {
            Logger.getLogger(UnitType.class.getName()).log(Level.WARNING, "Mismatched unit types: {0} is not a subtype of {1}",
                    new Object[]{clazz.getTypeName(), baseType().getTypeName()});
        }
        return assignable;
    }
    
    public BigDecimal getScale() {
        return scalePrefix == null ? BigDecimal.ONE : scalePrefix.getScale();
    }
    
    public UnitType obtainScaledUnit(ScalePrefix prefix) {
        if (instanceMap.containsKey(prefix)) {
            return instanceMap.get(prefix);
        }
        Logger.getLogger(UnitType.class.getName()).log(Level.FINEST,
                "No instance of {0} found for {1}; should be instantiated and cached in subclass.",
                new Object[]{this.getClass().getTypeName(), prefix.getName()});
        return null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof UnitType) {
            UnitType type = (UnitType) o;
            return type.unitName().equals(this.unitName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this.elements);
        hash = 13 * hash + Objects.hashCode(this.unitName());
        return hash;
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (scalePrefix != null) {
            buf.append(scalePrefix.getSymbol());
        }
        buf.append(unitSymbol());
        return buf.toString();
    }
}
