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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.annotations.Columnar;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;
import tungsten.types.util.collections.LRUCache;
import tungsten.types.vector.impl.ColumnVector;

/**
 * A {@link Matrix} implementation which stores each column in a
 * separate file store.  The columns are aggregated under a single
 * directory, which may also contain additional metadata.
 * 
 * WIP: Do not instantiate yet.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the {@link Numeric} subtype of the elements of this matrix
 */
@Columnar
public class VerticalMatrix<T extends Numeric> implements Matrix<T> {
    public static final String DIR_EXTENSION = ".vmatrix";
    public static final String FILE_EXTENSION = ".column";
    public static final String FILE_PREFIX = "col";
    private static final String VERTICAL_ELLIPSIS = "\u22EE";
    private final Class<T> clazz;
    private File directory;
    private long rows, columns;
    
    public VerticalMatrix(long rows, long columns, File directory, Class<T> clazz) {
        this.rows = rows;
        this.columns = columns;
        this.directory = directory;
        this.clazz = clazz;
        if (!directory.exists()) {
            assert directory.getName().endsWith(DIR_EXTENSION);
            if (!directory.mkdirs()) throw new IllegalStateException("Could not create directory " + directory);
        }
    }
    
    public VerticalMatrix(File directory, Class<T> clazz) {
        this.directory = directory;
        this.clazz = clazz;
        this.characterize();
    }
    
    private static final DirectoryStream.Filter<Path> columnFilter =
            new DirectoryStream.Filter<Path>() {
        @Override
        public boolean accept(Path entry) throws IOException {
            return entry.getFileName().toString().endsWith(FILE_EXTENSION);
        }
    };
    
