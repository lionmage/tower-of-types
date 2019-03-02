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
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
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
 * A {@link Matrix} implementation that stores its internal values in
 * a columnar format.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the {@link Numeric} type for the elements of this matrix
 */
public class ColumnarMatrix<T extends Numeric> implements Matrix<T> {
    private final List<ColumnVector<T>> columns = new ArrayList<>();
    
    public ColumnarMatrix() {
    }
    
    public ColumnarMatrix(Matrix<T> source) {
        for (long column = 0L; column < source.columns(); column++) {
            columns.add(source.getColumn(column));
        }
    }
    
    public ColumnarMatrix(T[][] source) {
        for (int column = 0; column < source[0].length; column++) {
            append(extractColumn(source, column));
        }
    }
    
    public ColumnarMatrix(List<ColumnVector<T>> source) {
        source.forEach(this::append);
    }
    
    private ColumnVector<T> extractColumn(T[][] source, int column) {
        final int rows = source[0].length;
        T[] temp = (T[]) Array.newInstance(source[0][0].getClass(), rows);
        for (int i = 0; i < rows; i++) temp[i] = source[i][column];
        return new ColumnVector<>(temp);
    }

    @Override
    public long columns() {
        return (long) columns.size();
    }

    @Override
    public long rows() {
        if (columns.isEmpty()) return 0L;
        return columns.get(0).length();  // length of the first colidx vector
    }

    @Override
    public T valueAt(long row, long column) {
        return columns.get((int) column).elementAt(row);
    }

    @Override
    public T determinant() {
        if (rows() != columns()) {
            throw new ArithmeticException("Can only compute determinant for a square matrix.");
        }
        if (columns() == 1L) { // 1x1 matrix
            return valueAt(0L, 0L);
        }
        if (columns() == 2L) {
            T a = valueAt(0L, 0L);
            T b = valueAt(0L, 1L);
            T c = valueAt(1L, 0L);
            T d = valueAt(1L, 1L);
            return (T) a.multiply(d).subtract(c.multiply(b));  // should not require coercion here
        }
        
        final Class<T> clazz = (Class<T>) valueAt(0L, 0L).getClass();
        try {
            // do not mess with the short circuit evaluation!
            if (columns() > 4L && isTriangular()) {
                Numeric accum = valueAt(0L, 0L);
                for (long index = 1L; index < rows(); index++) {
                    accum = accum.multiply(valueAt(index, index));
                }
                return (T) accum.coerceTo(clazz);
            }
            else {
                // A column-friendly version of the recursive algorithm.
                ColumnVector<T> firstColumn = columns.get(0);
                ColumnarMatrix<T> intermediate = this.removeColumn(0L);
                Numeric accum = Zero.getInstance(valueAt(0L, 0L).getMathContext());
                for (long row = 0L; row < rows(); row++) {
                    Numeric coeff = firstColumn.elementAt(row);
                    if (row % 2L == 1L) coeff = coeff.negate(); // alternate sign of the coefficient
                    ColumnarMatrix<T> subMatrix = intermediate.removeRow(row);
                    accum = accum.add(coeff.multiply(subMatrix.determinant()));
                }
                return (T) accum.coerceTo(clazz);
            }
        } catch (CoercionException ex) {
            Logger.getLogger(ColumnarMatrix.class.getName()).log(Level.SEVERE, "Coercion failed computing determinant.", ex);
            throw new ArithmeticException("Coercion failed: " + ex.getMessage());
        }
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
        if (columns() == 1L) {
            return new SingletonMatrix(valueAt(0L, 0L).inverse());
        } else if (columns() == 2L) {
            final Numeric scale = det.inverse();
            T a = valueAt(0L, 0L);
            T b = valueAt(0L, 1L);
            T c = valueAt(1L, 0L);
            T d = valueAt(1L, 1L);
            ColumnarMatrix<Numeric> result = new ColumnarMatrix<>();
            result.append(new ColumnVector(d, c.negate()).scale(scale));
            result.append(new ColumnVector(b.negate(), a).scale(scale));
            return result;
        }

        // otherwise recursively compute this using the adjoint
        final Matrix<T> adjoint = this.adjoint();
        ColumnarMatrix<Numeric> byAdjoint = new ColumnarMatrix<>();
        final Numeric factor = det.inverse();
        for (long column = 0L; column < adjoint.columns(); column++) {
            byAdjoint.append(((ColumnVector<Numeric>) adjoint.getColumn(column)).scale(factor));
        }
        return byAdjoint;
    }

