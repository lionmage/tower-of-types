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
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.util.OptionalOperations;

/**
 * Representation of a row vector.  This can also be
 * treated as a matrix with N columns and a single row.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the {@link Numeric} type of this row vector
 */
public class RowVector<T extends Numeric> implements Vector<T>, Matrix<T> {
    private T[] elements;
    private MathContext mctx;
    
    public RowVector(T... elements) {
        this.elements = elements;
    }
    
    public RowVector(List<T> elementList) {
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
        throw new UnsupportedOperationException("RowVector is fixed-length.");
    }

    @Override
    public Vector<T> add(Vector<T> addend) {
        if (addend.length() != this.length()) throw new ArithmeticException("Cannot add vectors of different lengths.");
        if (addend instanceof ColumnVector) throw new ArithmeticException("Cannot add a row vector to a column vector.");
        
        final Class<? extends Numeric> clazz = elements[0].getClass();
        T[] sumArray = (T[]) Array.newInstance(clazz, elements.length);
        try {
            for (int i = 0; i < elements.length; i++) {
                sumArray[i] = (T) elements[i].add(addend.elementAt((long) i)).coerceTo(clazz);
            }
            return new RowVector<>(sumArray);
        } catch (CoercionException ce) {
            Logger.getLogger(RowVector.class.getName()).log(Level.SEVERE, "Unable to compute sum of row vectors.", ce);
            throw new ArithmeticException("Cannot add row vectors.");
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
        return new RowVector<>(negArray);
    }

    @Override
    public Vector<T> scale(T factor) {
        final Class<? extends Numeric> clazz = elements[0].getClass();
        T[] scaledArray = (T[]) Array.newInstance(clazz, elements.length);
        try {
            for (int i = 0; i < elements.length; i++) {
                scaledArray[i] = (T) elements[i].multiply(factor).coerceTo(clazz);
            }
            return new RowVector<>(scaledArray);
        } catch (CoercionException ce) {
            Logger.getLogger(RowVector.class.getName()).log(Level.SEVERE, "Unable to compute scaled row vector.", ce);
            throw new ArithmeticException("Cannot scale row vector.");
        }
    }

    @Override
    public T magnitude() {
        Class<? extends Numeric> clazz = elements[0].getClass();
        try {
            T zero = (T) Zero.getInstance(mctx).coerceTo(clazz);
            return (T) Arrays.stream(elements).reduce(zero, (x, y) -> (T) x.add(y.multiply(y))).sqrt().coerceTo(clazz);
        } catch (CoercionException ex) {
            Logger.getLogger(RowVector.class.getName()).log(Level.SEVERE, "Unable to compute magnitude of row vector.", ex);
            throw new ArithmeticException("Cannot compute magnitude of row vector");
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
            Logger.getLogger(RowVector.class.getName()).log(Level.SEVERE, "Error computing dot product.", ex);
            throw new ArithmeticException("Error computing dot product");
        }
    }

    @Override
    public Vector<T> crossProduct(Vector<T> other) {
        if (other.length() != this.length()) throw new ArithmeticException("Cannot compute cross product for vectors of different dimension.");
        final Class<? extends Numeric> clazz = other.elementAt(0L).getClass();
        final Class<? extends Numeric> myclass = elements[0].getClass();
        if (OptionalOperations.findCommonType(clazz, myclass) == Numeric.class) {
            throw new UnsupportedOperationException("No types in common between " +
                    clazz.getTypeName() + " and " + myclass.getTypeName());
        }
        if (RealType.class.isAssignableFrom(clazz)) {
            RealVector realvec = new RealVector((RealType[]) elements, mctx);
            return (Vector<T>) realvec.crossProduct((Vector<RealType>) other);
        } else if (ComplexType.class.isAssignableFrom(clazz)) {
            ComplexVector cplxvec = new ComplexVector((ComplexType[]) elements, mctx);
            return (Vector<T>) cplxvec.crossProduct((Vector<ComplexType>) other);
        }
        Logger.getLogger(RowVector.class.getName()).log(Level.WARNING, "No way to compute cross product for {}", clazz.getTypeName());
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vector<T> normalize() {
        final Class<? extends Numeric> clazz = elements[0].getClass();
        try {
            return this.scale((T) this.magnitude().inverse().coerceTo(clazz));
        } catch (CoercionException ex) {
            Logger.getLogger(RowVector.class.getName()).log(Level.SEVERE,
                    "Unable to normalize vector for type " + clazz.getTypeName(), ex);
            throw new ArithmeticException("Error computing vector normal.");
        }
    }

    @Override
    public RealType computeAngle(Vector<T> other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MathContext getMathContext() {
        return mctx;
    }

    @Override
    public long columns() {
        return (long) elements.length;
    }

    @Override
    public long rows() {
        return 1L;
    }

    @Override
    public T valueAt(long row, long column) {
        if (row != 0L) throw new IndexOutOfBoundsException("Row vector does not have a row " + row);
        return elements[(int) column];
    }

    @Override
    public T determinant() {
        throw new ArithmeticException("Cannot compute determinant of a matrix with unequal columns and rows.");
    }
    
    @Override
    public T trace() {
        throw new ArithmeticException("Cannot compute trace of a matrix with unequal columns and rows.");
    }
    
    @Override
    public Matrix<T> transpose() {
        return new ColumnVector(elements);
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
                if (!this.valueAt(0L, i).equals(that.valueAt(0L, i))) return false;
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
