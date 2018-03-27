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

import java.util.ArrayList;
import java.util.List;

/**
 * A fundamental class representing types of units of measurement.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public abstract class UnitType {
    protected List<CompositionElement> elements;
    
    public class CompositionElement {
        private UnitType subunit;
        private int exponent;
        
        public CompositionElement(UnitType type, int exponent) {
            this.subunit = type;
            this.exponent = exponent;
        }
        
        public UnitType getType() { return subunit; }
        public int exponent() { return exponent; }
        public void addExponent(int exponent) { this.exponent += exponent; }
    }
    
    public List<CompositionElement> getComposition() {
        return elements;
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
}
