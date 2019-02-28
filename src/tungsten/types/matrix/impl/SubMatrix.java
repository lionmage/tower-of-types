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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.Vector;
import tungsten.types.numerics.impl.Zero;
import tungsten.types.vector.impl.ColumnVector;
import tungsten.types.vector.impl.RowVector;

/**
 * This class provides a limited view into the supplied {@link Matrix}.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the {@link Numeric} subtype for the elements of this matrix
 */
public class SubMatrix<T extends Numeric> implements Matrix<T> {
    private final Matrix<T> original;
    private long startRow, endRow;
    private long startColumn, endColumn;
    private final List<Long> removedRows = new ArrayList<>();
    private final List<Long> removedColumns = new ArrayList<>();
    
    public SubMatrix(Matrix<T> original) {
        this.original = original;
        startRow = 0L;
        endRow = original.rows() - 1L; // bounds are inclusive
        startColumn = 0L;
        endColumn = original.columns() - 1L;
    }
    
    public SubMatrix(Matrix<T> original, long row1, long column1, long row2, long column2) {
        if (row1 < 0L || row1 >= original.rows() || row2 < 0L || row2 >= original.rows()) {
            throw new IllegalArgumentException("Row indices must be within range.");
        }
        if (column1 < 0L || column1 >= original.columns() || column2 < 0L || column2 >= original.columns()) {
            throw new IllegalArgumentException("Column indices must be within range.");
        }
        if (row1 > row2) throw new IllegalArgumentException("row1 must be <= row2");
        if (column1 > column2) throw new IllegalArgumentException("column1 must be <= column2");
        this.original = original;
        this.startRow = row1;
        this.endRow   = row2;
        this.startColumn = column1;
        this.endColumn   = column2;
    }

    @Override
    public long columns() {
        return internalColumns() - removedColumns.size();
    }

    @Override
    public long rows() {
        return internalRows() - removedRows.size();
    }
    
    private long internalRows() {
        return endRow - startRow + 1L;
    }
    
    private long internalColumns() {
        return endColumn - startColumn + 1L;
    }
    
    public void removeRow(long row) {
        if (row < 0L || row >= internalRows()) throw new IndexOutOfBoundsException("Row index " + row + " out of bounds");
        if (row == 0L) {
            AtomicLong removedCount = new AtomicLong();
            // this is cheaper than tracking a removed row, but is irreversible
            while (removedRows.contains(++startRow)) { // incrementally move the start bound inward
                removedRows.remove(startRow); // eat up any adjacent rows that were marked as removed
                removedCount.incrementAndGet();
            }
            // shift all indices
            removedRows.replaceAll(val -> val - removedCount.longValue());
            return;
        } else if (row == internalRows() - 1L) {
            while (removedRows.contains(--endRow)) {
                removedRows.remove(endRow);
            }
            return;
        }
        if (!removedRows.contains(row)) removedRows.add(row);
    }
    
    public void removeColumm(long column) {
        if (column < 0L || column >= internalColumns()) throw new IndexOutOfBoundsException("Column index " + column + " out of bounds");
        if (column == 0L) {
            AtomicLong removedCount = new AtomicLong();
            // this is cheaper than tracking a removed row, but is irreversible
            while (removedColumns.contains(++startColumn)) { // incrementally move the start bound inward
                removedColumns.remove(startColumn); // eat up any adjacent rows that were marked as removed
                removedCount.incrementAndGet();
            }
            // shift all indices
            removedColumns.replaceAll(val -> val - removedCount.longValue());
            return;
        } else if (column == internalColumns() - 1L) {
            while (removedColumns.contains(--endColumn)) {
                removedColumns.remove(endColumn);
            }
            return;
        }
        if (!removedColumns.contains(column)) removedColumns.add(column);
    }
    
    private long computeRowIndex(long row) {
        long result = startRow + row;
        AtomicLong intermediateRow = new AtomicLong(result);
        while (removedRows.stream().anyMatch(x -> x <= intermediateRow.get())) {
            result = intermediateRow.incrementAndGet();
        }
        if (result >= original.rows()) {
            throw new IndexOutOfBoundsException(String.format("Provided row index %d maps to %d in the underlying matrix, whoch only has %d rows.",
                    row, result, original.rows()));
        }
        return result;
    }
    
