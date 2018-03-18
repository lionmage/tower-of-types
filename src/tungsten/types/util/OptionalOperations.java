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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.MathContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * This is a utility class that uses reflection to safely implement operations
 * on one of the {@link Numeric} or {@link Vector} types.  If the desired
 * method is not present, calling one of these is a no-op.  This is an
 * alternative to implementing additional interfaces on concrete implementations
 * of {@link Numeric} or {@link Vector} and then testing for the presence of
 * those interfaces everywhere.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class OptionalOperations {
    public static void setMathContext(Object obj, MathContext mctx) {
        try {
            Method m = obj.getClass().getMethod("setMathContext", MathContext.class);
            m.invoke(obj, mctx);
        } catch (NoSuchMethodException ex) {
            // silently ignore this
        } catch (SecurityException | IllegalAccessException ex) {
            Logger.getLogger(OptionalOperations.class.getName()).log(Level.SEVERE, "Failed to invoke setMathContext() due to security or access issue.", ex);
        } catch (IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(OptionalOperations.class.getName()).log(Level.SEVERE, "Bad target or bad MathContext argument.", ex);
        }
    }
    
    public static <T> Stream<T> obtainStream(Object obj) {
        Stream<T> s;
        try {
            Method m = obj.getClass().getMethod("stream");
            s = (Stream<T>) m.invoke(obj);
        } catch (NoSuchMethodException ex) {
            s = Stream.empty();
        } catch (SecurityException | IllegalAccessException ex) {
            Logger.getLogger(OptionalOperations.class.getName()).log(Level.SEVERE, "Failed to invoke stream() due to security or access issue.", ex);
            throw new IllegalStateException(ex);
        } catch (InvocationTargetException ex) {
            s = Stream.empty();
            Logger.getLogger(OptionalOperations.class.getName()).log(Level.SEVERE, "Bad target.", ex);
        }
        return s;
    }

    public static <T> Stream<T> obtainParallelStream(Object obj) {
        Stream<T> s;
        try {
            Method m = obj.getClass().getMethod("parallelStream");
            s = (Stream<T>) m.invoke(obj);
        } catch (NoSuchMethodException ex) {
            // we can attempt to fake it out if there's a stream() method
            s = ((Stream<T>) obtainStream(obj)).parallel();
        } catch (SecurityException | IllegalAccessException ex) {
            Logger.getLogger(OptionalOperations.class.getName()).log(Level.SEVERE, "Failed to invoke parallelStream() due to security or access issue.", ex);
            throw new IllegalStateException(ex);
        } catch (InvocationTargetException ex) {
            s = Stream.empty();
            Logger.getLogger(OptionalOperations.class.getName()).log(Level.SEVERE, "Bad target.", ex);
        }
        return s;
    }
}
