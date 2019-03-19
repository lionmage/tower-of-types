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
package tungsten.types;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * An abstraction of container types such as {@link Set} and {@link Vector}.
 * This class is mainly intended to be used for binding {@link Symbol}s to
 * aggregate types.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 * @param <T> the type of the elements contained in this aggregate
 */
public class AggregateType<T extends Numeric> {
    private final Object aggregateObject;
    private final Class<T> elementType;
    
    public AggregateType(Set<T> s, Class<T> elementType) {
        aggregateObject = s;
        this.elementType = elementType;
    }
    
    public AggregateType(Vector<T> v, Class<T> elementType) {
        aggregateObject = v;
        this.elementType = elementType;
    }
    
    public AggregateType(Matrix<T> m, Class<T> elementType) {
        aggregateObject = m;
        this.elementType = elementType;
    }
    
    public EnumSet<SupportedTypes> getSupportedInterfaces() {
        return SupportedTypes.getFor(aggregateObject.getClass());
    }
    
    public Class<T> getElementType() {
        return elementType;
    }
    
    public Set<T> asSet() {
        if (getSupportedInterfaces().contains(SupportedTypes.SET)) {
            return (Set<T>) aggregateObject;
        }
        throw new UnsupportedOperationException("Aggregate object of type " +
                aggregateObject.getClass().getTypeName() + " cannot be cast to a Set.");
    }
    
    public Vector<T> asVector() {
        if (getSupportedInterfaces().contains(SupportedTypes.VECTOR)) {
            return (Vector<T>) aggregateObject;
        }
        throw new UnsupportedOperationException("Aggregate object of type " +
                aggregateObject.getClass().getTypeName() + " cannot be cast to a Vector.");
    }
    
    public Matrix<T> asMatrix() {
        if (getSupportedInterfaces().contains(SupportedTypes.MATRIX)) {
            return (Matrix<T>) aggregateObject;
        }
        throw new UnsupportedOperationException("Aggregate object of type " +
                aggregateObject.getClass().getTypeName() + " cannot be cast to a Matrix.");

    }
    
    public Collection<T> asCollection() {
        if (getSupportedInterfaces().contains(SupportedTypes.VECTOR)) {
            Vector<T> vector = (Vector<T>) aggregateObject;
            LinkedList<T> list = new LinkedList<>();
            for (long index = 0; index < vector.length(); index++) {
                list.addLast(vector.elementAt(index));
            }
            return list;
        } else if (getSupportedInterfaces().contains(SupportedTypes.SET)) {
            Set<T> set = (Set<T>) aggregateObject;
            if (set.cardinality() >= 0L) {
                HashSet<T> hashSet = new HashSet<>();
                set.forEach(hashSet::add);
                return hashSet;
            }
            throw new UnsupportedOperationException("Infinite sets cannot be converted to a collection.");
        }
        throw new UnsupportedOperationException("Only Vector and Set can be converted to a Java collection.");
    }
    
    public enum SupportedTypes {
        SET(Set.class),
        VECTOR(Vector.class),
        MATRIX(Matrix.class);
        
        private final Class<?> typeOfAggregate;
        
        private SupportedTypes(Class<?> typeOfAggregate) {
            this.typeOfAggregate = typeOfAggregate;
        }
        
        public Class<?> getType() {
            return typeOfAggregate;
        }
        
        protected static EnumSet<SupportedTypes> getFor(Class<?> clazz) {
            EnumSet<SupportedTypes> result = EnumSet.noneOf(SupportedTypes.class);
            for (SupportedTypes value : values()) {
                if (value.typeOfAggregate.isAssignableFrom(clazz)) result.add(value);
            }
            return result;
        }
    }
}