    private void characterize() {
        if (!directory.isDirectory()) throw new IllegalArgumentException(directory.getName() + " is not a directory.");
        assert columns == 0L;
        assert rows == 0L;
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory.toPath(), columnFilter)) {
            for (Path entry : stream) {
                if (columns == 0L) {
                    VerticalVector v = new VerticalVector(entry.toFile());
                    Logger.getLogger(VerticalMatrix.class.getName()).log(Level.INFO,
                            "Scanned column {} and obtained {} rows.",
                            new Object[] {v.getColumnIndex(), v.length()});
                    rows = v.length();
                }
                columns++;
            }
        } catch (IOException ioe) {
            Logger.getLogger(VerticalMatrix.class.getName()).log(Level.SEVERE,
                    "Error scanning directory " + directory.getName(), ioe);
            throw new IllegalStateException(ioe);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public T determinant() {
        if (columns != rows) {
            throw new ArithmeticException("Cannot compute determinant of a non-square matrix.");
        }
        return new SubMatrix<>(this).determinant();
    }

    @Override
    public Matrix<? extends Numeric> inverse() {
        if (columns != rows) {
            throw new ArithmeticException("Cannot compute inverse of a non-square matrix.");
        }
        if (columns <= 1000L) {
            return new ColumnarMatrix<>(this).inverse();
        }
        // TODO replace with custom implementation
        return new SubMatrix<>(this).inverse();
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
    public Matrix<T> scale(T scaleFactor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ColumnVector<T> getColumn(long column) {
        return Matrix.super.getColumn(column); //To change body of generated methods, choose Tools | Templates.
    }
    
    public class VerticalVector<T extends Numeric> extends ColumnVector<T> {
        private final File backingFile;
        private final LRUCache<Long, T> elements;
        private final LRUCache<Long, Long> offsets;
        private long numElements;
        private long columnIndex;
        
        public VerticalVector(long column) {
            String fname = FILE_PREFIX + column + FILE_EXTENSION;
            columnIndex = column;
            backingFile = new File(directory, fname);
            numElements = characterize();
            elements = new LRUCache<>(computeCacheSize());
            offsets  = new LRUCache<>((int) Math.min(numElements, 25L));
        }
        
        
        public VerticalVector(File columnFile) {
            backingFile = columnFile;
            final String fileName = columnFile.getName();
            assert fileName.startsWith(FILE_PREFIX);
            assert fileName.endsWith(FILE_EXTENSION);
            String index = columnFile.getName().substring(3, fileName.lastIndexOf(FILE_EXTENSION));
            columnIndex = Long.parseLong(index);
            numElements = characterize();
            elements = new LRUCache<>(computeCacheSize());
            offsets  = new LRUCache<>((int) Math.min(numElements, 25L));
        }
        
        private long characterize() {
            if (!backingFile.exists()) return 0L;
            try {
                return Files.lines(backingFile.toPath()).filter(x -> x != null)
                        .filter(x -> !x.isEmpty()).count();
            } catch (IOException ex) {
                Logger.getLogger(VerticalMatrix.class.getName()).log(Level.SEVERE,
                        "Error reading backing store " + backingFile.getName(), ex);
            }
            return -1L;
        }
        
        private int computeCacheSize() {
            if (numElements <= 0) return 10;  // bail out with small default
            
            long intermediate = numElements;
            if (numElements > Integer.MAX_VALUE) {
                intermediate >>= 32L;
            }
            if (intermediate > 1_000_000) {
                intermediate /= 1000L;
            } else {
                intermediate /= 100L; // 1% of values
            }
            return (int) Math.max(intermediate, 10L);
        }
        
        @Override
        public long length() {
            return numElements;
        }
        
        protected long getColumnIndex() {
            return columnIndex;
        }
        
        @Override
        public T elementAt(long index) {
            T value = elements.get(index);
            if (value == null) {
                try {
                    // read the file and obtain the missing value, then cache it
                    RandomAccessFile raf = new RandomAccessFile(backingFile, "r");
                    long cacheIdx = offsets.keySet().stream().filter(x -> x <= index)
                            .max(Comparator.naturalOrder()).orElse(0L);
                    long offset = offsets.getOrDefault(cacheIdx, 0L);
                    raf.seek(offset);
                    long counter = index - cacheIdx;
                    String line = null;
                    while (counter-- > 0L) {
                        if (counter == 0L) offsets.put(index, raf.getFilePointer());
                        line = raf.readLine();
                    }
                    value = (T) clazz.getConstructor(String.class).newInstance(line);
                    elements.put(index, value);  // cache this value
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(VerticalMatrix.class.getName()).log(Level.SEVERE, "Backing store does not exist.", ex);
                    throw new IllegalStateException(ex);
                } catch (IOException ioe) {
                    Logger.getLogger(VerticalMatrix.class.getName()).log(Level.SEVERE,
                            "Error obtaining element at index " + index, ioe);
                    throw new IllegalStateException(ioe);
                } catch (NoSuchMethodException | InstantiationException ex) {
                    Logger.getLogger(VerticalMatrix.class.getName()).log(Level.SEVERE,
                            "Cannot construct type " + clazz.getTypeName(), ex);
                    throw new IllegalStateException(ex);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    Logger.getLogger(VerticalMatrix.class.getName()).log(Level.SEVERE, "Unexpected exception encountered.", ex);
                    throw new IllegalStateException(ex);
                }
            }
            
            return value;
        }
        
        @Override
        public void setElementAt(T element, long position) {
            throw new UnsupportedOperationException("Currently unsupported.");
        }
        
        @Override
        public void append(T element) {
            RandomAccessFile raf;
            try {
                if (!backingFile.exists()) {
                    backingFile.createNewFile();  // might not be strictly necessary
                    FileWriter fw = new FileWriter(backingFile);
                    fw.write(elementToString(element));
                    fw.write('\n');
                    fw.close();
                    elements.put(numElements++, element);
                    return;
                }
                
                raf = new RandomAccessFile(backingFile, "rw");
                // otherwise, append to the end of the file
            } catch (FileNotFoundException ex) {
                Logger.getLogger(VerticalMatrix.class.getName()).log(Level.SEVERE,
                        "Backing store " + backingFile.getName() + " was not created.", ex);
            } catch (IOException ex) {
                Logger.getLogger(VerticalMatrix.class.getName()).log(Level.SEVERE, "Error appending value" + element, ex);
            }
            
        }
        
        private String elementToString(T element) {
            if (element instanceof IntegerType) {
                return ((IntegerType) element).asBigInteger().toString();
            } else if (element instanceof RealType) {
                return ((RealType) element).asBigDecimal().toString();
            }
            // otherwise, delegate
            return element.toString();
        }
    }
}