    @Override
    public Matrix<T> transpose() {
        BasicMatrix<T> result = new BasicMatrix<>();
        
        for (ColumnVector<T> column : columns) {
            result.append(column.transpose());
        }
        
        return result;
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
        if (addend.columns() != columns() || addend.rows() != rows()) {
            throw new ArithmeticException("Cannot add matrices of different dimensions.");
        }
        
        ColumnarMatrix<T> result = new ColumnarMatrix<>();
        for (long column = 0L; column < addend.columns(); column++) {
            result.append(addend.getColumn(column).add((Vector<T>) getColumn(column)));
        }
        return result;
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        if (this.columns() != multiplier.rows()) {
            throw new ArithmeticException("Multiplier must have the same number of rows as this matrix has columns.");
        }
        T[][] temp = (T[][]) Array.newInstance(valueAt(0L, 0L).getClass(), (int) this.rows(), (int) multiplier.columns());
        for (long row = 0L; row < rows(); row++) {
            RowVector<T> rowvec = this.getRow(row);
            for (long column = 0L; column < multiplier.columns(); column++) {
                temp[(int) row][(int) column] = rowvec.dotProduct(multiplier.getColumn(column));
            }
        }
        return new ColumnarMatrix<>(temp);
    }

    @Override
    public ColumnVector<T> getColumn(long column) {
        return columns.get((int) column);
    }
    
    public final void append(ColumnVector<T> column) {
        if (columns.isEmpty() || column.length() == this.rows()) {
            columns.add(column);
        } else {
            throw new IllegalArgumentException("Column vector must have " + this.rows() + " elements.");
        }
    }
    
    public ColumnarMatrix<T> removeColumn(long column) {
        ColumnarMatrix<T> result = new ColumnarMatrix<>();
        for (long colidx = 0L; colidx < columns(); colidx++) {
            if (colidx == column) continue;
            result.append(getColumn(colidx));
        }
        return result;
    }
    
    public ColumnarMatrix<T> removeRow(long row) {
        ColumnarMatrix<T> result = new ColumnarMatrix<>();
        for (long colidx = 0L; colidx < columns(); colidx++) {
            result.append(removeElementAt(getColumn(colidx), row));
        }
        return result;
    }
    
    private ColumnVector<T> removeElementAt(ColumnVector<T> source, long index) {
        List<T> elementsToKeep = new ArrayList<>((int) source.length() - 1);
        for (long rowidx = 0L; rowidx < source.length(); rowidx++) {
            if (rowidx == index) continue;
            elementsToKeep.add(source.elementAt(rowidx));
        }
        return new ColumnVector<>(elementsToKeep);
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
        ColumnarMatrix<R> result = new ColumnarMatrix<>();
        for (long column = 0L; column < columns(); column++) {
            final ColumnVector<T> originalColumn = getColumn(column);
            R[] accum = (R[]) Array.newInstance(clazz, (int) originalColumn.length());
            for (long row = 0L; row < originalColumn.length(); row++) {
                try {
                    accum[(int) row] = (R) originalColumn.elementAt(row).coerceTo(clazz);
                } catch (CoercionException ex) {
                    Logger.getLogger(BasicMatrix.class.getName()).log(Level.SEVERE,
                            "Coercion failed while upconverting matrix to " + clazz.getTypeName(), ex);
                    throw new ArithmeticException(String.format("While converting value %s to %s at %d, %d",
                            valueAt(row, column), clazz.getTypeName(), row, column));
                }
            }
            result.append(new ColumnVector<>(accum));
        }
        return result;
    }

