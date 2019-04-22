/*
 * The MIT License
 *
 * Copyright © 2018 Robert Poole.
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
import java.math.RoundingMode;
import java.util.Comparator;
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
import tungsten.types.numerics.impl.ComplexRectImpl;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.RealImpl;

/**
 *
 * @author tarquin
 */
public class MathUtilsTest {
    
    public MathUtilsTest() {
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
     * Test of factorial method, of class MathUtils.
     */
    @Test
    public void testFactorial() {
        System.out.println("factorial");
        IntegerType n = new IntegerImpl("5");
        IntegerType expResult = new IntegerImpl("120");
        IntegerType result = MathUtils.factorial(n);
        assertEquals(expResult, result);
        
        n = new IntegerImpl("9");
        expResult = new IntegerImpl("362880");
        result = MathUtils.factorial(n);
        assertEquals(expResult, result);
    }

    /**
     * Test of computeIntegerExponent method, of class MathUtils.
     */
    @Test
    public void testComputeIntegerExponent() {
        System.out.println("computeIntegerExponent");
        RealType x = new RealImpl("3.0");
        int n = 3;
        MathContext mctx = new MathContext(5, RoundingMode.HALF_UP);
        RealType expResult = new RealImpl("27.0");
        RealType result = MathUtils.computeIntegerExponent(x, n, mctx);
        assertEquals(expResult, result);
        
        x = new RealImpl("2.0");
        n = -2;
        expResult = new RealImpl("0.25");
        result = MathUtils.computeIntegerExponent(x, n, mctx);
        assertEquals(expResult, result);
        
        x = new RealImpl("-2.0");
        n = 3;
        expResult = new RealImpl("-8.0");
        result = MathUtils.computeIntegerExponent(x, n, mctx);
        assertEquals(expResult, result);
    }

    /**
     * Test of ln method, of class MathUtils.
     */
    @Test
    public void testLn() {
        System.out.println("ln");
        RealType x = new RealImpl("10.0");
        MathContext mctx = new MathContext(9, RoundingMode.HALF_UP);
        RealType expResult = new RealImpl("2.30258509", false);
        RealType result = MathUtils.ln(x, mctx);
        assertEquals(expResult, result);
        
        x = new RealImpl("0.7");
        expResult = new RealImpl("-0.356674944", false);
        result = MathUtils.ln(x, mctx);
        assertEquals(expResult, result);
        
        x = new RealImpl("25.7");
        expResult = new RealImpl("3.24649099", false);
        result = MathUtils.ln(x, mctx);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testMantissa() {
        System.out.println("mantissa");
        RealType x = new RealImpl("54.789");
        RealType expResult = new RealImpl("5.4789");
        RealType result = MathUtils.mantissa(x);
        assertEquals(expResult, result);
        
        x = new RealImpl("0.00211");
        expResult = new RealImpl("2.11");
        result = MathUtils.mantissa(x);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testExponent() {
        System.out.println("exponent");
        RealType x = new RealImpl("54.789");
        IntegerType expResult = new IntegerImpl("1");
        IntegerType result = MathUtils.exponent(x);
        assertEquals(expResult, result);
        
        x = new RealImpl("0.00211");
        expResult = new IntegerImpl("-3");
        result = MathUtils.exponent(x);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testRootsOfUnity() {
        System.out.println("rootsOfUnity");
        Set<ComplexType> roots = MathUtils.rootsOfUnity(4, MathContext.DECIMAL128);
        assertEquals(4L, roots.cardinality());
        // dump out all the roots
        for (ComplexType root : roots) {
            System.out.println("Root at " + root);
        }
        // the first root should be equal to 1
        ComplexType firstRoot = roots.iterator().next();
        System.out.println("First root is exact? " + firstRoot.isExact());
        assertEquals(new RealImpl(BigDecimal.ONE, false), firstRoot.real());
        assertEquals(new RealImpl(BigDecimal.ZERO), firstRoot.imaginary());
    }
    
    @Test
    public void testComparison() {
        System.out.println("Testing generic Numeric Comparator");
        Comparator<Numeric> c = MathUtils.obtainGenericComparator();
        
        IntegerType a = new IntegerImpl("4");
        IntegerType b = new IntegerImpl("6");
        RealType mid = new RealImpl("5.1");
        
        assertTrue(c.compare(a, b) < 0);
        assertTrue(c.compare(a, mid) < 0);
        assertTrue(c.compare(b, mid) > 0);
    }
    
    @Test(expected = RuntimeException.class)
    public void testNonComparable() {
        System.out.println("Testing generic Numeric Comparator, failure case");
        Comparator<Numeric> c = MathUtils.obtainGenericComparator();
        IntegerType a = new IntegerImpl("4");
        ComplexType cplx = new ComplexRectImpl("-5+8i");
        
        // Complex numbers are not comparable with any other numeric type
        // this should fail
        c.compare(a, cplx);
    }
}
