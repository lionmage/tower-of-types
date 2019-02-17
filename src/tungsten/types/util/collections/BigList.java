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
package tungsten.types.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is an analog of {@link java.util.List}, but supports {@code long}
 * indices.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the parameterized type for this list
 */
public class BigList<T> implements Iterable<T> {
    private final ArrayList<ArrayList<T>> listOfLists = new ArrayList<>();
    private final ArrayList<Integer> capacities = new ArrayList<>();
    
    public BigList() {
        listOfLists.add(new ArrayList<>());
        capacities.add(-1);  // unset capacity
    }
    
    public BigList(Collection<T> source) {
        listOfLists.add(new ArrayList<>(source));
        capacities.add(source.size());  // capacity is at least size of source collection
    }
    
    public BigList(long capacity) {
        while (capacity > (long) Integer.MAX_VALUE) {
            listOfLists.add(new ArrayList<>(Integer.MAX_VALUE));
            capacities.add(Integer.MAX_VALUE);
        }
        if (capacity > 0L) {
            listOfLists.add(new ArrayList<>((int) capacity));
            capacities.add((int) capacity);
        }
    }
    
    public long size() {
        return listOfLists.parallelStream().mapToLong(x -> (long) x.size()).sum();
    }
    
    public boolean isEmpty() {
        return listOfLists.isEmpty() ||
                (listOfLists.size() == 1 && listOfLists.get(0).isEmpty());
    }
    
    public boolean contains(T obj) {
        return listOfLists.parallelStream().filter(x -> x.contains(obj)).findAny().isPresent();
    }
    
    public long indexOf(T obj) {
        long position = 0L;
        for (ArrayList<T> list : listOfLists) {
            if (list.contains(obj)) {
                return position + list.indexOf(obj);
            }
            position += (long) list.size();
        }
        return -1L;
    }
    
    public T get(long index) {
        if (index < 0L) {
            return reverseStream().skip(Math.abs(index + 1)).findFirst().orElseThrow(IndexOutOfBoundsException::new );
        }
        
        int arraycount = 0;
        while (index > listOfLists.get(arraycount).size()) {
            index -= listOfLists.get(arraycount).size();
            arraycount++;
        }
        return listOfLists.get(arraycount).get((int) index);
    }
    
    public void set(T obj, long index) {
        if (index < 0L) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
        int arraycount = 0;
        while (index > capacityOrSize(arraycount)) {
            index -= capacityOrSize(arraycount);
            arraycount++;
        }
        listOfLists.get(arraycount).set((int) index, obj);
    }
    
