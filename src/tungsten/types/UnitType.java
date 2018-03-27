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

import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.units.Temperature;

/**
 * A fundamental class representing types of units of measurement.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public abstract class UnitType {
    protected List<CompositionElement> elements;
    
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
    }
    
    public List<CompositionElement> getComposition() {
        return elements == null ? Collections.emptyList() : elements;
    }
    
    // Unicode superscript numerals 0 - 9
    private static final String[] superscripts = {
        "\u2070", "\u00B9", "\u00B2", "\u00B3", "\u2074", "\u2075",
        "\u2076", "\u2077", "\u2078", "\u2079"
    };
    private static final String negativeSup = "\u207B";
    
    public String getCompositionAsString() {
        StringBuilder buf = new StringBuilder();
        for (CompositionElement e : getComposition()) {
            buf.append(e.getType().unitSymbol());
            int exp = e.exponent();
            if (exp != 1) {
                if (exp < 0) buf.append(negativeSup);
                if (Math.abs(exp) < 10) buf.append(superscripts[Math.abs(exp)]);
            }
        }
        return buf.toString();
    }
    
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
}
