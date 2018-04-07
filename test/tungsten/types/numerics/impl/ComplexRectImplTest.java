/*
 * The MIT License
 *
 * Copyright 2018 Robert Poole <Tarquin.AZ@gmail.com>.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import tungsten.types.Numeric;
import tungsten.types.Set;
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class ComplexRectImplTest {
    
    public ComplexRectImplTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of magnitude method, of class ComplexRectImpl.
     */
    @Test
    public void testMagnitude() {
        System.out.println("magnitude");
        RealType one = new RealImpl(BigDecimal.ONE);
        ComplexRectImpl instance = new ComplexRectImpl(one, one);
        instance.setMathContext(MathContext.DECIMAL64);
        RealImpl two = new RealImpl("2.0");
        two.setMathContext(MathContext.DECIMAL64);
        RealType expResult = (RealType) two.sqrt();
        RealType result = instance.magnitude();
        assertEquals(expResult, result);
    }

    /**
     * Test of negate method, of class ComplexRectImpl.
     */
    @Test
    public void testNegate() {
        System.out.println("negate");
        ComplexRectImpl instance = new ComplexRectImpl(new RealImpl("-3"), new RealImpl("+5.6"));
        ComplexType expResult = new ComplexRectImpl(new RealImpl("3"), new RealImpl("-5.6"));
        ComplexType result = instance.negate();
        assertEquals(expResult, result);
    }

    /**
     * Test of conjugate method, of class ComplexRectImpl.
     */
    @Test
    public void testConjugate() {
        System.out.println("conjugate");
        ComplexRectImpl instance = null;
        ComplexType expResult = null;
        ComplexType result = instance.conjugate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of real method, of class ComplexRectImpl.
     */
    @Test
    public void testReal() {
        System.out.println("real");
        ComplexRectImpl instance = null;
        RealType expResult = null;
        RealType result = instance.real();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of imaginary method, of class ComplexRectImpl.
     */
    @Test
    public void testImaginary() {
        System.out.println("imaginary");
        ComplexRectImpl instance = null;
        RealType expResult = null;
        RealType result = instance.imaginary();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of argument method, of class ComplexRectImpl.
     */
    @Test
    public void testArgument() {
        System.out.println("argument");
        ComplexRectImpl instance = null;
        RealType expResult = null;
        RealType result = instance.argument();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isExact method, of class ComplexRectImpl.
     */
    @Test
    public void testIsExact() {
        System.out.println("isExact");
        ComplexRectImpl instance = null;
        boolean expResult = false;
        boolean result = instance.isExact();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isCoercibleTo method, of class ComplexRectImpl.
     */
    @Test
    public void testIsCoercibleTo() {
        System.out.println("isCoercibleTo");
        Class<? extends Numeric> numtype = null;
        ComplexRectImpl instance = null;
        boolean expResult = false;
        boolean result = instance.isCoercibleTo(numtype);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of coerceTo method, of class ComplexRectImpl.
     */
    @Test
    public void testCoerceTo() throws Exception {
        System.out.println("coerceTo");
        Class<? extends Numeric> numtype = null;
        ComplexRectImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.coerceTo(numtype);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of add method, of class ComplexRectImpl.
     */
    @Test
    public void testAdd() {
        System.out.println("add");
        Numeric addend = null;
        ComplexRectImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.add(addend);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of subtract method, of class ComplexRectImpl.
     */
    @Test
    public void testSubtract() {
        System.out.println("subtract");
        Numeric subtrahend = null;
        ComplexRectImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.subtract(subtrahend);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of multiply method, of class ComplexRectImpl.
     */
    @Test
    public void testMultiply() {
        System.out.println("multiply");
        Numeric multiplier = null;
        ComplexRectImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.multiply(multiplier);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of divide method, of class ComplexRectImpl.
     */
    @Test
    public void testDivide() {
        System.out.println("divide");
        Numeric divisor = null;
        ComplexRectImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.divide(divisor);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of inverse method, of class ComplexRectImpl.
     */
    @Test
    public void testInverse() {
        System.out.println("inverse");
        ComplexRectImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.inverse();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sqrt method, of class ComplexRectImpl.
     */
    @Test
    public void testSqrt() {
        System.out.println("sqrt");
        ComplexRectImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.sqrt();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of nthRoots method, of class ComplexRectImpl.
     */
    @Test
    public void testNthRoots_IntegerType() {
        System.out.println("nthRoots");
        IntegerType n = null;
        ComplexRectImpl instance = null;
        Set<ComplexType> expResult = null;
        Set<ComplexType> result = instance.nthRoots(n);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of nthRoots method, of class ComplexRectImpl.
     */
    @Test
    public void testNthRoots_long() {
        System.out.println("nthRoots");
        long n = 0L;
        ComplexRectImpl instance = null;
        Set<ComplexType> expResult = null;
        Set<ComplexType> result = instance.nthRoots(n);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of equals method, of class ComplexRectImpl.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Object o = null;
        ComplexRectImpl instance = null;
        boolean expResult = false;
        boolean result = instance.equals(o);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hashCode method, of class ComplexRectImpl.
     */
    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        ComplexRectImpl instance = null;
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class ComplexRectImpl.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        ComplexRectImpl instance = null;
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMathContext method, of class ComplexRectImpl.
     */
    @Test
    public void testGetMathContext() {
        System.out.println("getMathContext");
        ComplexRectImpl instance = new ComplexRectImpl(new RealImpl("4"), new RealImpl("3"));
        instance.setMathContext(MathContext.DECIMAL128);
        MathContext expResult = MathContext.DECIMAL128;
        MathContext result = instance.getMathContext();
        assertEquals(expResult, result);
        // real and imaginary parts should share the same MathContext
        // when complex numbers are constructed this way
        assertEquals(expResult, instance.real().getMathContext());
        assertEquals(expResult, instance.imaginary().getMathContext());
    }
}
