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
package tungsten.types.vector.impl;

import java.lang.reflect.Array;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.Zero;

/**
 * A column vector, which is also a Nx1 (single-column) matrix.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the {@link Numeric} type for this column vector
 */
public class ColumnVector<T extends Numeric> implements Vector<T>, Matrix<T> {
    private T[] elements;
    private MathContext mctx;
    
    public ColumnVector(T... elements) {
        this.elements = elements;
    }
    
    public ColumnVector(List<T> elementList) {
        this.elements = (T[]) Array.newInstance(elementList.get(0).getClass(), elementList.size());
        elementList.toArray(elements);
    }
    
    public void setMathContext(MathContext mctx) {
        this.mctx = mctx;
    }

    @Override
    public long length() {
        return (long) elements.length;
    }

    @Override
    public T elementAt(long position) {
        return elements[(int) position];
    }

    @Override
    public void setElementAt(T element, long position) {
        elements[(int) position] = element;
    }

    @Override
    public void append(T element) {
        throw new UnsupportedOperationException("ColumnVector is fixed-length.");
    }

    @Override
    public Vector<T> add(Vector<T> addend) {
        if (addend.length() != this.length()) throw new ArithmeticException("Cannot add vectors of different lengths.");
        // TODO disallow adding column and row vectors
        
        final Class<? extends Numeric> clazz = elements[0].getClass();
        T[] sumArray = (T[]) Array.newInstance(clazz, elements.length);
        try {
            for (int i = 0; i < elements.length; i++) {
                sumArray[i] = (T) elements[i].add(addend.elementAt((long) i)).coerceTo(clazz);
            }
            return new ColumnVector<>(sumArray);
        } catch (CoercionException ce) {
            Logger.getLogger(ColumnVector.class.getName()).log(Level.SEVERE, "Unable to compute sum of column vectors.", ce);
            throw new ArithmeticException("Cannot add column vectors.");
        }
    }

    @Override
    public Vector<T> subtract(Vector<T> subtrahend) {
        return this.add(subtrahend.negate());
    }

    @Override
    public Vector<T> negate() {
        T[] negArray = (T[]) Array.newInstance(elements[0].getClass(), elements.length);
        Arrays.stream(elements).map(x -> x.negate()).toArray(size -> negArray);
        return new ColumnVector<>(negArray);
    }

    @Override
    public Vector<T> scale(T factor) {
        final Class<? extends Numeric> clazz = elements[0].getClass();
        T[] scaledArray = (T[]) Array.newInstance(clazz, elements.length);
        try {
            for (int i = 0; i < elements.length; i++) {
                scaledArray[i] = (T) elements[i].multiply(factor).coerceTo(clazz);
            }
            return new ColumnVector<>(scaledArray);
        } catch (CoercionException ce) {
            Logger.getLogger(ColumnVector.class.getName()).log(Level.SEVERE, "Unable to compute scaled column vector.", ce);
            throw new ArithmeticException("Cannot scale column vector.");
        }
    }

    @Override
    public T magnitude() {
        Class<? extends Numeric> clazz = elements[0].getClass();
        try {
            T zero = (T) Zero.getInstance(mctx).coerceTo(clazz);
            return (T) Arrays.stream(elements).reduce(zero, (x, y) -> (T) x.add(y.multiply(y))).sqrt().coerceTo(clazz);
        } catch (CoercionException ex) {
            Logger.getLogger(ColumnVector.class.getName()).log(Level.SEVERE, "Unable to compute magnitude of column vector.", ex);
            throw new ArithmeticException("Cannot compute magnitude of column vector");
        }
    }

    @Override
    public T dotProduct(Vector<T> other) {
        if (other.length() != this.length()) throw new ArithmeticException("Cannot compute dot product for vectors of different length.");
        final Class<? extends Numeric> clazz = elements[0].getClass();
        try {
            T accum = (T) Zero.getInstance(mctx).coerceTo(clazz);
            for (long i = 0L; i < this.length(); i++) {
                accum = (T) accum.add(this.elementAt(i).multiply(other.elementAt(i))).coerceTo(clazz);
            }
            return accum;
        } catch (CoercionException ex) {
            Logger.getLogger(ColumnVector.class.getName()).log(Level.SEVERE, "Error computing dot product.", ex);
            throw new ArithmeticException("Error computing dot product");
        }
    }

    @Override
    public Vector<T> crossProduct(Vector<T> other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Vector<T> normalize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RealType computeAngle(Vector<T> other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }

    @Override
    public long columns() {
        return 1L;
    }

    @Override
    public long rows() {
        return (long) elements.length;
    }

    @Override
    public T valueAt(long row, long column) {
        if (column != 0L) throw new IndexOutOfBoundsException("Column vector does not have a column " + column);
        return elements[(int) row];
    }

    @Override
    public T determinant() {
        throw new ArithmeticException("Cannot compute determinant of a matrix with unequal columns and rows.");
    }
    
    @Override
    public Matrix<T> transpose() {
        return new RowVector(elements);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Vector) {
            Vector<T> that = (Vector<T>) o;
            if (this.length() != that.length()) return false;
            for (long i = 0L; i < this.length(); i++) {
                if (!this.elementAt(i).equals(that.elementAt(i))) return false;
            }
            return true;
        } else if (o instanceof Matrix) {
            Matrix<T> that = (Matrix<T>) o;
            if (that.columns() != this.columns() || that.rows() != this.rows()) return false;
            for (long i = 0L; i < this.rows(); i++) {
                if (!this.valueAt(i, 0L).equals(that.valueAt(i, 0L))) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Arrays.deepHashCode(this.elements);
        hash = 53 * hash + Objects.hashCode(this.mctx);
        return hash;
    }
    
    @Override
    public String toString() {
        return Arrays.stream(elements).map(x -> x.toString()).collect(Collectors.joining(", ", "[ ", " ]"));
    }
}
