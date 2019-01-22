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
package tungsten.types.matrix.impl;

import java.math.BigInteger;
import java.math.MathContext;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.One;
import tungsten.types.vector.impl.OneVector;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class IdentityMatrix extends DiagonalMatrix<Numeric> {
    private final MathContext mctx;
    private long elementCount;
    
    public IdentityMatrix(long size, MathContext mctx) {
        super(OneVector.getInstance(size, mctx));
        this.mctx = mctx;
        this.elementCount = size;
    }
    
    @Override
    public Numeric determinant() {
        return One.getInstance(mctx);
    }
    
    @Override
    public Numeric trace() {
        // this could be any Numeric subtype, really, but IntegerImpl has less overhead
        return new IntegerImpl(BigInteger.valueOf(elementCount));
    }
    
    @Override
    public IdentityMatrix inverse() {
        // the identity matrix is its own inverse
        return this;
    }
}
