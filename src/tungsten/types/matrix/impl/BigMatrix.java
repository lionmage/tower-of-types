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
import java.io.RandomAccessFile;
import java.math.MathContext;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.util.FileMonitor;
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
    public static final String FILE_EXTENSION = ".matrix";
    private static final String OPT_WHITESPACE = "\\s*";
    private static final String HORIZONTAL_ELLIPSIS = "\u2026\u2009";  // horizontal ellipsis followed by thin-space
    private static final int DEFAULT_ROW_CACHE_SIZE = 5;
    private static final int DEFAULT_OFFSET_CACHE_SIZE = 10000;
    private Class<T> interfaceType;
    private long rows, columns;
    private LRUCache<Long, BigList<T>> rowCache = new LRUCache<>(DEFAULT_ROW_CACHE_SIZE); // might move construction elsewhere
    private LRUCache<Long, Long> offsetCache = new LRUCache<>(DEFAULT_OFFSET_CACHE_SIZE);
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
        this.sourceFile = destFile;
    }
    
    public BigMatrix(File sourceFile, char delimiter, Class<T> ofType) {
        checkInterfaceType(ofType);
        long startingRowOffset = 0L;  // in case we have to put in some metadata on the first line of the matrix
        if (sourceFile.exists() && sourceFile.canRead()) {
            final String myPattern = OPT_WHITESPACE + delimiter + OPT_WHITESPACE;
            try (RandomAccessFile source = new RandomAccessFile(sourceFile, "r")) {
                String line = null;
                offsetCache.put(0L, startingRowOffset);
                while ((line = source.readLine()) != null) {
                    if (line == null || line.isEmpty()) continue;
                    
                    if (rows == 0L) {
                        // read the first row and count the columns
                        BigList<T> row = new BigList<>();
                        try (Scanner lineScanner = new Scanner(line)) {
                            lineScanner.useDelimiter(myPattern);
                            while (hasNextValue(lineScanner)) {
                                row.add(readNextValue(lineScanner));
                                columns++;
                            }
                        }
                        // now that we have a row, we should cache it
                        rowCache.put(rows, row);
                    }
                    offsetCache.put(++rows, source.getFilePointer());
                }
                // set up a file listener for any changes
                FileMonitor.getInstance().monitorFile(sourceFile, () -> {
                    WriteLock lock = readWriteLock.writeLock();
                    try {
                        rowCache.clear();
                        offsetCache.clear();
                        RandomAccessFile mySource = new RandomAccessFile(sourceFile, "r");
                        Scanner scanner = new Scanner(mySource.readLine());
                        scanner.useDelimiter(myPattern);
                        columns = 0L;
                        scanner.forEachRemaining(element -> columns++);
                        rows = Files.lines(sourceFile.toPath()).filter(x -> x != null)
                                .filter(x -> !x.isEmpty()).count();
                    } catch (IOException ex) {
                        Logger.getLogger(BigMatrix.class.getName()).log(Level.INFO,
                                "IO error encountered while reloading matrix parameters.", ex);
                    } finally {
                        lock.unlock();
                    }
                });
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Cannot find file specified.", ex);
                throw new IllegalStateException(ex);
            } catch (IOException ex) {
                Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Error reading from " + sourceFile.getName(), ex);
                throw new IllegalStateException(ex);
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
    
    private void skipNextValue(Scanner s) {
        // just read and quietly consume the values
        if (IntegerType.class.isAssignableFrom(interfaceType)) {
            s.nextBigInteger();
        } else {
            s.nextBigDecimal();
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
            BigList<T> rowContents = rowCache.get(row);
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
    
    private Boolean utcache;
    private Boolean ltcache;
    private Lock utLock = new ReentrantLock();
    private Lock ltLock = new ReentrantLock();
    
    @Override
    public boolean isUpperTriangular() {
        utLock.lock();
        try {
            if (utcache != null) return utcache.booleanValue();
            // cache this for posterity
            utcache = Matrix.super.isUpperTriangular();
            return utcache.booleanValue();
        } finally {
            utLock.unlock();
        }
    }
    
    @Override
    public boolean isLowerTriangular() {
        ltLock.lock();
        try {
            if (ltcache != null) return ltcache.booleanValue();
            // cache this for posterity
            ltcache = Matrix.super.isLowerTriangular();
            return ltcache.booleanValue();
        } finally {
            ltLock.unlock();
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
        long startingRowOffset = 0L;  // in case we have to put in some metadata on the first line of the matrix
        try (RandomAccessFile source = new RandomAccessFile(sourceFile, "r")) {
            final long highestCachedRow = offsetCache.keySet().stream().filter(r -> r <= row).max((x, y) -> x.compareTo(y)).orElse(0L);
            long offset = offsetCache.getOrDefault(highestCachedRow, startingRowOffset);
            source.seek(offset);
            long rowCount = highestCachedRow;
            String line = null;
            while (rowCount <= row && (line = source.readLine()) != null) {
                // if we've just read the row right before the one we want,
                // we are exactly at the beginning of the row we want
                if (rowCount == row - 1L) offsetCache.put(row, source.getFilePointer());
                if (rowCount < row) rowCount++;
            }
            if (rowCount != row) throw new IndexOutOfBoundsException("Requested row " + row +
                    ", but could only read " + rowCount + " rows from backing file " + sourceFile);
            BigList<T> result = new BigList<>();
            try (Scanner lineScanner = new Scanner(line)) {
                lineScanner.useDelimiter(OPT_WHITESPACE + delimiter + OPT_WHITESPACE);
                while (hasNextValue(lineScanner)) {
                    result.add(readNextValue(lineScanner));
                }
            }
            if (result.size() < columns) {
                throw new IllegalStateException("Expected " + columns + " columns, but only read " + result.size());
            }
            if (cacheRow) {
                lock.lock();
                rowCache.put(row, result);
            }
            return result;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Unable to read " + sourceFile, ex);
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Error while reading " + sourceFile.getName(), ex);
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
        if (rows() <= 10L) {
            BasicMatrix<T> temp = new BasicMatrix<>(this);
            return temp.determinant();
        }
        // TODO compute the determinant for sizes > 10
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<? extends Numeric> inverse() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
        if (addend.rows() != rows || addend.columns() != columns) {
            throw new ArithmeticException("Matrix dimensions must match.");
        }
        try {
            final File backingFile = File.createTempFile("bigMatrix_sum_temp", FILE_EXTENSION);
            backingFile.deleteOnExit();
            BigMatrix<T> result = new BigMatrix(backingFile, rows, columns, delimiter, interfaceType);
            for (long row = 0L; row < rows; row++) {
                RowVector<T> ours = this.getRow(row);
                RowVector<T> theirs = addend.getRow(row);
                result.append(ours.add((Vector<T>) theirs));
            }
            return result;
        } catch (IOException ex) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Failed to create backing store for result.", ex);
        }
        throw new IllegalStateException("Unable to persist sum.");
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        if (this.columns() != multiplier.rows()) {
            throw new ArithmeticException("Multiplier must have the same number of rows as this matrix has columns.");
        }
        try {
            final File backingFile = File.createTempFile("bigMatrix_prod_temp", FILE_EXTENSION);
            backingFile.deleteOnExit();
            BigMatrix<T> result = new BigMatrix(backingFile, this.rows(), multiplier.columns(), delimiter, interfaceType);
            for (long row = 0L; row < rows(); row++) {
                BigList<T> accum = new BigList<>();
                RowVector<T> rowvec = this.getRow(row);
                for (long column = 0L; column < multiplier.columns(); column++) {
                    accum.add(rowvec.dotProduct(multiplier.getColumn(column)));
                }
                result.append(new BigRowVector(accum));
            }
            return result;
        } catch (IOException ioe) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Failed to create backing store for result.", ioe);
        }
        throw new IllegalStateException("Unable to persist product.");
    }
    
    @Override
    public Matrix<T> scale(T scaleFactor) {
        try {
            final File backingFile = File.createTempFile("bigMatrix_scale_temp", FILE_EXTENSION);
            backingFile.deleteOnExit();
            BigMatrix<T> result = new BigMatrix(backingFile, this.rows(), this.columns(), delimiter, interfaceType);
            for (long row = 0L; row < rows(); row++) {
                RowVector<T> rowvec = this.getRow(row).scale(scaleFactor);
                result.append(rowvec);
            }
            return result;
        } catch (IOException ioe) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Failed to create backing store for result.", ioe);
        }
        throw new IllegalStateException("Unable to persist scaled matrix.");
    }
    
    public void writeMatrix(File output) throws IOException {
        if (!output.exists()) {
            output.createNewFile();
        }
        try (FileWriter writer = new FileWriter(output, false)) {
            for (long row = 0L; row < rows; row++) {
                RowVector<T> values = getRow(row);
                for (long column = 0L; column < columns; column++) {
                    // shouldn't use toString() directly on RealType because
                    // some values (e.g., pi) have a non-numeric representation
                    if (IntegerType.class.isAssignableFrom(interfaceType)) {
                        writer.write(((IntegerType) values.elementAt(column)).asBigInteger().toString());
                    } else {
                        writer.write(((RealType) values.elementAt(column)).asBigDecimal().toString());
                    }
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
                if (IntegerType.class.isAssignableFrom(interfaceType)) {
                    writer.write(((IntegerType) rowVector.elementAt(column)).asBigInteger().toString());
                } else {
                    writer.write(((RealType) rowVector.elementAt(column)).asBigDecimal().toString());
                }
                if (column < columns - 1L) {
                    writer.write(delimiter);
                }
            }
            writer.write('\n');
        }
    }

    @Override
    public RowVector<T> getRow(long row) {
        if (row < 0L || row >= rows) throw new IndexOutOfBoundsException("Row index " + row + " is outside range 0 - " + rows);
        ReadLock readLock = readWriteLock.readLock();
        try {
            readLock.lock();
            BigList<T> rowContents = rowCache.get(row);
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
        if (column < 0L || column >= columns) throw new IndexOutOfBoundsException("Column index " + column + " is outside range 0 - " + columns);
        BigColumnVector<T> result = new BigColumnVector();
        ReadLock readLock = readWriteLock.readLock();
        long startingRowOffset = 0L; // for if we need to put metadata at the start of the file
        try (RandomAccessFile raf = new RandomAccessFile(sourceFile, "r")) {
            raf.seek(startingRowOffset);
            readLock.lock();
            for (long row = 0L; row < rows; row++) {
                BigList<T> rowContents = rowCache.get(row);
                if (rowContents != null) {
                    result.append(rowContents.get(column));
                    // seek to the next row
                    if (offsetCache.containsKey(row + 1L)) {
                        raf.seek(offsetCache.get(row + 1L));
                    } else {
                        while (raf.read() != '\n'); // consume to the end of line without reading into a String
                    }
                } else {
                    try (Scanner s = new Scanner(raf.readLine())) {
                        for (long idx = 0L; idx < column; idx++) skipNextValue(s);
                        result.append(readNextValue(s));
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE,
                    "File " + sourceFile.getName() + " cannot be read during getColumn() operation; column value = " + column, ex);
            throw new IllegalStateException(ex);
        } catch (IOException ioe) {
            Logger.getLogger(BigMatrix.class.getName()).log(Level.SEVERE, "Exception while reading matrix values from file.", ioe);
            throw new IllegalStateException(ioe);
        } finally {
            readLock.unlock();
        }
        return result;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Matrix) {
            Matrix<? extends Numeric> that = (Matrix<Numeric>) o;
            if (rows() != that.rows()) return false;
            for (long row = 0L; row < rows(); row++) {
                if (!getRow(row).equals(that.getRow(row))) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.interfaceType);
        hash = 53 * hash + (int) (this.rows ^ (this.rows >>> 32));
        hash = 53 * hash + (int) (this.columns ^ (this.columns >>> 32));
        hash = 53 * hash + Objects.hashCode(this.sourceFile);
        hash = 53 * hash + this.delimiter;
        return hash;
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
        public Stream<T> stream() {
            return elements.stream();
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
            // Since this is potentially a huge vector, only display the first and last 3 elements.
            List<String> values = new ArrayList<>();
            elements.head(3L).stream().map(x -> x.toString()).forEachOrdered(values::add);
            values.add(HORIZONTAL_ELLIPSIS);
            elements.tail(3L).stream().map(x -> x.toString()).forEachOrdered(values::add);
            // 202F = Narrow No-Break Space
            return values.stream().collect(Collectors.joining(", ", "[\u202F", "\u202F]ᵀ"));
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
                        .log(Level.SEVERE, "Unable to compute magnitude of column vector.", ex);
                throw new ArithmeticException("Cannot compute magnitude of column vector.");
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
        public Stream<T> stream() {
            return elements.stream();
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
            // Since this is potentially a huge vector, only display the first and last 3 elements.
            List<String> values = new ArrayList<>();
            elements.head(3L).stream().map(x -> x.toString()).forEachOrdered(values::add);
            values.add(HORIZONTAL_ELLIPSIS);
            elements.tail(3L).stream().map(x -> x.toString()).forEachOrdered(values::add);
            // 202F = Narrow No-Break Space
            return values.stream().collect(Collectors.joining(", ", "[\u202F", "\u202F]ᵀ"));
        }
    }
}
