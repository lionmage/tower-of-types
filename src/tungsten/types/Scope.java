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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.annotations.Constant;

/**
 * A basic encapsulation of scope, which in this context means binding of
 * symbols to values, etc.  A scope may or may not have a parent scope, and
 * may have children.  If there is no parent scope, then this scope is rooted
 * in the global namespace; that global namespace is handled by {@link Symbol}.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class Scope {
    private final ConcurrentHashMap<String, Symbol> locallyDefined = new ConcurrentHashMap<>();
    private Scope parentScope;
    
    public Optional<Scope> getParentScope() { return Optional.ofNullable(parentScope); }
    
    public Scope createNewChildScope() {
        Scope child = new Scope();
        child.parentScope = this;
        return child;
    }
    
    public Optional<Symbol> getForName(String name) {
        return Optional.ofNullable(locallyDefined.getOrDefault(name,
                getParentScope().map(scope -> scope.getForName(name).orElse(Symbol.getForName(name))).orElse(null)));
    }
    
    public java.util.Set<String> getAllSymbolNames() {
        return Collections.unmodifiableSet(locallyDefined.keySet());
    }
    
    public java.util.Set<Symbol> getAllSymbols() {
        return new HashSet<>(locallyDefined.values());
    }
    
    public Symbol getOrCreate(String name, String representation) {
        return locallyDefined.getOrDefault(name, new Symbol(name, representation, this));
    }
    
    public Optional<Symbol> unbind(String name) {
        return Optional.ofNullable(locallyDefined.remove(name));
    }
    
    public void rebind(Symbol binding) {
        Symbol previous = locallyDefined.put(binding.getName(), binding);
        if (previous == null) {
            Logger.getLogger(Scope.class.getName()).log(Level.WARNING,
                    "Symbol {} was rebound, but had no previous binding", binding.getName());
        } else {
            if (!previous.getRepresentation().equals(binding.getRepresentation())) {
                Logger.getLogger(Scope.class.getName()).log(Level.SEVERE,
                        "Symbol {} rebound with new representation; old = {}, current = {}",
                        new Object[] { binding.getName(), previous.getRepresentation(), binding.getRepresentation() });
            }
        }
    }

    public void cacheValue(String name, Numeric value) {
        checkIfAlreadyCached(name);
        Symbol symbol = new Symbol(name, name, this);
        symbol.setConcreteValue(value);
        locallyDefined.put(name, symbol);
    }

    private void checkIfAlreadyCached(String name) throws IllegalStateException {
        if (locallyDefined.containsKey(name)) {
            throw new IllegalStateException("Cache already contains entry for "
                    + name + " with value " + locallyDefined.get(name).getConcreteValue());
        }
    }
    
    public void cacheValue(String name, String representation, Numeric value) {
        checkIfAlreadyCached(name);
        Symbol symbol = new Symbol(name, representation, value, this);
        locallyDefined.put(name, symbol);
    }
    
    /**
     * Cache a {@link Symbol} explicitly. If the symbol has already been
     * associated with a different scope, the operation will fail without
     * throwing an exception.
     * 
     * @param symbol the symbol to cache
     * @return true if successful, false otherwise
     */
    public boolean cacheSymbol(Symbol symbol) {
        checkIfAlreadyCached(symbol.getName());
        if (!symbol.getScope().isPresent()) {
            symbol.updateScope(this);
            locallyDefined.put(symbol.getName(), symbol);
            return true;
        } else {
            return false;
        }
    }
    
    public Optional<? extends Numeric> updateValue(Symbol symbol, Numeric newValue) {
        final String name = symbol.getName();
        if (!locallyDefined.containsKey(name)) {
            Logger.getLogger(Scope.class.getName()).log(Level.INFO,
                    "Symbol {} is being redefined in this scope from an ancestor scope.", name);
            if (Symbol.getForName(name) != null &&
                    Symbol.getForName(name).getValueClass()
                            .map(clazz -> clazz.isAnnotationPresent(Constant.class)).orElse(false)) {
                throw new IllegalArgumentException("Cannot redefine a constant value in any scope.");
            }
        }
        
        Symbol old = locallyDefined.getOrDefault(name,
                getParentScope().flatMap((scope) -> scope.getForName(name)).orElse(symbol));
        final Optional<? extends Numeric> oldValue = old.getConcreteValue();
        oldValue.ifPresent(value -> {
            if (value.equals(newValue)) {
                Logger.getLogger(Scope.class.getName()).log(Level.INFO,
                        "Symbol {} is being updated in this scope, but the value {} appears to be unchanged.",
                        new Object[] { name, value });
            }
        });
        symbol.setConcreteValue(newValue);
        locallyDefined.put(name, symbol);
        return oldValue;
    }
    
    /**
     * Update the concrete value bound to a symbol. This method returns
     * the previously bound value.  Note that if the symbol was bound in a
     * parent {@link Scope}, this operation will rebind the symbol in the
     * current {@link Scope}, thus masking the binding in the parent scope.
     * 
     * @param name the unique name of the symbol to be rebound
     * @param newValue the new value to bind to the named symbol
     * @return the previously bound value of the named symbol
     */
    public Optional<? extends Numeric> updateValue(String name, Numeric newValue) {
        Optional<Symbol> old = getForName(name);
        if (old.isPresent()) {
            Symbol fresh = locallyDefined.containsKey(name) ? old.get() : new Symbol(name, old.get().getRepresentation(), this);
            Optional<? extends Numeric> oldValue = old.get().getConcreteValue();
            fresh.setConcreteValue(newValue);
            locallyDefined.put(name, fresh);
            return oldValue;
        } else {
            throw new IllegalArgumentException("Symbol " + name + " does not exist!");
        }
    }
}
