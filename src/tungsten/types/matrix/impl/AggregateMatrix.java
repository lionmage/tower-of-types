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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.vector.impl.ColumnVector;
import tungsten.types.vector.impl.RowVector;

/**
 * A tool for composing smaller matrices into larger matrices.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the {@link Numeric} subtype of this matrix
 */
public class AggregateMatrix<T extends Numeric> implements Matrix<T> {
    private Class<T> clazz;
    private final Lock cacheLock = new ReentrantLock();
    private T detCache;
    private long columns = -1, rows = -1;
    private Matrix<T>[][] subMatrices;
    
    public AggregateMatrix(Matrix<T>[][] subMatrices) {
        // first, check that each row has matrices with the same number of rows
        for (int row = 0; row < subMatrices.length; row++) {
            long val = subMatrices[row][0].rows();
            for (int column = 1; column < subMatrices[row].length; column++) {
                if (subMatrices[row][column].rows() != val) {
                    throw new IllegalArgumentException("All submatrices in row " + row + " must have the same number of rows.");
                }
            }
        }
        // then check that each column has matrices with the same number of columns
        for (int column = 0; column < subMatrices[0].length; column++) {
            long val = subMatrices[0][column].columns();
            for (int row = 1; row < subMatrices.length; row++) {
                if (subMatrices[row][column].columns() != val) {
                    throw new IllegalArgumentException("All submatrices in column " + column + " must have the same number of columns.");
                }
            }
        }
        // if we passed those tests, do the internal bookkeeping and return
        this.subMatrices = subMatrices;
        this.clazz = (Class<T>) subMatrices[0][0].valueAt(0L, 0L).getClass();
    }

