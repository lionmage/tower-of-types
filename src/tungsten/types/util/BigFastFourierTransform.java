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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.ComplexPolarImpl;
import tungsten.types.numerics.impl.Pi;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.util.collections.BigList;

/**
 * An implementation of FFT which uses recursion and the Fork/Join framework
 * first introduced in Java 7.  It requires the following of the input:
 * <ul>
 * <li>The input must have an even number of elements.</li>
 * <li>The input elements must already be instances of {@link ComplexType}.</li>
 * </ul>
 * This implementation relies upon {@link BigList} instead of {@link java.util.List}
 * or one of its subclasses.
 * This work is based heavily on an FFT implementation provided by Princeton.
 *
 * @author tarquin
 * @see <a href="https://introcs.cs.princeton.edu/java/97data/FFT.java.html">Princeton's FFT implementation</a>
 */
public class BigFastFourierTransform implements Function<BigList<ComplexType>, BigList<ComplexType>> {
    final MathContext mctx;
    
    public BigFastFourierTransform(MathContext mctx) {
        this.mctx = mctx;
    }

    @Override
    public BigList<ComplexType> apply(BigList<ComplexType> t) {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        FFTRecursiveTask task = new FFTRecursiveTask(t);
        return commonPool.invoke(task);
    }
    
    private class FFTRecursiveTask extends RecursiveTask<BigList<ComplexType>> {
        private final BigList<ComplexType> source;
        
        private FFTRecursiveTask(BigList<ComplexType> source) {
            this.source = source;
        }

        @Override
        protected BigList<ComplexType> compute() {
            long length = source.size();
            if (length == 1L) {
                return BigList.singleton(source.get(0));
            } else if (length % 2L != 0L) {
                throw new IllegalArgumentException("Fourier transform requires an even-length List.");
            }
            FFTRecursiveTask[] tasks = createSubtasks();
            
            ForkJoinTask.invokeAll(tasks[0], tasks[1]);
            return combine(tasks[0].join(), tasks[1].join());
        }
        
        private FFTRecursiveTask[] createSubtasks() {
            BigList<ComplexType>[] split = splitList(source);
            BigList<ComplexType> q = split[0];  // even
            BigList<ComplexType> r = split[1];  // odd
            FFTRecursiveTask[] result = {new FFTRecursiveTask(q), new FFTRecursiveTask(r)};
            return result;
        }
    }
    
    private BigList<ComplexType> combine(BigList<ComplexType> q, BigList<ComplexType> r) {
        assert q.size() == r.size();
        final RealImpl one = new RealImpl(BigDecimal.ONE, true);
        final RealImpl negtwo = new RealImpl("-2", true);
        one.setMathContext(mctx);
        negtwo.setMathContext(mctx);
        RealType negtwopiovern = (RealType) Pi.getInstance(mctx).multiply(negtwo).divide(new RealImpl(BigDecimal.valueOf(2L * q.size())));
        BigList<ComplexType> result = new BigList(q.size() * 2L);
        
        for (long i = 0L; i < q.size(); i++) {
            RealType kth = (RealType) negtwopiovern.multiply(new RealImpl(BigDecimal.valueOf(i)));
            ComplexType wk = new ComplexPolarImpl(one, kth);
            result.set((ComplexType) q.get(i).add(wk.multiply(r.get(i))), i);
            result.set((ComplexType) q.get(i).subtract(wk.multiply(r.get(i))), i + q.size());
        }
        return result;
    }
    
    private BigList<ComplexType>[] splitList(BigList<ComplexType> source) {
        BigList<ComplexType> evenElements;
        BigList<ComplexType> oddElements;
        final long n = source.size() / 2L;
        evenElements = new BigList<>(n);
        oddElements  = new BigList<>(n);
        
        for (long i = 0L; i < source.size(); i++) {
            switch ((int) (i % 2L)) {
                case 0:
                    evenElements.add(source.get(i));
                    break;
                case 1:
                    oddElements.add(source.get(i));
                    break;
            }
        }
        assert evenElements.size() == oddElements.size();
        BigList[] result = {evenElements, oddElements};
        
        return result;
    }
}
