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
import tungsten.types.numerics.impl.Zero;
import tungsten.types.vector.impl.OneVector;

/**
 * An Identity matrix (&#x1D7D9;) representation.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class IdentityMatrix extends DiagonalMatrix<Numeric> {
    private final MathContext mctx;
    private long elementCount;
    private final Numeric one;
    
    public IdentityMatrix(long size, MathContext mctx) {
        super(OneVector.getInstance(size, mctx));
        this.mctx = mctx;
        this.elementCount = size;
        this.one = One.getInstance(mctx);
    }
    
    @Override
    public Numeric valueAt(long row, long column) {
        if (row < 0L || row >= elementCount || column < 0L || column >= elementCount) {
            throw new IndexOutOfBoundsException("Row and column indices must be between 0 and " +
                    (elementCount - 1L) + ", inclusive.");
        }
        if (row == column) return One.getInstance(mctx);
        return Zero.getInstance(mctx);
    }
    
    @Override
    public long columns() { return elementCount; }
    
    @Override
    public long rows() { return elementCount; }
    
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
    public Matrix<Numeric> multiply(Matrix<Numeric> multiplier) {
        if (elementCount != multiplier.rows()) {
            throw new ArithmeticException("The multiplier must have the same number of rows as this matrix has columns.");
        }
        return multiplier;  // IA = A
    }
    
    @Override
    public Matrix<Numeric> add(Matrix<Numeric> addend) {
        if (addend.rows() != this.rows() || addend.columns() != this.columns()) {
            throw new ArithmeticException("Addend must match dimensions of this diagonal matrix.");
        }
        
        BasicMatrix<Numeric> result = new BasicMatrix<>(addend);
        for (long idx = 0L; idx < elementCount; idx++) {
            final Numeric sum = addend.valueAt(idx, idx).add(one);
            result.setValueAt(sum, idx, idx);
        }
        return result;
    }
    
    @Override
    public IdentityMatrix inverse() {
        // the identity matrix is its own inverse
        return this;
    }
    
    @Override
    public String toString() {
        return "\uD835\uDFD9";  // surrogate pair for 1D7D9
    }
}
