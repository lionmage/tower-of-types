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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.NumericHierarchy;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.vector.impl.ColumnVector;
import tungsten.types.vector.impl.RowVector;

/**
 * Basic concrete implementation of {@link Matrix}.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the numeric type of this matrix
 */
public class BasicMatrix<T extends Numeric> implements Matrix<T> {
    private List<RowVector<T>> rows = new ArrayList<>();
    
    public BasicMatrix() {
    }
    
    /**
     * Construct a matrix from a 2D array.
     * Note that this constructor assumes the first array index
     * is the row, and the second array index is the column.
     * @param source a two-dimensional array
     */
    public BasicMatrix(T[][] source) {
        for (int i = 0; i < source.length; i++) {
            append(new RowVector<>(source[i]));
        }
    }
    
    /**
     * Copy constructor.
     * @param source the matrix to copy
     */
    public BasicMatrix(Matrix<T> source) {
        for (long row = 0L; row < source.rows(); row++) {
            append(source.getRow(row));
        }
    }

    @Override
    public long columns() {
        if (!rows.isEmpty()) {
            return rows.get(0).columns();
        }
        throw new IllegalStateException("This matrix has no rows.");
    }

    @Override
    public long rows() {
        return (long) rows.size();
    }

    @Override
    public T valueAt(long row, long column) {
        if (row < 0L || row >= rows() || column < 0L || column >= columns()) {
            throw new IndexOutOfBoundsException("Row and column indices must be within bounds.");
        }
        return rows.get((int) row).elementAt((int) column);
    }
    
    public void setValueAt(T value, long row, long column) {
        RowVector<T> currentRow = this.getRow(row);
        currentRow.setElementAt(value, column);
    }

    @Override
    public T determinant() {
        if (rows() != columns()) {
            throw new ArithmeticException("Can only compute determinant for a square matrix.");
        }
        if (rows() == 1L) { // 1x1 matrix
            return valueAt(0L, 0L);
        }
        if (rows() == 2L) {
            T a = valueAt(0L, 0L);
            T b = valueAt(0L, 1L);
            T c = valueAt(1L, 0L);
            T d = valueAt(1L, 1L);
            return (T) a.multiply(d).subtract(c.multiply(b));  // should not require coercion here
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
        if (this.rows() != addend.rows() || this.columns() != addend.columns()) {
            throw new ArithmeticException("Addend must match dimensions of matrix.");
        }
        BasicMatrix<T> result = new BasicMatrix<>();
        for (long row = 0L; row < rows(); row++) {
            // casting to Vector<T> to avoid ambiguity over which add() method to use
            RowVector<T> rowsum = this.getRow(row).add((Vector<T>) addend.getRow(row));
            result.append(rowsum);
        }
        return result;
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public RowVector<T> getRow(long row) {
        if (row < 0L || row >= rows()) {
            throw new IndexOutOfBoundsException("Row index is out of range.");
        }
        return rows.get((int) row);
    }
    
    /**
     * Append a row to this matrix.
     * @param row a row vector representing the new row to append
     */
    public final void append(RowVector<T> row) {
        if (rows.isEmpty() || row.columns() == this.columns()) {
            rows.add(row);
        } else {
            throw new IllegalArgumentException("Expected a row vector with " + this.columns() +
                    " columns, but received one with " + row.columns() + " instead.");
        }
    }
    
    /**
     * Convenience method for internal methods and subclasses to manipulate
     * this matrix by appending a row.  Useful for when arrays are being
     * generated in intermediate computational steps, e.g. for speed.
     * 
     * @param row an array of type T
     */
    protected void append(T[] row) {
        append(new RowVector<>(row));
    }
    
    /**
     * Append a column to this matrix.
     * @param column a column vector representing the new column to append
     */
    public void append(ColumnVector<T> column) {
        if (column.rows() != this.rows()) {
            throw new IllegalArgumentException("Column vector has wrong number of elements.");
        }
        for (long rowidx = 0; rowidx < this.rows(); rowidx++) {
            rows.get((int) rowidx).append(column.elementAt(rowidx));
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        rows.forEach(rowvec -> {
            // TODO let's pretty up the formatting!  This works for now.
            // Ideally, I'd like to align each column on the decimal point
            // (or something clever like that).
            buf.append(rowvec.toString()).append('\n');
        });
        return buf.toString();
    }

    @Override
    public Matrix<? extends Numeric> inverse() {
        if (rows() != columns()) {
            throw new ArithmeticException("Cannot invert a non-square matrix.");
        }
        final T det = this.determinant();
        if (det.equals(Zero.getInstance(valueAt(0L, 0L).getMathContext()))) {
            throw new ArithmeticException("Matrix is singular.");
        }
        if (rows() == 1L) {
            return new SingletonMatrix(valueAt(0L, 0L).inverse());
        } else if (rows() == 2L) {
            final Numeric scale = det.inverse();
            T a = valueAt(0L, 0L);
            T b = valueAt(0L, 1L);
            T c = valueAt(1L, 0L);
            T d = valueAt(1L, 1L);
            BasicMatrix<Numeric> result = new BasicMatrix<>();
            result.append(new RowVector(d, b.negate()).scale(scale));
            result.append(new RowVector(c.negate(), a).scale(scale));
            return result;
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public <R extends Numeric> Matrix<R> upconvert(Class<R> clazz) {
        // first, check to make sure we can do this -- ensure R is a wider type than T
        NumericHierarchy targetType = NumericHierarchy.forNumericType(clazz);
        NumericHierarchy currentType = NumericHierarchy.forNumericType(valueAt(0L, 0L).getClass());
        // if our elements are already of the requested type, just cast and return
        if (currentType == targetType) return (Matrix<R>) this;
        if (currentType.compareTo(targetType) > 0) {
            throw new ArithmeticException("Cannot upconvert elements of " + currentType + " to elements of " + targetType);
        }
        BasicMatrix<R> result = new BasicMatrix<>();
        for (long row = 0L; row < rows(); row++) {
            R[] accum = (R[]) Array.newInstance(clazz, (int) columns());
            for (long column = 0L; column < columns(); column++) {
                try {
                    accum[(int) column] = (R) valueAt(row, column).coerceTo(clazz);
                } catch (CoercionException ex) {
                    Logger.getLogger(BasicMatrix.class.getName()).log(Level.SEVERE,
                            "Coercion failed while upconverting matrix to " + clazz.getTypeName(), ex);
                    throw new ArithmeticException(String.format("While converting value %s to %s at %d, %d",
                            valueAt(row, column), clazz.getTypeName(), row, column));
                }
            }
            result.append(new RowVector<>(accum));
        }
        return result;
    }
}