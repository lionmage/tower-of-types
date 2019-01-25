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
import tungsten.types.matrix.impl.BasicMatrix;
import tungsten.types.matrix.impl.SingletonMatrix;
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.util.OptionalOperations;

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
        final int nulength = elements.length + 1;
        elements = Arrays.copyOf(elements, nulength); // lengthen the array by 1
        elements[nulength - 1] = element;
    }

    @Override
    public ColumnVector<T> add(Vector<T> addend) {
        if (addend.length() != this.length()) throw new ArithmeticException("Cannot add vectors of different lengths.");
        if (addend instanceof RowVector) throw new ArithmeticException("Cannot add a column vector to a row vector.");
        
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
    public ColumnVector<T> subtract(Vector<T> subtrahend) {
        return this.add(subtrahend.negate());
    }

    @Override
    public ColumnVector<T> negate() {
        T[] negArray = (T[]) Array.newInstance(elements[0].getClass(), elements.length);
        Arrays.stream(elements).map(x -> x.negate()).toArray(size -> negArray);
        return new ColumnVector<>(negArray);
    }

    @Override
    public ColumnVector<T> scale(T factor) {
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
            throw new ArithmeticException("Cannot compute magnitude of column vector.");
        }
    }

    @Override
    public T dotProduct(Vector<T> other) {
        if (other.length() != this.length()) throw new ArithmeticException("Cannot compute dot product for vectors of different length.");
        final Class<? extends Numeric> clazz = elements[0].getClass();
        try {
            Numeric accum = Zero.getInstance(mctx);
            for (long i = 0L; i < this.length(); i++) {
                accum = accum.add(this.elementAt(i).multiply(other.elementAt(i)));
            }
            return (T) accum.coerceTo(clazz);
        } catch (CoercionException ex) {
            Logger.getLogger(ColumnVector.class.getName()).log(Level.SEVERE, "Error computing dot product.", ex);
            throw new ArithmeticException("Error computing dot product.");
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
    public T trace() {
        throw new ArithmeticException("Cannot compute trace of a matrix with unequal columns and rows.");
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
        // 202F = Narrow No-Break Space; small superscript T indicates this is a column vector,
        // i.e. the transpose of a row vector.
        return Arrays.stream(elements).map(x -> x.toString()).collect(Collectors.joining(", ", "[\u202F", "\u202F]ᵀ"));
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
        if (addend.rows() != rows() || addend.columns() != columns()) {
            throw new ArithmeticException("Dimension mismatch for single-column matrix.");
        }
        Class<T> clazz = (Class<T>) elements[0].getClass();
        T[] result = (T[]) Array.newInstance(clazz, elements.length);
        for (long index = 0; index < elements.length; index++) {
            try {
                result[(int) index] = (T) elements[(int) index].add(addend.valueAt(index, 0L)).coerceTo(clazz);
            } catch (CoercionException ex) {
                throw new ArithmeticException("Unable to coerce matrix element to type " +
                        clazz.getTypeName() + " during matrix addition.");
            }
        }
        return new ColumnVector(result);
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        if (this.columns() != multiplier.rows()) {
            throw new ArithmeticException("Multiplier must have a single row.");
        }
        
        Class<T> clazz = (Class<T>) elements[0].getClass();
        T[][] temp = (T[][]) Array.newInstance(clazz, (int) this.rows(), (int) multiplier.columns());

        try {
            for (int row = 0; row < rows(); row++) {
                for (int column = 0; column < multiplier.columns(); column++) {
                    temp[row][column] = (T) elements[row].multiply(multiplier.valueAt(0L, column)).coerceTo(clazz);
                }
            }
        } catch (CoercionException ce) {
            throw new ArithmeticException("Type coercion failed during matrix multiply: " + ce.getMessage());
        }
        return new BasicMatrix<>(temp);
    }
    
    @Override
    public ColumnVector<T> getColumn(long column) {
        if (column != 0L) throw new IndexOutOfBoundsException("Index does not match the single column of this matrix.");
        return this;
    }

    @Override
    public Matrix<? extends Numeric> inverse() {
        if (length() == 1L) {
            return new SingletonMatrix(elements[0].inverse());
        }
        throw new ArithmeticException("Inverse only applies to square matrices.");
    }
}
