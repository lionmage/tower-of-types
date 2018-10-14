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
package tungsten.types.numerics.impl;

import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tungsten.types.Numeric;

/**
 * A representation of zero for situations where zero is being asymptotically
 * approached from a negative value.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class NegZero extends Zero {
    private NegZero(MathContext mctx) {
        super(mctx);
    }
    
    private static final Map<MathContext, NegZero> instanceMap = new HashMap<>();
    private static final Lock instanceLock = new ReentrantLock();
    
    public static Numeric getInstance(MathContext mctx) {
        instanceLock.lock();
        try {
            NegZero instance = instanceMap.get(mctx);
            if (instance == null) {
                instance = new NegZero(mctx);
                instanceMap.put(mctx, instance);
            }
            return instance;
        } finally {
            instanceLock.unlock();
        }
    }

    @Override
    public Numeric inverse() {
//        throw new ArithmeticException("Cannot divide by zero");
        return NegInfinity.getInstance(mctx);
    }

    @Override
    public String toString() { return "\u22120"; }
}
