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

import java.math.MathContext;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.vector.impl.ColumnVector;
import tungsten.types.vector.impl.RowVector;
import tungsten.types.vector.impl.ZeroVector;

/**
 * A representation of the zero matrix, of which all elements
 * are zero (0).
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class ZeroMatrix extends ParametricMatrix<Numeric> {
    private final MathContext mctx;
    
    public ZeroMatrix(long size, MathContext mctx) {
        super(size, size, (row, column) -> Zero.getInstance(mctx));
        this.mctx = mctx;
    }
    
    public ZeroMatrix(long rows, long columns, MathContext mctx) {
        super(rows, columns, (row, column) -> Zero.getInstance(mctx));
        this.mctx = mctx;
    }
    
    @Override
    public RowVector<Numeric> getRow(long row) {
        return new RowVector<>(ZeroVector.getInstance(this.columns(), mctx));
    }
    
    @Override
    public ColumnVector<Numeric> getColumn(long column) {
        return new ColumnVector<>(ZeroVector.getInstance(this.rows(), mctx));
    }
    
    @Override
    public Matrix<Numeric> add(Matrix<Numeric> addend) {
        if (addend.rows() != this.rows() || addend.columns() != this.columns()) {
            throw new ArithmeticException("Dimensional mismatch.");
        }
        return addend;
    }
    
    @Override
    public Matrix<Numeric> multiply(Matrix<Numeric> multiplier) {
        if (this.columns() != multiplier.rows()) {
            throw new ArithmeticException("Multiplier must have the same number of rows as this matrix has columns.");
        }
        return new ZeroMatrix(this.rows(), multiplier.columns(), mctx);
    }
    
    @Override
    public Numeric determinant() {
        if (columns() != rows()) throw new ArithmeticException("Cannot compute determinant of a non-square matrix");
        return Zero.getInstance(mctx);
    }
    
    @Override
    public Numeric trace() {
        if (columns() != rows()) throw new ArithmeticException("Cannot compute trace of a non-square matrix");
        return Zero.getInstance(mctx);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ZeroMatrix) {
            ZeroMatrix that = (ZeroMatrix) o;
            if (this == that) return true;
            return this.rows() == that.rows() && this.columns() == that.columns();
        }
        return super.equals(o);
    }
}
