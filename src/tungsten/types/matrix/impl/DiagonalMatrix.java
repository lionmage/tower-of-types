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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.impl.Zero;

/**
 * A compact representation of a diagonal matrix.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the numeric type of the elements of this matrix
 */
public class DiagonalMatrix<T extends Numeric> implements Matrix<T>  {
    private T[] elements;
    
    public DiagonalMatrix(T[] elements) {
        this.elements = Arrays.copyOf(elements, elements.length);
    }
    
    public DiagonalMatrix(Vector<T> source) {
        Class<T> clazz = (Class<T>) source.elementAt(0L).getClass();
        elements = (T[]) Array.newInstance(clazz, (int) source.length());
        for (int i = 0; i < source.length(); i++) {
            elements[i] = source.elementAt((long) i);
        }
    }

    @Override
    public long columns() {
        return (long) elements.length;
    }

    @Override
    public long rows() {
        return (long) elements.length;
    }

    @Override
    public T valueAt(long row, long column) {
        if (row == column) {
            return elements[(int) row];
        }
        Class<T> clazz = (Class<T>) elements[0].getClass();
        try {
            return (T) Zero.getInstance(elements[0].getMathContext()).coerceTo(clazz);
        } catch (CoercionException ex) {
            Logger.getLogger(DiagonalMatrix.class.getName()).log(Level.SEVERE,
                    "Coercion of Zero to " + clazz.getTypeName() + " failed.", ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public T determinant() {
        T accum = elements[0];
        Class<T> clazz = (Class<T>) accum.getClass();
        try {
            for (int idx = 1; idx < elements.length; idx++) {
                accum = (T) accum.multiply(elements[idx]).coerceTo(clazz);
            }
        } catch (CoercionException ce) {
            throw new IllegalStateException(ce);
        }
        return accum;
    }

    @Override
    public T trace() {
        T accum = elements[0];
        Class<T> clazz = (Class<T>) accum.getClass();
        try {
            for (int idx = 1; idx < elements.length; idx++) {
                accum = (T) accum.add(elements[idx]).coerceTo(clazz);
            }
        } catch (CoercionException ce) {
            throw new IllegalStateException(ce);
        }
        return accum;
    }

    @Override
    public Matrix<T> transpose() {
        return this; // diaginal matrices are their own transpose
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
        if (addend.rows() != this.rows() || addend.columns() != this.columns()) {
            throw new ArithmeticException("Addend must match dimensions of this diagonal matrix.");
        }
        
        BasicMatrix<T> result = new BasicMatrix(addend);
        Class<T> clazz = (Class<T>) elements[0].getClass();

        try {
            for (long idx = 0L; idx < this.rows(); idx++) {
                T sum = (T) addend.valueAt(idx, idx).add(elements[(int) idx]).coerceTo(clazz);
                result.setValueAt(sum, idx, idx);
            }
        } catch (CoercionException ce) {
            throw new IllegalStateException(ce);
        }
        return result;
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        BasicMatrix<T> result = new BasicMatrix<>();
        // scale the rows of the multiplier
        for (long idx = 0L; idx < this.rows(); idx++) {
            result.append(multiplier.getRow(idx).scale(elements[(int) idx]));
        }
        return result;
    }
}