    private long computeColumnIndex(long column) {
        long result = startColumn + column;
        AtomicLong intermediateColumn = new AtomicLong(result);
        while (removedColumns.stream().anyMatch(x -> x <= intermediateColumn.get())) {
            result = intermediateColumn.incrementAndGet();
        }
        if (result >= original.columns()) {
            throw new IndexOutOfBoundsException(String.format("Provided column index %d maps to %d in the underlying matrix, whoch only has %d columns.",
                    column, result, original.columns()));
        }
        return result;
    }

    @Override
    public T valueAt(long row, long column) {
        if (row < 0L || row >= rows()) throw new IndexOutOfBoundsException("Row parameter is out of range.");
        if (column < 0L || column >= columns()) throw new IndexOutOfBoundsException("Column parameter is out of range.");
        return original.valueAt(computeRowIndex(row), computeColumnIndex(column));
    }

    @Override
    public T determinant() {
        if (rows() != columns()) throw new ArithmeticException("Determinant only applies to square matrices.");
        if (rows() == 2L) {
            T a = valueAt(0L, 0L);
            T b = valueAt(0L, 1L);
            T c = valueAt(1L, 0L);
            T d = valueAt(1L, 1L);
            return (T) a.multiply(d).subtract(c.multiply(b));
        }
        if (rows() > 4L && isTriangular()) {
            Numeric accum = valueAt(0L, 0L);
            final Numeric zero = Zero.getInstance(accum.getMathContext());
            for (long index = 1L; index < rows(); index++) {
                accum = accum.multiply(valueAt(index, index));
                if (accum.equals(zero)) break;
            }
            return (T) accum;
        }
        return new BasicMatrix<>(this).determinant();
    }

    @Override
    public Matrix<? extends Numeric> inverse() {
        return new BasicMatrix<>(this).inverse();
    }

    @Override
    public Matrix<T> add(Matrix<T> addend) {
        if (addend.rows() != this.rows() || addend.columns() != this.columns()) {
            throw new ArithmeticException("Addend must have the same dimensions as this matrix.");
        }
        BasicMatrix<T> sum = new BasicMatrix<>();
        for (long index = 0L; index < rows(); index++) {
            sum.append(getRow(index).add((Vector<T>) addend.getRow(index)));
        }
        return sum;
    }

    @Override
    public Matrix<T> multiply(Matrix<T> multiplier) {
        if (this.columns() != multiplier.rows()) {
            throw new ArithmeticException("Multiplier must have the same number of rows as this matrix has columns.");
        }
        T[][] temp = (T[][]) Array.newInstance(valueAt(0L, 0L).getClass(), (int) this.rows(), (int) multiplier.columns());
        for (long row = 0L; row < rows(); row++) {
            RowVector<T> rowvec = this.getRow(row);
            for (long column = 0L; column < multiplier.columns(); column++) {
                temp[(int) row][(int) column] = rowvec.dotProduct(multiplier.getColumn(column));
            }
        }
        return new BasicMatrix<>(temp);
    }

    @Override
    public RowVector<T> getRow(long row) {
        List<T> result = new ArrayList<>();
        original.getRow(computeRowIndex(row)).stream().skip(startColumn).limit(columns()).forEachOrdered(result::add);
        removeFromList(result, removedRows);
        return new RowVector<>(result);
    }

    @Override
    public ColumnVector<T> getColumn(long column) {
        List<T> result = new ArrayList<>();
        original.getColumn(computeColumnIndex(column)).stream().skip(startRow).limit(rows()).forEachOrdered(result::add);
        removeFromList(result, removedColumns);
        return new ColumnVector<>(result);
    }

    private void removeFromList(List<T> source, List<Long> indices) {
        indices.sort(Comparator.naturalOrder());  // make sure the biggest indices are at the end
        ListIterator<Long> iter = indices.listIterator(indices.size());
        while (iter.hasPrevious()) {  // work backwards through the list of indices and remove
            source.remove(iter.previous().intValue());
        }
    }
    
    public SortedSet<Long> getRemovedRowIndices() {
        return new TreeSet<>(removedRows);
    }
    
    public SortedSet<Long> getRemovedColumnIndices() {
        return new TreeSet<>(removedColumns);
    }
}
