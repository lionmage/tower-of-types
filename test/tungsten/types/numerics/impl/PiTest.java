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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.ComplexType;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RationalType;
import tungsten.types.numerics.RealType;

/**
 *
 * @author tarquin
 */
public class PiTest {
    private static final String pi100 = "3.1415926535 8979323846 2643383279 5028841971 6939937510 5820974944 5923078164 0628620899 8628034825 3421170679";
    
    public PiTest() {
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
     * Test of getInstance method, of class Pi.
     */
    @Test
    public void testGetInstance() {
        System.out.println("getInstance");
        MathContext mctx = new MathContext(8, RoundingMode.HALF_UP);
        BigDecimal expResult = new BigDecimal("3.1415927");
        Pi result = Pi.getInstance(mctx);
        System.out.println("Pi instance: " + result.toString());
        String resultstr = result.asBigDecimal().toPlainString();
        System.out.println("Pi value = " + resultstr);
        assertEquals(0, result.asBigDecimal().compareTo(expResult));
        
        // now test for 100 digits
        mctx = new MathContext(100, RoundingMode.HALF_UP);
        final String piStr = pi100.replaceAll("\\s", "");  // strip whitespace
        System.out.println("pi100 string has " + (piStr.length() - 1) + " digits");
        expResult = new BigDecimal(piStr, mctx);
        result = Pi.getInstance(mctx);
        resultstr = result.asBigDecimal().toPlainString();
        String expResultStr = expResult.toPlainString();
        System.out.println("result string has " + (resultstr.length() - 1) + " digits");
        System.out.println("Testing for " + result.toString());
        System.out.println("Expected ends with:\n" + expResultStr.substring(expResultStr.length() - 20));
        System.out.println("Actual ends with:\n" + resultstr.substring(resultstr.length() - 20));
        assertEquals(0, result.asBigDecimal().compareTo(expResult));
    }

    /**
     * Test of isCoercibleTo method, of class Pi.
     */
    @Test
    public void testIsCoercibleTo() {
        System.out.println("isCoercibleTo");
        Class<? extends Numeric> numtype = RealType.class;
        Pi instance = Pi.getInstance(MathContext.DECIMAL128);
        boolean expResult = true;
        boolean result = instance.isCoercibleTo(numtype);
        assertEquals(expResult, result);
        
        numtype = IntegerType.class;
        expResult = false;
        result = instance.isCoercibleTo(numtype);
        assertEquals(expResult, result);
        
        numtype = ComplexType.class;
        expResult = true;
        result = instance.isCoercibleTo(numtype);
        assertEquals(expResult, result);
    }

    /**
     * Test of coerceTo method, of class Pi.
     */
    @Test
    public void testCoerceTo() throws Exception {
        System.out.println("coerceTo");
        Class<? extends Numeric> numtype = RealType.class;
        Pi instance = Pi.getInstance(MathContext.DECIMAL64);
        Numeric result = instance.coerceTo(numtype);
        assertTrue(result instanceof RealType);
        
        numtype = ComplexType.class;
        result = instance.coerceTo(numtype);
        assertTrue(result instanceof ComplexType);
        
        numtype = RationalType.class;
        try {
            result = instance.coerceTo(numtype);
            fail("Should not be able to coerce Pi to a rational.");
        } catch (Exception e) {
            assertTrue(e instanceof CoercionException);
        }
    }

    /**
     * Test of add method, of class Pi.
     */
    @Test
    public void testAdd() {
        System.out.println("add");
        Numeric addend = null;
        Pi instance = null;
        Numeric expResult = null;
        Numeric result = instance.add(addend);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of subtract method, of class Pi.
     */
    @Test
    public void testSubtract() {
        System.out.println("subtract");
        Numeric subtrahend = null;
        Pi instance = null;
        Numeric expResult = null;
        Numeric result = instance.subtract(subtrahend);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of multiply method, of class Pi.
     */
    @Test
    public void testMultiply() {
        System.out.println("multiply");
        Numeric multiplier = null;
        Pi instance = null;
        Numeric expResult = null;
        Numeric result = instance.multiply(multiplier);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of divide method, of class Pi.
     */
    @Test
    public void testDivide() {
        System.out.println("divide");
        Numeric divisor = null;
        Pi instance = null;
        Numeric expResult = null;
        Numeric result = instance.divide(divisor);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of inverse method, of class Pi.
     */
    @Test
    public void testInverse() {
        System.out.println("inverse");
        Pi instance = null;
        Numeric expResult = null;
        Numeric result = instance.inverse();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sqrt method, of class Pi.
     */
    @Test
    public void testSqrt() {
        System.out.println("sqrt");
        Pi instance = null;
        Numeric expResult = null;
        Numeric result = instance.sqrt();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of numberOfDigits method, of class Pi.
     */
    @Test
    public void testNumberOfDigits() {
        System.out.println("numberOfDigits");
        Pi instance = Pi.getInstance(MathContext.DECIMAL128);
        long expResult = 34L; // number of significant digits for DECIMAL128
        long result = instance.numberOfDigits();
        assertEquals(expResult, result);
    }

    /**
     * Test of equals method, of class Pi.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Object o = null;
        Pi instance = null;
        boolean expResult = false;
        boolean result = instance.equals(o);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of compareTo method, of class Pi.
     */
    @Test
    public void testCompareTo() {
        System.out.println("compareTo");
        RealType three = new RealImpl(BigDecimal.valueOf(3L));
        RealType four = new RealImpl(BigDecimal.valueOf(4L));
        Pi instance = Pi.getInstance(MathContext.DECIMAL128);
        int result = instance.compareTo(three);
        assertTrue(result > 0);
        result = instance.compareTo(four);
        assertTrue(result < 0);
    }

}