    private Stream<T> reverseStream() {
        Stream.Builder<Stream<T>> builder = Stream.builder();
        for (int idx = listOfLists.size() - 1; idx >= 0; idx--) {
            final ArrayList<T> innerList = listOfLists.get(idx);
            
            ListIterator<T> iter = innerList.listIterator(innerList.size());
            Iterator<T> rev = new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return iter.hasPrevious();
                }

                @Override
                public T next() {
                    return iter.previous();
                }
            };
            final int characteristics = Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
            builder.accept(StreamSupport.stream(Spliterators.spliterator(rev, size(), characteristics), false));
        }
        return builder.build().flatMap(x -> x);
    }
    
    private int capacityOrSize(int arrayidx) {
        return Math.max(listOfLists.get(arrayidx).size(), capacities.get(arrayidx));
    }
    
    public void add(T obj) {
        int arrayidx = listOfLists.size() - 1;
        ArrayList<T> appendTarget;
        if (listOfLists.get(arrayidx).size() < Integer.MAX_VALUE) {
            appendTarget = listOfLists.get(arrayidx);
            if (capacities.get(arrayidx) != -1) {
                capacities.set(arrayidx, capacities.get(arrayidx) + 1);
            }
        } else {
            appendTarget = allocNew();
        }
        appendTarget.add(obj);
    }
    
    protected ArrayList<T> allocNew() {
        ArrayList<T> allocated = new ArrayList<>();
        listOfLists.add(allocated);
        capacities.add(-1);  // no initial capacity, so use placeholder
        return allocated;
    }
    
    public void remove(T obj) {
        // since this opperation has the potential to shift indices in multiple places,
        // we set the capacities for all sublists to -1 to avoid problems with set and
        // append operations on the underlying sublists
        for (int k = 0; k < capacities.size(); k++) capacities.set(k, -1);
        listOfLists.parallelStream().filter(x -> x.contains(obj)).forEach(x -> x.removeIf(y -> y.equals(obj)));
    }
    
    /**
     * Compactify this {@link BigList} so that its internal storage
     * is as efficient as possible.  This is the sort of operation
     * one might use after performing a {@link #remove(java.lang.Object) }
     * operation.  This could get expensive for very large lists.
     */
    public void compact() {
        int lastIndex = listOfLists.size() - 1;
        for (int k = 0; k < lastIndex; k++) {
            ArrayList<T> workingList = listOfLists.get(k);
            ArrayList<T> nextList = listOfLists.get(k + 1);
            while (nextList.size() > 0 && workingList.size() < Integer.MAX_VALUE) {
                T element = nextList.remove(0);
                workingList.add(element);
            }
        }
        int lastCapIndex = capacities.size() - 1;
        assert lastCapIndex == lastIndex;
        if (listOfLists.get(lastIndex).isEmpty()) {
            listOfLists.remove(lastIndex--);
            capacities.remove(lastCapIndex--);
        }
        // now clean up the capacities list
        for (int k = 0; k < listOfLists.size() - 2; k++) {
            final int size = listOfLists.get(k).size();
            if (size < Integer.MAX_VALUE) {
                Logger.getLogger(BigList.class.getName()).log(Level.WARNING, "Unexpected size for sublist {0} is {1}", new Object[]{k, size});
            }
            capacities.set(k, size);
        }
        // ensure the max capacity for the last list
        if (capacities.size() > 1 && capacities.get(lastCapIndex) < Integer.MAX_VALUE) {
            listOfLists.get(lastIndex).ensureCapacity(Integer.MAX_VALUE);
            capacities.set(lastCapIndex, Integer.MAX_VALUE);
        }
    }
    
    public void clear() {
        listOfLists.clear();
        capacities.clear();
        allocNew();
    }
    
    public Stream<T> stream() {
        Stream<T> intermediate = Stream.empty();
        for (ArrayList<T> list : listOfLists) {
            intermediate = Stream.concat(intermediate, list.stream());
        }
        return intermediate;
    }
    
    public Stream<T> parallelStream() {
        return StreamSupport.stream(this.spliterator(), true);
    }
    
    public static <E> BigList<E> singleton(E element) {
        return new BigList<E>() {
            private final E singleElement = element;
            @Override
            public long size() { return 1L; }
            @Override
            public boolean isEmpty() { return false; }
            @Override
            public boolean contains(E e) { return this.singleElement.equals(e); }
            @Override
            public long indexOf(E e) { return this.contains(e) ? 0L : -1L; }
            @Override
            public E get(long index) { return index == 0L ? singleElement : null; }
            @Override
            public void set(E obj, long index) { throw new UnsupportedOperationException("Not supported."); }
            @Override
            public void add(E obj) { throw new UnsupportedOperationException("Not supported."); }
            @Override
            public void remove(E obj) { throw new UnsupportedOperationException("Not supported."); }
            @Override
            public void clear() { throw new UnsupportedOperationException("Not supported."); }
            @Override
            public Stream<E> stream() { return Stream.of(singleElement); }
            @Override
            public Stream<E> parallelStream() { return stream(); }
            @Override
            public Iterator<E> iterator() { return stream().iterator(); }
        };
    }
    
    /**
     * Apply the given function to each element of this {@link BigList},
     * returning a new {@link BigList} containing the transformed elements.
     * @param <R> the type of the elements in the returned {@link BigList}
     * @param func a function which takes elements of type T and returns elements of type R
     * @return a newly constructed {@link BigList} with the transformed elements
     */
    public <R> BigList<R> apply(Function<T, R> func) {
        BigList<R> result = new BigList<>();
        stream().map(func).forEachOrdered(result::add);
        
        return result;
    }
    
    /**
     * Atomically apply the given function to the entire {@link BigList} at once.
     * The function takes a {@link BigList} of T elements and returns a
     * {@link BigList} of R elements.
     * @param <R> the type of the elements in the returned {@link BigList}
     * @param func a function which takes a {@link BigList} of T and returns a {@link BigList} of R
     * @return a newly constructed {@link BigList} resulting from the direct application of the function
     */
    public <R> BigList<R> applyAtomic(Function<BigList<T>, BigList<R>> func) {
        return func.apply(this);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof BigList) {
            BigList that = (BigList) o;
            if (this.size() != that.size()) return false;
            
            for (long idx = 0L; idx < this.size(); idx++) {
                if (!this.get(idx).equals(that.get(idx))) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.listOfLists);
        return hash;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int arrayidx = 0;
            private int position = 0;
            private boolean nextCalled = false;
            private boolean removeCalled = false;
            
            @Override
            public boolean hasNext() {
                try {
                    ArrayList<T> list = listOfLists.get(arrayidx);
                    return position < list.size();
                } catch (IndexOutOfBoundsException e) {
                    Logger.getLogger("BigListIterator").log(Level.FINER, stateInfo(), e);
                    return false;
                }
            }

            @Override
            public T next() {
                try {
                    ArrayList<T> list = listOfLists.get(arrayidx);
                    if (position >= list.size()) {
                        list = listOfLists.get(++arrayidx);
                        position = 0;
                    }
                    nextCalled = true;
                    removeCalled = false; // reset flag
                    return list.get(position++);
                } catch (IndexOutOfBoundsException e) {
                    Logger.getLogger("BigListIterator").log(Level.FINER, stateInfo(), e);
                    throw new NoSuchElementException("At the end of BigList.");
                }
            }
            
            @Override
            public void remove() {
                if (nextCalled && !removeCalled) {
                    removeCalled = true;
                    try {
                        ArrayList<T> list = listOfLists.get(arrayidx);
                        list.remove(position - 1);
                    } catch (IndexOutOfBoundsException e) {
                        Logger.getLogger("BigListIterator").log(Level.FINER, stateInfo(), e);
                        throw new IllegalStateException("Unexpected state: failed at " + stateInfo());
                    }
                } else {
                    if (removeCalled) {
                        throw new IllegalStateException("Already called remove().");
                    }
                    throw new IllegalStateException("Cannot call remove() before next().");
                }
            }
            
            private String stateInfo() {
                StringBuilder buf = new StringBuilder();
                buf.append("ArrayList #").append(arrayidx);
                buf.append(", position ").append(position + 1);
                buf.append(" [nextCalled = ").append(nextCalled);
                buf.append(", removeCalled = ").append(removeCalled).append(']');
                return buf.toString();
            }
        };
    }
    
    @Override
    public Spliterator<T> spliterator() {
        if (listOfLists.isEmpty() ||
                this.size() == 0L) return Spliterators.emptySpliterator();
        if (listOfLists.size() == 1) {
            return listOfLists.get(0).spliterator();
        }

        return new Spliterator<T>() {
            private long position;
            
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if (position < size()) {
                    action.accept(get(position++));
                    return true;
                }
                return false;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                int sublist = 0;
                long index = position;
                while (index > (long) listOfLists.get(sublist).size()) {
                    index -= (long) listOfLists.get(sublist++).size();
                    if (sublist >= listOfLists.size()) return;
                }
                
                final ArrayList startlist = listOfLists.get((int) sublist);
                startlist.subList((int) index, startlist.size()).forEach(action);
                for (int i = sublist + 1; i < listOfLists.size(); i++) {
                    listOfLists.get(i).forEach(action);
                }
            }

            @Override
            public Spliterator<T> trySplit() {
                int partitionSize = listOfLists.size() / 2;
                if (partitionSize == 0) return null;
                
                int sublist = 0;
                long index = position;
                while (index > (long) listOfLists.get(sublist).size()) {
                    index -= (long) listOfLists.get(sublist++).size();
                    if (sublist >= listOfLists.size()) return null;
                }
                final ArrayList startlist = listOfLists.get((int) sublist);
                position += (long) startlist.size() - index;
                if (partitionSize == 1) {
                    return Spliterators.spliterator(startlist.subList((int) index, startlist.size()),
                            SIZED | SUBSIZED | ORDERED);
                } else {
                    Stream.Builder builder = Stream.builder()
                            .add(startlist.subList((int) index, startlist.size()).stream());
                    for (int i = sublist + 1; i < partitionSize; i++) {
                        position += (long) listOfLists.get(i).size();
                        builder.add(listOfLists.get(i).stream());
                    }
                    // TODO needs testing
                    return builder.build().flatMap(x -> x).spliterator();
                }
            }

            @Override
            public long estimateSize() {
                return size() - position;
            }
            
            @Override
            public int characteristics() {
                return SIZED | ORDERED;
            }
        };
    }
}
