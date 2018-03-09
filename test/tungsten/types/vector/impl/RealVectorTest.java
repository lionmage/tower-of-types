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
package tungsten.types.vector.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import tungsten.types.Vector;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.Pi;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.util.OptionalOperations;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class RealVectorTest {
    private RealVector vect1;
    
    public RealVectorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        String[] values = {"1", "2", "3", "4", "5"};
        List<RealType> elements = Arrays.stream(values).map(x -> new RealImpl(x)).collect(Collectors.toList());
        vect1 = new RealVector(elements);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of setMathContext method, of class RealVector.
     */
    @Test
    public void testSetMathContext() {
        System.out.println("setMathContext");
        MathContext mctx = null;
        RealVector instance = null;
        instance.setMathContext(mctx);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of length method, of class RealVector.
     */
    @Test
    public void testLength() {
        System.out.println("length");
        RealVector instance = vect1;
        long expResult = 5L;
        long result = instance.length();
        assertEquals(expResult, result);
        
        instance.append(new RealImpl("5.6"));
        expResult = 6L;
        result = instance.length();
        assertEquals(expResult, result);
    }

    /**
     * Test of elementAt method, of class RealVector.
     */
    @Test
    public void testElementAt() {
        System.out.println("elementAt");
        long position = 1L;
        RealVector instance = vect1;
        RealType expResult = new RealImpl("2");
        RealType result = instance.elementAt(position);
        assertEquals(expResult, result);
    }

    /**
     * Test of setElementAt method, of class RealVector.
     */
    @Test
    public void testSetElementAt() {
        System.out.println("setElementAt");
        RealType element = new RealImpl("-7");
        long position = 1L;
        RealVector instance = vect1;
        instance.setElementAt(element, position);
        assertEquals(element, instance.elementAt(position));
    }

    /**
     * Test of add method, of class RealVector.
     */
    @Test
    public void testAdd() {
        System.out.println("add");
        Vector<RealType> addend = null;
        RealVector instance = null;
        Vector<RealType> expResult = null;
        Vector<RealType> result = instance.add(addend);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of subtract method, of class RealVector.
     */
    @Test
    public void testSubtract() {
        System.out.println("subtract");
        Vector<RealType> subtrahend = null;
        RealVector instance = null;
        Vector<RealType> expResult = null;
        Vector<RealType> result = instance.subtract(subtrahend);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of negate method, of class RealVector.
     */
    @Test
    public void testNegate() {
        System.out.println("negate");
        RealVector instance = null;
        Vector<RealType> expResult = null;
        Vector<RealType> result = instance.negate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of magnitude method, of class RealVector.
     */
    @Test
    public void testMagnitude() {
        System.out.println("magnitude");
        RealVector instance = null;
        RealType expResult = null;
        RealType result = instance.magnitude();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of dotProduct method, of class RealVector.
     */
    @Test
    public void testDotProduct() {
        System.out.println("dotProduct");
        Vector<RealType> other = null;
        RealVector instance = null;
        RealType expResult = null;
        RealType result = instance.dotProduct(other);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of crossProduct method, of class RealVector.
     */
    @Test
    public void testCrossProduct() {
        System.out.println("crossProduct");
        Vector<RealType> other = null;
        RealVector instance = null;
        Vector<RealType> expResult = null;
        Vector<RealType> result = instance.crossProduct(other);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of computeAngle method, of class RealVector.
     */
    @Test
    public void testComputeAngle() {
        System.out.println("computeAngle");
        MathContext ctx = new MathContext(10, RoundingMode.HALF_UP);
        String[] values1 = {"1.2", "0.0"};
        String[] values2 = {"1.6", "1.6"};
        Vector<RealType> other = new RealVector(Arrays.stream(values2).map(x -> new RealImpl(x)).collect(Collectors.toList()));
        OptionalOperations.setMathContext(other, ctx);
        RealVector instance = new RealVector(Arrays.stream(values1).map(x -> new RealImpl(x)).collect(Collectors.toList()));
        instance.setMathContext(ctx);
        // Should be a 45 degree angle between these two vectors.
        RealType expResult = (RealType) Pi.getInstance(ctx).divide(new RealImpl("4.0"));
        RealType result = instance.computeAngle(other);
        assertEquals(expResult, result);
    }

    /**
     * Test of append method, of class RealVector.
     */
    @Test
    public void testAppend() {
        System.out.println("append");
        RealType element = new RealImpl("3.52");
        RealVector instance = vect1;
        instance.append(element);
        assertEquals(6L, instance.length());
        assertEquals(element, instance.elementAt(5L));
    }

    /**
     * Test of scale method, of class RealVector.
     */
    @Test
    public void testScale() {
        System.out.println("scale");
        RealType factor = new RealImpl(BigDecimal.valueOf(2L));
        RealVector instance = vect1;
        String[] values = {"2", "4", "6", "8", "10"};
        List<RealType> elements = Arrays.stream(values).map(x -> new RealImpl(x)).collect(Collectors.toList());
        Vector<RealType> expResult = new RealVector(elements);
        Vector<RealType> result = instance.scale(factor);
        assertEquals(expResult, result);
    }

    /**
     * Test of normalize method, of class RealVector.
     */
    @Test
    public void testNormalize() {
        System.out.println("normalize");
        RealVector instance = vect1;
        MathContext context = new MathContext(10, RoundingMode.HALF_UP);
        instance.setMathContext(context);
        System.out.println("Done initializing MathContext on vector");
        Vector<RealType> result = instance.normalize();
        assertEquals(5L, result.length());
        RealImpl expValue = new RealImpl(BigDecimal.ONE);
        expValue.setMathContext(context);
        assertEquals(expValue, result.magnitude());
    }
    
}
