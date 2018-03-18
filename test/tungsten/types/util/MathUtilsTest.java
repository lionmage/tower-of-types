/*
 * The MIT License
 *
 * Copyright 2018 tarquin.
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

import java.math.MathContext;
import java.math.RoundingMode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;
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
    
}
