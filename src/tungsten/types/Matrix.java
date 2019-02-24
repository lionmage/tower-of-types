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

import java.lang.reflect.Array;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.vector.impl.ColumnVector;
import tungsten.types.vector.impl.RowVector;

/**
 * The root type for matrices.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the {@link Numeric} type of the elements of this matrix 
 */
public interface Matrix<T extends Numeric> {
    long columns();
    long rows();
    T valueAt(long row, long column);
    T determinant();
    Matrix<? extends Numeric> inverse();
    
    default boolean isUpperTriangular() {
        if (columns() != rows()) return false;
        if (rows() == 1L) return false;  // singleton matrix can't really be either upper or lower triangular
        final MathContext mctx = valueAt(0L, 0L).getMathContext();
        final Numeric zero = Zero.getInstance(mctx);
        
        for (long row = 1L; row < rows(); row++) {
            for (long column = 0L; column < columns() - row; column++) {
                if (!valueAt(row, column).equals(zero)) return false;
            }
        }
        return true;
    }
    
    default boolean isLowerTriangular() {
        if (columns() != rows()) return false;
        if (rows() == 1L) return false;  // singleton matrix can't really be either upper or lower triangular
        final MathContext mctx = valueAt(0L, 0L).getMathContext();
        final Numeric zero = Zero.getInstance(mctx);
        
        for (long row = 0L; row < rows() - 1L; row++) {
            for (long column = row + 1L; column < columns(); column++) {
                if (!valueAt(row, column).equals(zero)) return false;
            }
        }
        return true;
    }
    
    default boolean isTriangular() {
        Predicate<Matrix<T>> isLower = (Matrix<T> t) -> t.isLowerTriangular();
        Predicate<Matrix<T>> isUpper = (Matrix<T> t) -> t.isUpperTriangular();
        return Arrays.asList(isLower, isUpper).parallelStream()
                .<Boolean> map(p -> p.test(this))
                .anyMatch(x -> x.booleanValue());
        // the naive version below might be easier to read and understand,
        // but for big matrices, you really want to do these checks in parallel
        // if you possibly can
//        return isLowerTriangular() || isUpperTriangular();
    }
    
    default T trace() {
        if (this.columns() != this.rows()) {
            throw new ArithmeticException("Trace is only defined for square matrices.");
        }
        Numeric accum = valueAt(0L, 0L);
        Class<T> clazz = (Class<T>) accum.getClass();
        for (long index = 1L; index < this.columns(); index++) {
            accum = accum.add(valueAt(index, index));
        }
        try {
            return (T) accum.coerceTo(clazz);
        } catch (CoercionException ex) {
            Logger.getLogger(Matrix.class.getName()).log(Level.SEVERE,
                    "Could not coerce " + accum + " to " + clazz.getTypeName(), ex);
            throw new ArithmeticException("Type coercion failed.");
        }
    }
    
    default Matrix<T> transpose() {
        final long rows = this.columns();
        final long columns = this.rows();
        final Matrix<T> source = this;
        
        return new Matrix<T>() {
            @Override
            public long columns() { return columns; }

            @Override
            public long rows() { return rows; }

            @Override
            public T valueAt(long row, long column) {
                if (row < 0L || row >= rows || column < 0L || column >= columns) {
                    throw new IndexOutOfBoundsException("row:" + row + ", column:" + column +
                            " is out of bounds for a " + rows + " by " + columns + " matrix.");
                }
                return source.valueAt(column, row);
            }

            @Override
            public T determinant() {
                // the determinant of the transpose is the same as the determinant
                // of the original matrix
                return source.determinant();
            }

            @Override
            public Matrix<T> add(Matrix<T> addend) {
                // delegate to the add method of the addend if possible
                // since matrix addition is commutative
                if (!addend.getClass().isAnonymousClass()) {
                    return addend.add(this);
                }
                // (A + B)^T = A^T + B^T
                return source.add(addend.transpose()).transpose();
            }

            @Override
            public Matrix<T> multiply(Matrix<T> multiplier) {
                // (A*B)^T = B^T * A^T
                return multiplier.transpose().multiply(source).transpose();
            }
            
            @Override
            public Matrix<T> transpose() {
                return source;
            }

            @Override
            public Matrix<? extends Numeric> inverse() {
                // the inverse of the transpose is the transpose of the inverse of the original matrix
                return source.inverse().transpose();
            }
        };
    }
    
    Matrix<T> add(Matrix<T> addend);
    Matrix<T> multiply(Matrix<T> multiplier);
    
    default RowVector<T> getRow(long row) {
        Class<T> clazz = (Class<T>) valueAt(0L, 0L).getClass();
        T[] temp = (T[]) Array.newInstance(clazz, (int) columns());
        for (int i = 0; i < columns(); i++) {
            temp[i] = valueAt(row, i);
        }
        return new RowVector<>(temp);
    }
    
    default ColumnVector<T> getColumn(long column) {
        Class<T> clazz = (Class<T>) valueAt(0L, 0L).getClass();
        T[] temp = (T[]) Array.newInstance(clazz, (int) rows());
        for (int j = 0; j < rows(); j++) {
            temp[j] = valueAt(j, column);
        }
        return new ColumnVector<>(temp);
    }
}
