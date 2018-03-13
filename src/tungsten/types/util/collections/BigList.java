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
import java.util.Objects;
import java.util.stream.Stream;

/**
 * This is an analog of {@link java.util.List}, but supports {@code long}
 * indices.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the parameterized type for this list
 */
public class BigList<T> {
    private final ArrayList<ArrayList<T>> listOfLists = new ArrayList<>();
    
    public BigList() {
        listOfLists.add(new ArrayList<>());
    }
    
    public BigList(Collection<T> source) {
        listOfLists.add(new ArrayList<>(source));
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
        int arraycount = 0;
        while (index > listOfLists.get(arraycount).size()) {
            index -= listOfLists.get(arraycount).size();
            arraycount++;
        }
        return listOfLists.get(arraycount).get((int) index);
    }
    
    public void set(T obj, long index) {
        int arraycount = 0;
        while (index > listOfLists.get(arraycount).size()) {
            index -= listOfLists.get(arraycount).size();
            arraycount++;
        }
        listOfLists.get(arraycount).set((int) index, obj);
    }
    
    public void add(T obj) {
        int arrayidx = listOfLists.size() - 1;
        ArrayList<T> appendTarget;
        if (listOfLists.get(arrayidx).size() < Integer.MAX_VALUE) {
            appendTarget = listOfLists.get(arrayidx);
        } else {
            appendTarget = allocNew();
        }
        appendTarget.add(obj);
    }
    
    protected ArrayList<T> allocNew() {
        ArrayList<T> allocated = new ArrayList<>();
        listOfLists.add(allocated);
        return allocated;
    }
    
    public void remove(T obj) {
        listOfLists.parallelStream().filter(x -> x.contains(obj)).forEach(x -> x.removeIf(y -> y.equals(obj)));
    }
    
    public void clear() {
        listOfLists.clear();
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
        Stream<T> intermediate = Stream.empty();
        for (ArrayList<T> list : listOfLists) {
            intermediate = Stream.concat(intermediate, list.parallelStream());
        }
        return intermediate;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof BigList) {
            BigList that = (BigList) o;
            if (this.size() != that.size()) return false;
            
            for (long idx = 0; idx < this.size(); idx++) {
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
}
