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
package tungsten.types;

/**
 * Represents a set of objects, e.g. numeric or symbolic.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the type of elements in this set
 */
public interface Set<T> extends Iterable<T> {
    /**
     * Returns the cardinality (size) of this set.  If this is an
     * infinite set, returns -1.
     * @return the cardinality of a finite set, or -1 for an infinite set
     */
    public long cardinality();
    public boolean countable();
    public boolean contains(T element);
    public void append(T element);
    public void remove(T element);
    public Set<T> union(Set<T> other);
    public Set<T> intersection(Set<T> other);
    public Set<T> difference(Set<T> other);
}
