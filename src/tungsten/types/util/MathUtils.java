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
package tungsten.types.util;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.impl.IntegerImpl;

/**
 * A utility class to hold commonly used functions and algorithms.
 *
 * @author tarquin
 */
public class MathUtils {
    private static final BigInteger TWO = BigInteger.valueOf(2L);
    
    private static final Map<Long, BigInteger> factorialCache = new HashMap<>();
    
    public static IntegerType factorial(IntegerType n) {
        if (n.asBigInteger().equals(BigInteger.ZERO) || n.asBigInteger().equals(BigInteger.ONE)) {
            return new IntegerImpl(BigInteger.ONE);
        } else if (getCacheFor(n) != null) {
            return new IntegerImpl(getCacheFor(n));
        }
        
        final long ncmp = n.asBigInteger().longValueExact();
        Long m = factorialCache.keySet().parallelStream().filter(x -> x < ncmp).max(Long::compareTo).orElse(null);
        
        BigInteger accum = m != null ? factorialCache.get(m) : BigInteger.ONE;
        BigInteger intermediate = n.asBigInteger();
        BigInteger bailout = m != null ? BigInteger.valueOf(m + 1L) : TWO;
        while (intermediate.compareTo(bailout) >= 0) {
            accum = accum.multiply(intermediate);
            intermediate = intermediate.subtract(BigInteger.ONE);
        }
        cacheFact(n, accum);
        return new IntegerImpl(accum);
    }
    
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    
    private static void cacheFact(BigInteger n, BigInteger value) {
        if (n.compareTo(TWO) >= 0 && n.compareTo(MAX_LONG) < 0) {
            Long key = n.longValueExact();
            
            if (!factorialCache.containsKey(key)) factorialCache.put(key, value);
        }
    }
    
    private static void cacheFact(IntegerType n, BigInteger value) {
        cacheFact(n.asBigInteger(), value);
    }
    
    private static BigInteger getCacheFor(BigInteger n) {
        try {
            return factorialCache.get(n.longValueExact());
        } catch (ArithmeticException e) {
            Logger.getLogger(MathUtils.class.getName()).log(Level.FINER, "Attempt to cache a factorial value for n outside Long range.", e);
            return null; // this is the same as if we had a regular cache miss
        }
    }
    
    private static BigInteger getCacheFor(IntegerType n) {
        return getCacheFor(n.asBigInteger());
    }
}
