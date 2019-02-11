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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.MathContext;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.util.collections.BigList;
import tungsten.types.util.collections.LRUCache;
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
    private static final String OPT_WHITESPACE = "\\s*";
    private static final int DEFAULT_CACHE_SIZE = 50;
    private Class<T> interfaceType;
    private long rows, columns;
    private LRUCache<Long, BigList<T>> cache = new LRUCache<>(DEFAULT_CACHE_SIZE); // might move construction elsewhere
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private File sourceFile;
    private char delimiter;
    
    public BigMatrix(File destFile, long rows, long columns, char delimiter, Class<T> ofType) {
        if (destFile.exists()) throw new IllegalArgumentException("Cannot write new matrix to an already existing file.");
        checkInterfaceType(ofType);
        this.rows = rows;
        this.columns = columns;
        this.delimiter = delimiter;
        try {
            // create the new file for append purposes
            if (!destFile.createNewFile()) throw new IllegalStateException("Cannot create empty file " + destFile);
        } catch (IOException ex) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Matrix file creation failed.", ex);
            throw new IllegalStateException(ex);
        }
    }
    
    public BigMatrix(File sourceFile, char delimiter, Class<T> ofType) {
        checkInterfaceType(ofType);
        if (sourceFile.exists() && sourceFile.canRead()) {
            final String myPattern = OPT_WHITESPACE + delimiter + OPT_WHITESPACE;
            try (Scanner fileScanner = new Scanner(sourceFile)) {
                while (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine();
                    if (line == null || line.isEmpty()) continue;
                    
                    BigList<T> row = new BigList<>();
                    Scanner lineScanner = new Scanner(line);
                    lineScanner.useDelimiter(myPattern);
                    while (hasNextValue(lineScanner)) {
                        row.add(readNextValue(lineScanner));
                        if (rows == 0L) columns++;
                    }
                    lineScanner.close();
                    rows++;
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Cannot find file specified.", ex);
            }
            this.sourceFile = sourceFile;
            this.delimiter  = delimiter;
        }
    }
    
    private void checkInterfaceType(Class<T> ofType) {
        if (!IntegerType.class.isAssignableFrom(ofType) && !RealType.class.isAssignableFrom(ofType)) {
            throw new IllegalArgumentException("Currently, BigMatrix does not support type " + ofType.getTypeName());
        }
        if (!ofType.isInterface()) {
            throw new IllegalArgumentException("Supplied type must be an interface type, not a concrete class.");
        }
        interfaceType = ofType;
    }
    
    private boolean hasNextValue(Scanner s) {
        return IntegerType.class.isAssignableFrom(interfaceType) ? s.hasNextBigInteger() : s.hasNextBigDecimal();
    }
    
    private T readNextValue(Scanner s) {
        if (IntegerType.class.isAssignableFrom(interfaceType)) {
            return (T) new IntegerImpl(s.nextBigInteger());
        } else {
            return (T) new RealImpl(s.nextBigDecimal());
        }
    }
    
    @Override
    public long columns() {
        return columns;
    }

    @Override
    public long rows() {
        return rows;
    }

    @Override
    public T valueAt(long row, long column) {
        ReadLock readLock = readWriteLock.readLock();
        try {
            readLock.lock();
            BigList<T> rowContents = cache.get(row);
            if (rowContents != null) {
                return rowContents.get(column);
            } else {
                // read the row from the source file, cache it, and return our result
                rowContents = readRowFromFile(row);
                return rowContents.get(column);
            }
        } finally {
            readLock.unlock();
        }
    }
    
    public void append(RowVector<T> rowVector) {
        if (rowVector.length() != columns) {
            throw new IllegalArgumentException("Supplied row has " + rowVector.length() +
                    " elements, but this matrix has " + columns + " columns.");
        }
        try {
            if (rowVector instanceof BigRowVector) {
                writeRowIncremental((BigRowVector<T>) rowVector);
            } else {
                writeRowIncremental(new BigRowVector<>(rowVector));
            }
        } catch (IOException ioe) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Failed to write row.", ioe);
        }
    }
    
    private BigList<T> readRowFromFile(long row, boolean cacheRow) {
        WriteLock lock = readWriteLock.writeLock();
        long rowCount = 0L;
        try (Scanner fileScanner = new Scanner(sourceFile)) {
            String line = null;
            while (rowCount <= row && fileScanner.hasNextLine()) {
                line = fileScanner.nextLine();
                if (rowCount < row) rowCount++;
            }
            if (rowCount != row) throw new IndexOutOfBoundsException("Requested row " + row +
                    ", but could only read " + rowCount + " rows from backing file " + sourceFile);
            Scanner lineScanner = new Scanner(line);
            lineScanner.useDelimiter(OPT_WHITESPACE + delimiter + OPT_WHITESPACE);
            BigList<T> result = new BigList<>();
            while (hasNextValue(lineScanner)) {
                result.add(readNextValue(lineScanner));
            }
            lineScanner.close();
            if (result.size() < columns) {
                throw new IllegalStateException("Expected " + columns + " columns, but only read " + result.size());
            }
            if (cacheRow) {
                lock.lock();
                cache.put(row, result);
            }
            return result;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Unable to read " + sourceFile, ex);
            throw new IllegalStateException(ex);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
    
    private BigList<T> readRowFromFile(long row) {
        return readRowFromFile(row, true);
    }

    @Override
    public T determinant() {
        if (rows() != columns()) throw new ArithmeticException("Cannot compute determinant of a non-square matrix.");
        if (rows() < 10L) {
            BasicMatrix<T> temp = new BasicMatrix<>(this);
            return temp.determinant();
        }
        // TODO compute the determinant for sizes >= 10
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix<? extends Numeric> inverse() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
//        File backingFile = File.createTempFile("bigMatrix_intermediate", ".matrix");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void writeMatrix(File output) throws IOException {
        if (!output.exists()) {
            output.createNewFile();
        }
        try (FileWriter writer = new FileWriter(output, false)) {
            for (long row = 0L; row < rows; row++) {
                RowVector<T> values = getRow(row);
                for (long column = 0L; column < columns; column++) {
                    writer.write(values.elementAt(column).toString());
                    if (column < columns - 1L) {
                        writer.write(delimiter);
                    }
                }
                writer.write('\n');
            }
        }
    }
    
    private void writeRowIncremental(BigRowVector<T> rowVector) throws IOException {
        if (!sourceFile.exists()) {
            sourceFile.createNewFile();
        }
        try (FileWriter writer = new FileWriter(sourceFile, true)) {
            for (long column = 0L; column < columns; column++) {
                writer.write(rowVector.elementAt(column).toString());
                if (column < columns - 1L) {
                    writer.write(delimiter);
                }
            }
            writer.write('\n');
        }
    }

    @Override
    public RowVector<T> getRow(long row) {
        ReadLock readLock = readWriteLock.readLock();
        try {
            readLock.lock();
            BigList<T> rowContents = cache.get(row);
            if (rowContents != null) {
                return new BigRowVector(rowContents);
            } else {
                // read the row from the source file, cache it, and return our result
                rowContents = readRowFromFile(row);
                return new BigRowVector(rowContents);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ColumnVector<T> getColumn(long column) {
        BigColumnVector<T> result = new BigColumnVector();
        ReadLock readLock = readWriteLock.readLock();
        for (long row = 0L; row < rows; row++) {
            try {
                readLock.lock();
                BigList<T> rowContents = cache.get(row);
                if (rowContents != null) {
                    result.append(rowContents.get(column));
                } else {
                    rowContents = readRowFromFile(row, false);  // do not thrash the cache
                    result.append(rowContents.get(column));
                }
            } finally {
                readLock.unlock();
            }
        }
        return result;
    }
    
    public class BigRowVector<T extends Numeric> extends RowVector<T> {
        private BigList<T> elements;
        
        protected BigRowVector(BigList<T> source) {
            elements = source;
            setMathContext(source.get(0L).getMathContext());
        }
        
        protected BigRowVector(RowVector<T> source) {
            elements = new BigList<>();
            for (long index = 0L; index < source.length(); index++) {
                elements.add(source.elementAt(index));
            }
            setMathContext(source.elementAt(0L).getMathContext());
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
