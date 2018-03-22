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
package tungsten.types.set.impl;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import tungsten.types.Numeric;
import tungsten.types.Set;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.impl.IntegerImpl;


/**
 *
 * @author tarquin
 */
public class ParametricSetTest {
    ParametricSet<IntegerType> even;
    ParametricSet<IntegerType> odd;
    
    public ParametricSetTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        IntegerType zero = new IntegerImpl(BigInteger.ZERO);
        IntegerType one = new IntegerImpl(BigInteger.ONE);
        IntegerType two = new IntegerImpl(BigInteger.valueOf(2L));
        even = new ParametricSet<IntegerType>(zero, x -> (IntegerType) x.add(two));
        odd = new ParametricSet<IntegerType>(one, x -> (IntegerType) x.add(two));
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of cardinality method, of class ParametricSet.
     */
    @Test
    public void testCardinality() {
        System.out.println("cardinality");
        ParametricSet<IntegerType> instance = even;
        long expResult = -1L;
        long result = instance.cardinality();
        assertEquals(expResult, result);
    }

    /**
     * Test of countable method, of class ParametricSet.
     */
    @Test
    public void testCountable() {
        System.out.println("countable");
        ParametricSet<IntegerType> instance = odd;
        boolean expResult = true;
        boolean result = instance.countable();
        assertEquals(expResult, result);
    }

    /**
     * Test of contains method, of class ParametricSet.
     */
    @Test
    public void testContains() {
        System.out.println("contains");
        Numeric element = new IntegerImpl(BigInteger.valueOf(3L));
        ParametricSet<IntegerType> instance = even;
        boolean expResult = false;
        boolean result = instance.contains(element);
        assertEquals(expResult, result);
        
        instance = odd;
        expResult = true;
        result = instance.contains(element);
        assertEquals(expResult, result);
    }

    /**
     * Test of monotonicity method, of class ParametricSet.
     */
    @Test
    public void testMonotonicity() {
        System.out.println("monotonicity");
        ParametricSet<IntegerType> instance = odd;
        int expResult = 1;
        int result = instance.monotonicity();
        assertEquals(expResult, result);
    }

    /**
     * Test of union method, of class ParametricSet.
     */
    @Test
    public void testUnion() {
        System.out.println("union");
        ParametricSet<IntegerType> instance = (ParametricSet<IntegerType>) even.union(odd);
        List<IntegerType> result = instance.stream().limit(10L).collect(Collectors.toList());
        int[] expValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        List<IntegerType> expResult = Arrays.stream(expValues).mapToObj(x -> BigInteger.valueOf(x)).map(IntegerImpl::new).collect(Collectors.toList());
        assertEquals(expResult, result);
    }

    /**
     * Test of intersection method, of class ParametricSet.
     */
    @Test
    public void testIntersection() {
        System.out.println("intersection");
        ParametricSet<IntegerType> instance = odd;
        int[] testValues = {3, 4, 5};
        Collection<Numeric> valSet = Arrays.stream(testValues).mapToObj(x -> BigInteger.valueOf(x)).map(IntegerImpl::new).collect(Collectors.toSet());
        NumericSet rhs = new NumericSet(valSet);
        try {
            Set<IntegerType> result = instance.intersection(rhs.coerceTo(IntegerType.class));
//            assertFalse(result.contains(new IntegerImpl("6")));
            assertTrue(result.contains(new IntegerImpl("3")));
            assertTrue(result.contains(new IntegerImpl("5")));
            assertFalse(result.contains(new IntegerImpl("4")));
            assertFalse(result.contains(new IntegerImpl("1")));
            assertFalse(result.contains(new IntegerImpl("7")));
        } catch (CoercionException ex) {
            fail("Ran into a coercion problem!");
        }
    }

    /**
     * Test of difference method, of class ParametricSet.
     */
    @Test
    public void testDifference() {
        System.out.println("difference");
        ParametricSet<IntegerType> instance = odd;
//        int count = 0;
//        for (IntegerType val : instance) {
//            System.out.println(val);
//            if (count++ > 5) break;
//        }
        int[] testValues = {3, 4, 5};
        Collection<Numeric> valSet = Arrays.stream(testValues).mapToObj(x -> BigInteger.valueOf(x)).map(IntegerImpl::new).collect(Collectors.toSet());
        NumericSet rhs = new NumericSet(valSet);
        try {
            Set<IntegerType> result = instance.difference(rhs.coerceTo(IntegerType.class));
//            count = 0;
//            for (IntegerType val : result) {
//                System.out.println("Result element " + val);
//                if (count++ > 7) break;
//            }
            assertFalse(result.contains(new IntegerImpl("3")));
            assertFalse(result.contains(new IntegerImpl("5")));
            assertFalse(result.contains(new IntegerImpl("4")));
            assertTrue(result.contains(new IntegerImpl("1")));
            assertTrue(result.contains(new IntegerImpl("7")));
        } catch (CoercionException ex) {
            fail("Ran into a coercion problem!");
        }
    }
}
