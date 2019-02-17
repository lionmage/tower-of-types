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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.annotations.Constant;
import tungsten.types.annotations.ConstantFactory;
import tungsten.types.numerics.impl.Euler;
import tungsten.types.numerics.impl.ImaginaryUnit;
import tungsten.types.numerics.impl.One;
import tungsten.types.numerics.impl.Pi;
import tungsten.types.numerics.impl.Zero;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class Symbol {
    private final String name;
    private final String representation;
    private Scope scope;
    private Class<? extends Numeric> valueClass;
    private Numeric concreteValue;  // TODO can this be made to handle objects like matrix, vector, function, etc.?
    
    private static final ConcurrentMap<String, Symbol> GLOBAL = new ConcurrentHashMap<>();
    public static final String PKG_SCAN_PROP = "tungsten.scan.external.types";
    
    static {
        final String[] externalTypePackages = System.getProperty(PKG_SCAN_PROP, "").split(",");
        final String[] packagesToScan = (String[]) Array.newInstance(String.class, externalTypePackages.length + 1);
        packagesToScan[0] = "tungsten.types.numerics.impl";  // scan our own stuff first
        System.arraycopy(externalTypePackages, 0, packagesToScan, 1, externalTypePackages.length);
        ScanResult scanResult = new ClassGraph().enableAllInfo().whitelistPackages(packagesToScan).scan();
        ClassInfoList classInfoList = scanResult.getClassesWithAnnotation(Constant.class.getName());
        // start with numerics
        List<Class<Numeric>> classes = classInfoList.loadClasses(Numeric.class);
        classes.forEach(clazz -> {
            Constant annotation = clazz.getAnnotation(Constant.class);
            Logger.getLogger(Symbol.class.getName()).log(Level.FINE,
                    "Processing Class {} for symbol {}, represented as {}",
                    new Object[] {clazz.getSimpleName(), annotation.name(), annotation.representation()});
            GLOBAL.put(annotation.name(),
                    new Symbol(annotation.name(), annotation.representation(), clazz));
        });
    }
    
    /**
     * Create a new {@link Symbol} in the global scope.
     * This object has no assigned value.
     * @param name
     * @param representation 
     */
    public Symbol(String name, String representation) {
        this.name = name;
        this.representation = representation;
        cacheThis(name);
    }
    
    /**
     * Some symbols have a name that is the same as the representation.
     * @param name 
     */
    public Symbol(String name) {
        this(name, name);
    }
    
    /**
     * Create a {@link Symbol} that represents a constant.
     * @param name
     * @param representation
     * @param valueClass 
     */
    public Symbol(String name, String representation, Class<? extends Numeric> valueClass) {
        // we are not auto-caching in this case
        this.name = name;
        this.representation = representation;
        this.valueClass = valueClass;
    }
    
    /**
     * Create a {@link Symbol} in a given {@link Scope}.
     * @param name
     * @param representation
     * @param scope 
     */
    public Symbol(String name, String representation, Scope scope) {
        this.scope = scope;
        this.name  = name;
        this.representation = representation;
    }
    
    /**
     * Convenience constructor for creating a {@link Symbol} that has
     * the same name and representation, with a given initial value
     * and a {@link Scope}.
     * @param name
     * @param value
     * @param scope 
     */
    public Symbol(String name, Numeric value, Scope scope) {
        this.scope = scope;
        this.name  = name;
        this.representation = name;
        this.concreteValue = value;
    }
    
    /**
     * Full-freight constructor.
     * 
     * @param name
     * @param representation
     * @param value
     * @param scope 
     */
    public Symbol(String name, String representation, Numeric value, Scope scope) {
        this(name, representation, scope);
        this.concreteValue = value;
    }
    
    private void cacheThis(String key) {
        Symbol old = GLOBAL.put(key, this);
        if (old != null) {
            Logger.getLogger(Symbol.class.getName()).log(Level.WARNING,
                    "Symbol named {} redefined from {} to {}", new Object[] {key, old, this});
        }
    }
    
    /**
     * Get the bound names for all {@link Symbol}s.
     * @return an unmodifiable {@link Set} view of the names of currently bound symbols
     */
    public static java.util.Set<String> getAllSymbolNames() {
        return Collections.unmodifiableSet(GLOBAL.keySet());
    }
    
    /**
     * Get all globally bound symbols.
     * @return a {@link Set} of all globally bound symbols.
     */
    public static java.util.Set<Symbol> getAllSymbols() {
        return new HashSet<>(GLOBAL.values());
    }
    
    /**
     * Obtain a {@link Symbol} bound in the global context.
     * 
     * @param name
     * @return a {@link Symbol} bound to the given name, or {@code null} if no such symbol exists
     */
    public static Symbol getForName(String name) {
        return GLOBAL.get(name);
    }
    
    /**
     * Get a {@link Symbol} bound to the given name, or a brand new
     * {@link Symbol} if none exists.
     * 
     * @param name
     * @param representation
     * @return an existing {@link Symbol} or brand new {@link Symbol} bound to the given name
     */
    public static Symbol getOrCreate(String name, String representation) {
        return GLOBAL.getOrDefault(name, new Symbol(name, representation));
    }

    public String getName() {
        return name;
    }

    public String getRepresentation() {
        return representation;
    }
    
    /**
     * Returns the {@link Class} of the numeric value represented by this
     * symbol if and only if this symbol represents a constant.
     * 
     * @return an {@link Optional} which contains the {@link Class} of a numeric constant, if present
     */
    public Optional<Class<? extends Numeric>> getValueClass() {
        return Optional.ofNullable(valueClass);
    }
    
    public void setConcreteValue(Numeric value) {
        if (getValueClass().isPresent()) {
            throw new UnsupportedOperationException("Symbol " + name + " is a constant.");
        }
        if (concreteValue != null) {
            final Class<? extends Numeric> oldClass = concreteValue.getClass();
            final Class<? extends Numeric> newClass = value.getClass();
            if (!oldClass.isAssignableFrom(newClass)) {
                Logger.getLogger(Symbol.class.getName()).log(Level.WARNING,
                        "Symbol {} changing type of bound value from {} to {}.",
                        new Object[] {name, oldClass.getTypeName(), newClass.getTypeName()});
            }
        }
        concreteValue = Objects.requireNonNull(value, "Cannot undefine symbol value.");
    }
    
    public Optional<? extends Numeric> getConcreteValue() {
        return Optional.ofNullable(concreteValue);
    }
    
    public Optional<? extends Numeric> getValueInstance(MathContext mctx) {
        Optional<Class<? extends Numeric>> clazz = getValueClass();
        if (!clazz.isPresent()) return Optional.empty();
        
        try {
            Optional<Method> annotatedMethod = Arrays.stream(clazz.get().getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ConstantFactory.class)).findFirst();
            annotatedMethod.ifPresent(method -> {
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalStateException("Factory method must be static.");
                }
                ConstantFactory factoryAnnotation = method.getAnnotation(ConstantFactory.class);
                if (!factoryAnnotation.argType().isAssignableFrom(MathContext.class)) {
                    throw new IllegalArgumentException("Cannot instantiate " + clazz.get().getSimpleName() + " with given MathContext.");
                }
                if (!Numeric.class.isAssignableFrom(factoryAnnotation.returnType())) {
                    throw new IllegalStateException("Factory method " + method.getName() +
                            " returns a concrete type of " + factoryAnnotation.returnType().getSimpleName());
                }
            });
            // if we didn't find an annotated factory method, look for the default getInstance() method
            Method m = annotatedMethod.isPresent() ? annotatedMethod.get() : clazz.get().getMethod("getInstance", MathContext.class);
            return Optional.of(clazz.get().cast(m.invoke(null, mctx)));  // m is a static method
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(Symbol.class.getName()).log(Level.SEVERE,
                    "Unable to obtain an instance of " + clazz.get().getTypeName(), ex);
        }
        return Optional.empty();
    }
    
    /**
     * Obtain the {@link Scope} for this {@link Symbol}.
     * If the returned {@link Optional} is empty, this symbol belongs
     * to the global scope.
     * 
     * @return an {@link Optional<Scope>} representing the scope of this symbol
     *   definition, or {@link Optional#EMPTY} if globally scoped
     */
    public Optional<Scope> getScope() {
        return Optional.ofNullable(scope);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.name);
        hash = 53 * hash + Objects.hashCode(this.representation);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Symbol other = (Symbol) obj;
        return Objects.equals(this.name, other.name);
    }
    
    @Override
    public String toString() {
        return representation;
    }
}
