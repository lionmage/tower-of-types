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
package tungsten.types.units;

import tungsten.types.UnitType;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public abstract class Angle extends UnitType {
    public Angle() { super(); }
    public Angle(ScalePrefix prefix) { super(prefix); }
    
    /** 
     * Subclasses should use this to easily create the unit composition.
     * All measures of area are derived from units of length.
     * @param lengthType the unit of length
     */
    protected void composeFromLength(Length lengthType) {
        this.compose(lengthType, 3);
    }

    @Override
    public Class<Angle> baseType() {
        return Angle.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Angle) {
            super.equals(o);
        }
        return false;
    }
}