    /**
     * Return a matrix with row {@code row} and column {@code column}
     * removed.
     * 
     * @param row the row index
     * @param column the column index
     * @return the sub-matrix formed by row and column removal
     */
    public ColumnarMatrix<T> minor(long row, long column) {
        return this.removeColumn(column).removeRow(row);
    }
    
    public ColumnarMatrix<T> cofactor() {
        T[][] result = (T[][]) Array.newInstance(columns.get(0).elementAt(0L).getClass(), (int) this.rows(), columns.size());
        for (long row = 0L; row < rows(); row++) {
            for (long column = 0L; column < columns(); column++) {
                T intermediate = minor(row, column).determinant();
                if ((row + column) % 2L == 1L) intermediate = (T) intermediate.negate();
                result[(int) row][(int) column] = intermediate;
            }
        }
        return new ColumnarMatrix<>(result);
    }
    
    public Matrix<T> adjoint() {
        return cofactor().transpose();
    }
    
    public Matrix<T> exchangeColumns(long column1, long column2) {
        if (column1 < 0L || column1 >= columns()) throw new IndexOutOfBoundsException("column1 must be within bounds 0 - " + (columns() - 1L));
        if (column2 < 0L || column2 >= columns()) throw new IndexOutOfBoundsException("column2 must be within bounds 0 - " + (columns() - 1L));
        if (column1 == column2) return this; // NO-OP
        
        ArrayList<ColumnVector<T>> columns2 = new ArrayList<>(this.columns);
        Collections.swap(columns2, (int) column1, (int) column2);
        return new ColumnarMatrix<>(columns2);
    }
    
    public Matrix<T> exchangeRows(long row1, long row2) {
        if (row1 < 0L || row1 >= rows()) throw new IndexOutOfBoundsException("row1 must be within bounds 0 - " + (rows() - 1L));
        if (row2 < 0L || row2 >= rows()) throw new IndexOutOfBoundsException("row2 must be within bounds 0 - " + (rows() - 1L));
        if (row1 == row2) return this; // NO-OP
        
        final ArrayList<ColumnVector<T>> result = new ArrayList<>();
        this.columns.forEach(colVec -> {
            ArrayList<T> column = new ArrayList<>((int) colVec.length());
            for (long row = 0L; row < rows(); row++) {
                if (row == row1) column.add(colVec.elementAt(row2));
                else if (row == row2) column.add(colVec.elementAt(row1));
                else column.add(colVec.elementAt(row));
            }
            result.add(new ColumnVector<>(column));
        });
        
        return new ColumnarMatrix<>(result);
    }
    
    @Override
    public boolean isUpperTriangular() {
        final MathContext mctx = valueAt(0L, 0L).getMathContext();
        final Numeric zero = Zero.getInstance(mctx);
        long skipRows = 1L;
        boolean hasNonZero = false;
        for (ColumnVector column : columns.subList(0, columns.size() - 1)) {
            hasNonZero = column.stream().skip(skipRows++).anyMatch(x -> !x.equals(zero));
            if (hasNonZero) break;
        }
        return !hasNonZero;
    }
    
    @Override
    public boolean isLowerTriangular() {
        final MathContext mctx = valueAt(0L, 0L).getMathContext();
        final Numeric zero = Zero.getInstance(mctx);
        long endRow = 1L;
        boolean hasNonZero = false;
        for (ColumnVector column : columns.subList(1, columns.size())) {
            hasNonZero = column.stream().limit(endRow++).anyMatch(x -> !x.equals(zero));
            if (hasNonZero) break;
        }
        return !hasNonZero;
    }
}
