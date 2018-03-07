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
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import tungsten.types.Numeric;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.Sign;

/**
 *
 * @author tarquin
 */
public class RealImplTest {
    
    public RealImplTest() {
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
     * Test of setIrrational method, of class RealImpl.
     */
    @Test
    public void testSetIrrational() {
        System.out.println("setIrrational");
        boolean irrational = false;
        RealImpl instance = null;
        instance.setIrrational(irrational);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMathContext method, of class RealImpl.
     */
    @Test
    public void testSetMathContext() {
        System.out.println("setMathContext");
        MathContext mctx = null;
        RealImpl instance = null;
        instance.setMathContext(mctx);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isIrrational method, of class RealImpl.
     */
    @Test
    public void testIsIrrational() {
        System.out.println("isIrrational");
        RealImpl instance = null;
        boolean expResult = false;
        boolean result = instance.isIrrational();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of magnitude method, of class RealImpl.
     */
    @Test
    public void testMagnitude() {
        System.out.println("magnitude");
        RealImpl instance = null;
        RealType expResult = null;
        RealType result = instance.magnitude();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of negate method, of class RealImpl.
     */
    @Test
    public void testNegate() {
        System.out.println("negate");
        RealImpl instance = null;
        RealType expResult = null;
        RealType result = instance.negate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of asBigDecimal method, of class RealImpl.
     */
    @Test
    public void testAsBigDecimal() {
        System.out.println("asBigDecimal");
        RealImpl instance = null;
        BigDecimal expResult = null;
        BigDecimal result = instance.asBigDecimal();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sign method, of class RealImpl.
     */
    @Test
    public void testSign() {
        System.out.println("sign");
        RealImpl instance = null;
        Sign expResult = null;
        Sign result = instance.sign();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isExact method, of class RealImpl.
     */
    @Test
    public void testIsExact() {
        System.out.println("isExact");
        RealImpl instance = null;
        boolean expResult = false;
        boolean result = instance.isExact();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isCoercibleTo method, of class RealImpl.
     */
    @Test
    public void testIsCoercibleTo() {
        System.out.println("isCoercibleTo");
        Class<? extends Numeric> numtype = null;
        RealImpl instance = null;
        boolean expResult = false;
        boolean result = instance.isCoercibleTo(numtype);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of coerceTo method, of class RealImpl.
     */
    @Test
    public void testCoerceTo() throws Exception {
        System.out.println("coerceTo");
        Class<? extends Numeric> numtype = null;
        RealImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.coerceTo(numtype);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of add method, of class RealImpl.
     */
    @Test
    public void testAdd() {
        System.out.println("add");
        Numeric addend = null;
        RealImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.add(addend);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of subtract method, of class RealImpl.
     */
    @Test
    public void testSubtract() {
        System.out.println("subtract");
        Numeric subtrahend = null;
        RealImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.subtract(subtrahend);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of multiply method, of class RealImpl.
     */
    @Test
    public void testMultiply() {
        System.out.println("multiply");
        Numeric multiplier = null;
        RealImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.multiply(multiplier);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of divide method, of class RealImpl.
     */
    @Test
    public void testDivide() {
        System.out.println("divide");
        Numeric divisor = null;
        RealImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.divide(divisor);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of inverse method, of class RealImpl.
     */
    @Test
    public void testInverse() {
        System.out.println("inverse");
        RealImpl instance = null;
        Numeric expResult = null;
        Numeric result = instance.inverse();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sqrt method, of class RealImpl.
     */
    @Test
    public void testSqrt() {
        System.out.println("sqrt");
        MathContext myctx = new MathContext(10, RoundingMode.HALF_EVEN);
        RealImpl instance = new RealImpl("0.25");
        instance.setMathContext(myctx);
        Numeric expResult = new RealImpl("0.5");
        Numeric result = instance.sqrt();
        System.out.println("Result is " + result);
        assertEquals(expResult, result);
        assertFalse(((RealType) result).isIrrational());
        
        instance = new RealImpl("16.0");
        instance.setMathContext(myctx);
        expResult = new IntegerImpl(BigInteger.valueOf(4L));
        result = instance.sqrt();
        assertEquals(expResult, result);
        
        instance = new RealImpl("2.0");
        instance.setMathContext(myctx);
        result = instance.sqrt();
        System.out.println("Sqrt(2) is " + result);
        assertFalse(result.isExact());
        assertTrue(((RealType) result).isIrrational());
    }

    /**
     * Test of isIntegralValue method, of class RealImpl.
     */
    @Test
    public void testIsIntegralValue() {
        System.out.println("isIntegralValue");
        RealImpl instance = null;
        boolean expResult = false;
        boolean result = instance.isIntegralValue();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
