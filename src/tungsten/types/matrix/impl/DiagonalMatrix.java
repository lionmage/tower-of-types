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
import java.util.stream.Collectors;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.Euler;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.util.MathUtils;

/**
 * A compact representation of a diagonal matrix.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the numeric type of the elements of this matrix
 */
public class DiagonalMatrix<T extends Numeric> implements Matrix<T>  {
    final private T[] elements;
    
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

    @Override
    public DiagonalMatrix<? extends Numeric> inverse() {
        final Numeric zero = Zero.getInstance(elements[0].getMathContext());
        if (Arrays.stream(elements).anyMatch(element -> element.equals(zero))) {
            throw new ArithmeticException("Diagonal matrices with any 0 elements on the diagonal have no inverse.");
        };
        Numeric[] result = Arrays.stream(elements).map(element -> element.inverse()).toArray(size -> new Numeric[size]);
        return new DiagonalMatrix(result);
    }
    
    public Matrix<? extends Numeric> pow(Numeric n) {
        Numeric[] result;
        if (elements[0] instanceof RealType) {
            result = Arrays.stream(elements)
                    .map(element -> MathUtils.generalizedExponent((RealType) element, n, element.getMathContext()))
                    .toArray(size -> new Numeric[size]);
        } else {
            if (!(n instanceof IntegerType)) {
                throw new IllegalArgumentException("Currently, non-integer exponents are not supported for non-real types.");
            }
            result = Arrays.stream(elements)
                    .map(element -> MathUtils.computeIntegerExponent(element, (IntegerType) n))
                    .toArray(size -> new Numeric[size]);
        }
        return new DiagonalMatrix<>(result);
    }
    
    public Matrix<? extends Numeric> exp() {
        final Euler e = Euler.getInstance(elements[0].getMathContext());

        Numeric[] result = Arrays.stream(elements)
                .map(element -> {
                    return element instanceof ComplexType ? e.exp((ComplexType) element) : e.exp(limitedUpconvert(element));
                }).toArray(size -> new Numeric[size]);
        return new DiagonalMatrix<>(result);
    }
    
    // this method strictly exists to promote lesser types in the hierarchy to real values
    private RealType limitedUpconvert(Numeric val) {
        if (val instanceof RealType)  return (RealType) val;
        try {
            return (RealType) val.coerceTo(RealType.class);
        } catch (CoercionException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    @Override
    public String toString() {
        // 202F = non-breaking narrow space
        return Arrays.stream(elements).map(element -> element.toString())
                .collect(Collectors.joining(", ", "diag(\u202F", "\u202F)"));
    }
}
