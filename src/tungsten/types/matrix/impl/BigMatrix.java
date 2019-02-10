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
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.util.collections.BigList;
import tungsten.types.vector.impl.ColumnVector;
import tungsten.types.vector.impl.RowVector;

/**
 * A Matrix implementation backed by a file, such as a CSV file.
 * As such, this matrix is not limited by the maximum size of Java arrays.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the {@link Numeric} subtype of this matrix; currently only IntegerType and RealType are supported
 */
public class BigMatrix<T extends Numeric> implements Matrix<T> {

    @Override
    public long columns() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long rows() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public T valueAt(long row, long column) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public T determinant() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix<? extends Numeric> inverse() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RowVector<T> getRow(long row) {
        return Matrix.super.getRow(row); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ColumnVector<T> getColumn(long column) {
        return Matrix.super.getColumn(column); //To change body of generated methods, choose Tools | Templates.
    }
    
    public class BigRowVector<T extends Numeric> extends RowVector<T> {
        private BigList<T> elements;
        
        protected BigRowVector(BigList<T> source) {
            elements = source;
            setMathContext(source.get(0L).getMathContext());
        }
        
        protected BigRowVector() {
            elements = new BigList<>();
        }
        
        @Override
        public T elementAt(long index) {
            return elements.get(index);
        }

        @Override
        public long length() {
            return elements.size();
        }
        
        @Override
        public void setElementAt(T element, long position) {
            elements.set(element, position);
        }

        @Override
        public void append(T element) {
            elements.add(element);
        }

        @Override
        public RowVector<T> add(Vector<T> addend) {
            if (addend.length() != this.length()) throw new ArithmeticException("Cannot add vectors of different lengths.");
            if (addend instanceof ColumnVector) throw new ArithmeticException("Cannot add a row vector to a column vector.");
            
            BigRowVector<T> result = new BigRowVector();
            for (long index = 0L; index < length(); index++) {
                result.append((T) elements.get(index).add(addend.elementAt(index)));
            }
            return result;
        }

        @Override
        public RowVector<T> negate() {
            BigRowVector<T> result = new BigRowVector();
            elements.forEach(element -> result.append((T) element.negate()));
            return result;
        }

        @Override
        public RowVector<T> scale(T factor) {
            BigRowVector<T> result = new BigRowVector();
            elements.forEach(element -> result.append((T) element.multiply(factor)));
            return result;
        }
        
        @Override
        public T magnitude() {
            Class<? extends Numeric> clazz = elements.get(0L).getClass();
            MathContext mctx = elements.get(0L).getMathContext();
            try {
                T zero = (T) Zero.getInstance(mctx).coerceTo(clazz);
                return (T) elements.stream().reduce(zero, (x, y) -> (T) x.add(y.multiply(y))).sqrt().coerceTo(clazz);
            } catch (CoercionException ex) {
                Logger.getLogger(BigMatrix.BigRowVector.class.getName())
                        .log(Level.SEVERE, "Unable to compute magnitude of row vector.", ex);
                throw new ArithmeticException("Cannot compute magnitude of row vector.");
            }
        }

        @Override
        public Vector<T> crossProduct(Vector<T> other) {
            throw new UnsupportedOperationException("Cannot compute cross product for generalized row vector.");
        }
        
        @Override
        public long columns() { return elements.size(); }
        
        @Override
        public T valueAt(long row, long column) {
            if (row != 0L) throw new IndexOutOfBoundsException("Row vector does not have a row " + row);
            return elements.get(column);
        }

        @Override
        public ColumnVector<T> transpose() {
            return new BigColumnVector(elements);
        }
        
        @Override
        public Matrix<? extends Numeric> inverse() {
            if (length() == 1L) {
                return new SingletonMatrix(elements.get(0L).inverse());
            }
            throw new ArithmeticException("Inverse only applies to square matrices.");
        }

        @Override
        public Matrix<T> add(Matrix<T> addend) {
            if (addend.rows() != rows() || addend.columns() != columns()) {
                throw new ArithmeticException("Dimension mismatch for single-row matrix.");
            }
            Class<T> clazz = (Class<T>) elements.get(0L).getClass();
            BigRowVector<T> result = new BigRowVector<>();
            for (long index = 0L; index < elements.size(); index++) {
                try {
                    result.append((T) elements.get(index).add(addend.valueAt(0L, index)).coerceTo(clazz));
                } catch (CoercionException ex) {
                    throw new ArithmeticException("Unable to coerce matrix element to type " +
                            clazz.getTypeName() + " during matrix addition.");
                }
            }
            return result;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + elements.hashCode();
            hash = 53 * hash + Objects.hashCode(getMathContext());
            return hash;
        }

        @Override
        public String toString() {
            // 202F = Narrow No-Break Space
            return elements.stream().map(x -> x.toString()).collect(Collectors.joining(", ", "[\u202F", "\u202F]"));
        }
    }
    
    public class BigColumnVector<T extends Numeric> extends ColumnVector<T> {
        private BigList<T> elements;
        
        protected BigColumnVector(BigList<T> source) {
            elements = source;
        }
        
        protected BigColumnVector() {
            elements = new BigList<>();
        }
        @Override
        public T elementAt(long index) {
            return elements.get(index);
        }

        @Override
        public long length() {
            return elements.size();
        }
        
        @Override
        public void setElementAt(T element, long position) {
            elements.set(element, position);
        }

        @Override
        public void append(T element) {
            elements.add(element);
        }

        @Override
        public ColumnVector<T> add(Vector<T> addend) {
            if (addend.length() != this.length()) throw new ArithmeticException("Cannot add vectors of different lengths.");
            if (addend instanceof RowVector) throw new ArithmeticException("Cannot add a column vector to a row vector.");
            
            BigColumnVector<T> result = new BigColumnVector();
            for (long index = 0L; index < length(); index++) {
                result.append((T) elements.get(index).add(addend.elementAt(index)));
            }
            return result;
        }

        @Override
        public ColumnVector<T> negate() {
            BigColumnVector<T> result = new BigColumnVector();
            elements.forEach(element -> result.append((T) element.negate()));
            return result;
        }

        @Override
        public ColumnVector<T> scale(T factor) {
            BigColumnVector<T> result = new BigColumnVector();
            elements.forEach(element -> result.append((T) element.multiply(factor)));
            return result;
        }
        
        @Override
        public T magnitude() {
            Class<? extends Numeric> clazz = elements.get(0L).getClass();
            MathContext mctx = elements.get(0L).getMathContext();
            try {
                T zero = (T) Zero.getInstance(mctx).coerceTo(clazz);
                return (T) elements.stream().reduce(zero, (x, y) -> (T) x.add(y.multiply(y))).sqrt().coerceTo(clazz);
            } catch (CoercionException ex) {
                Logger.getLogger(BigMatrix.BigRowVector.class.getName())
                        .log(Level.SEVERE, "Unable to compute magnitude of row vector.", ex);
                throw new ArithmeticException("Cannot compute magnitude of row vector.");
            }
        }

        @Override
        public Vector<T> crossProduct(Vector<T> other) {
            throw new UnsupportedOperationException("Cannot compute cross product for generalized column vector.");
        }
        
        @Override
        public long rows() { return elements.size(); }
        
        @Override
        public T valueAt(long row, long column) {
            if (column != 0L) throw new IndexOutOfBoundsException("Column vector does not have a column " + column);
            return elements.get(row);
        }

        @Override
        public RowVector<T> transpose() {
            return new BigRowVector(elements);
        }
        
        @Override
        public Matrix<? extends Numeric> inverse() {
            if (length() == 1L) {
                return new SingletonMatrix(elements.get(0L).inverse());
            }
            throw new ArithmeticException("Inverse only applies to square matrices.");
        }

        @Override
        public Matrix<T> add(Matrix<T> addend) {
            if (addend.rows() != rows() || addend.columns() != columns()) {
                throw new ArithmeticException("Dimension mismatch for single-column matrix.");
            }
            Class<T> clazz = (Class<T>) elements.get(0L).getClass();
            BigColumnVector<T> result = new BigColumnVector<>();
            for (long index = 0L; index < elements.size(); index++) {
                try {
                    result.append((T) elements.get(index).add(addend.valueAt(index, 0L)).coerceTo(clazz));
                } catch (CoercionException ex) {
                    throw new ArithmeticException("Unable to coerce matrix element to type " +
                            clazz.getTypeName() + " during matrix addition.");
                }
            }
            return result;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + elements.hashCode();
            hash = 53 * hash + Objects.hashCode(getMathContext());
            return hash;
        }

        @Override
        public String toString() {
            // 202F = Narrow No-Break Space
            return elements.stream().map(x -> x.toString()).collect(Collectors.joining(", ", "[\u202F", "\u202F]ᵀ"));
        }
    }
}