    @Override
    public long columns() {
        cacheLock.lock();
        try {
            if (columns == -1L) {
                columns = Arrays.stream(subMatrices[0]).mapToLong(m -> m.columns()).sum();
            }
            return columns;
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public long rows() {
        cacheLock.lock();
        try {
            if (rows == -1L) {
                rows = 0L;
                for (int row = 0; row < subMatrices.length; row++) {
                    rows += subMatrices[row][0].rows();
                }
            }
            return rows;
        } finally {
            cacheLock.unlock();
        }
    }
    
    private int getTileRow(long rowIndex) {
        int row = 0;
        while (rowIndex >= subMatrices[row][0].rows()) {
            rowIndex -= subMatrices[row++][0].rows();
        }
        return row;
    }
    
    private int getTileColumn(long columnIndex) {
        int column = 0;
        while (columnIndex >= subMatrices[0][column].columns()) {
            columnIndex -= subMatrices[0][column++].columns();
        }
        return column;
    }
    
    private long getSubRowIndex(int tileRow, long rowIndex) {
        for (int row = 0; row < tileRow; row++) {
            rowIndex -= subMatrices[row][0].rows();
        }
        if (rowIndex >= subMatrices[tileRow][0].rows()) {
            throw new IllegalArgumentException("Row index " + rowIndex + " does not map to submatrix tile at row " + tileRow);
        }
        return rowIndex;
    }
    
    private long getSubColumnIndex(int tileColumn, long columnIndex) {
        columnIndex -= Arrays.stream(subMatrices[0]).limit(tileColumn).mapToLong(m -> m.columns()).sum();
        if (columnIndex >= subMatrices[0][tileColumn].columns()) {
            throw new IllegalArgumentException("Column index " + columnIndex + " does not map to submatrix tile at column " + tileColumn);
        }
        return columnIndex;
    }

    @Override
    public T valueAt(long row, long column) {
        int tileRow = getTileRow(row);
        int tileColumn = getTileColumn(column);
        return subMatrices[tileRow][tileColumn].valueAt(getSubRowIndex(tileRow, row), getSubColumnIndex(tileColumn, column));
    }

    @Override
    public T determinant() {
        cacheLock.lock();
        try {
            if (detCache == null) {
                detCache = new SubMatrix<>(this).determinant();
            }
            return detCache;
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Matrix<? extends Numeric> inverse() {
        if (subMatrices.length == 2 && subMatrices[0].length == 2) {
            // do blockwise inverse
            Matrix<Numeric> A = (Matrix<Numeric>) subMatrices[0][0];
            Matrix<Numeric> B = (Matrix<Numeric>) subMatrices[0][1];
            Matrix<Numeric> C = (Matrix<Numeric>) subMatrices[1][0];
            Matrix<Numeric> D = (Matrix<Numeric>) subMatrices[1][1];
            
            // A and D must be square to be invertible
            if (A.rows() == A.columns() && D.rows() == D.columns()) {
                final Numeric negone = IntegerType.class.isAssignableFrom(clazz) ? 
                        new IntegerImpl(BigInteger.valueOf(-1L)) : new RealImpl(BigDecimal.valueOf(-1L));
                Matrix<Numeric> Di = (Matrix<Numeric>) D.inverse();
                Matrix<Numeric> BDi = B.multiply(Di);
                Matrix<Numeric> BDiC = BDi.multiply(C);
                Matrix<Numeric> DiC  = Di.multiply(C);
                Matrix<Numeric> term = (Matrix<Numeric>) A.subtract(BDiC).inverse();
                Matrix<Numeric>[][] result = (Matrix<Numeric>[][]) Array.newInstance(Matrix.class, 2, 2);
                result[0][0] = term;
                result[0][1] = term.multiply(BDi).scale(negone);
                result[1][0] = DiC.multiply(term).scale(negone);
                result[1][1] = Di.add(DiC.multiply(term).multiply(BDi));
                return new AggregateMatrix(result);
            }
        }
        // otherwise, lean on SubMatrix to do it
        return new SubMatrix(this).inverse();
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
        if (this.rows() != addend.rows() || this.columns() != addend.columns()) {
            throw new ArithmeticException("Addend must match the dimensions of this matrix.");
        }
        // first check a sub-case where we're adding to another AggregateMatrix
        // and its sub-matrices have the same dimensions
        if (addend instanceof AggregateMatrix) {
            final AggregateMatrix<T> coercedAddend = (AggregateMatrix<T>) addend;
            if (checkSubmatrixDimensions(coercedAddend)) {
                Matrix<T>[][] result = (Matrix<T>[][]) Array.newInstance(clazz, subMatrices.length, subMatrices[0].length);
                for (int tileRow = 0; tileRow < subMatrices.length; tileRow++) {
                    for (int tileColumn = 0; tileColumn < subMatrices[tileRow].length; tileColumn++) {
                        result[tileRow][tileColumn] = subMatrices[tileRow][tileColumn].add(coercedAddend.subMatrices[tileRow][tileColumn]);
                    }
                }
                return new AggregateMatrix<>(result);
            }
        }
        // otherwise, let SubMatrix do the heavy lifting
        return new SubMatrix(this).add(addend);
    }
    
    private boolean checkSubmatrixDimensions(AggregateMatrix<T> other) {
        if (subMatrices.length != other.subMatrices.length ||
                subMatrices[0].length != other.subMatrices[0].length) return false;
        for (int tileRow = 0; tileRow < subMatrices.length; tileRow++) {
            for (int tileColumn = 0; tileColumn < subMatrices[tileRow].length; tileColumn++) {
                if (subMatrices[tileRow][tileColumn].rows() != other.subMatrices[tileRow][tileColumn].rows() ||
                        subMatrices[tileRow][tileColumn].columns() != other.subMatrices[tileRow][tileColumn].columns()) {
                    // bail out quickly here since we are not equal
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        if (this.columns() != multiplier.rows()) {
            throw new ArithmeticException("Multiplier must have the same number of rows as this matrix has columns.");
        }
        return new SubMatrix<>(this).multiply(multiplier);
    }

    @Override
    public RowVector<T> getRow(long row) {
        final int tileRow = getTileRow(row);
        final long rowIndex = getSubRowIndex(tileRow, row);
        RowVector<T> result = new RowVector<>();
        Arrays.stream(subMatrices[tileRow]).map(tr -> tr.getRow(rowIndex).stream()).flatMap(e -> e).forEachOrdered(result::append);
        return result;
    }

    @Override
    public ColumnVector<T> getColumn(long column) {
        final int tileColumn = getTileColumn(column);
        final long columnIndex = getSubColumnIndex(tileColumn, column);
        ColumnVector<T> result = new ColumnVector<>();
        for (int rowidx = 0; rowidx < subMatrices.length; rowidx++) {
            subMatrices[rowidx][tileColumn].getColumn(columnIndex).stream().forEachOrdered(result::append);
        }
        return result;
    }

    @Override
    public Matrix<T> scale(T scaleFactor) {
        Matrix<T>[][] result = (Matrix<T>[][]) Array.newInstance(Matrix.class, subMatrices.length, subMatrices[0].length);
        for (int row = 0; row < subMatrices.length; row++) {
            for (int column = 0; column < subMatrices[0].length; column++) {
                result[row][column] = subMatrices[row][column].scale(scaleFactor);
            }
        }
        return new AggregateMatrix(result);
    }
}
