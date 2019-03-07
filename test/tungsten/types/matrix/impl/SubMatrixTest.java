/*
 * The MIT License
 *
 * Copyright © 2019 Robert Poole <Tarquin.AZ@gmail.com>.
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
package tungsten.types.matrix.impl;

import java.math.BigInteger;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import tungsten.types.Matrix;
import tungsten.types.Numeric;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.vector.impl.ColumnVector;
import tungsten.types.vector.impl.RowVector;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class SubMatrixTest {
    private Matrix<IntegerType> A;
    private Matrix<IntegerType> B;
    
    public SubMatrixTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        A = new BasicMatrix<>(generateRandomSquareMatrix(4));
        B = new BasicMatrix<>(generateRandomSquareMatrix(4));
    }
    
    private IntegerType[][] generateRandomSquareMatrix(int size) {
        IntegerType[][] result = new IntegerType[size][size];
        Random rand = new Random();
        for (IntegerType[] row : result) {
            for (int idx = 0; idx < size; idx++) {
                final BigInteger randVal = BigInteger.valueOf(rand.nextLong() % 133L);
                row[idx] = new IntegerImpl(rand.nextBoolean() ? randVal : randVal.negate());
            }
        }
        return result;
    }
    
    @After
    public void tearDown() {
        A = null;
        B = null;
    }

    /**
     * Test of columns method, of class SubMatrix.
     */
    @Test
    public void testColumns() {
        System.out.println("columns");
        SubMatrix instance = null;
        long expResult = 0L;
        long result = instance.columns();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of rows method, of class SubMatrix.
     */
    @Test
    public void testRows() {
        System.out.println("rows");
        SubMatrix instance = null;
        long expResult = 0L;
        long result = instance.rows();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isUpperTriangular method, of class SubMatrix.
     */
    @Test
    public void testIsUpperTriangular() {
        System.out.println("isUpperTriangular");
        SubMatrix instance = null;
        boolean expResult = false;
        boolean result = instance.isUpperTriangular();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isLowerTriangular method, of class SubMatrix.
     */
    @Test
    public void testIsLowerTriangular() {
        System.out.println("isLowerTriangular");
        SubMatrix instance = null;
        boolean expResult = false;
        boolean result = instance.isLowerTriangular();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of removeRow method, of class SubMatrix.
     */
    @Test
    public void testRemoveRow() {
        System.out.println("removeRow");
        long row = 0L;
        SubMatrix instance = null;
        instance.removeRow(row);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of removeColumm method, of class SubMatrix.
     */
    @Test
    public void testRemoveColumm() {
        System.out.println("removeColumm");
        long column = 0L;
        SubMatrix instance = null;
        instance.removeColumm(column);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of valueAt method, of class SubMatrix.
     */
    @Test
    public void testValueAt() {
        System.out.println("valueAt");
        long row = 0L;
        long column = 0L;
        SubMatrix instance = null;
        Object expResult = null;
        Object result = instance.valueAt(row, column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of determinant method, of class SubMatrix.
     */
    @Test
    public void testDeterminant() {
        System.out.println("determinant");
        SubMatrix instance = null;
        Object expResult = null;
        Object result = instance.determinant();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of inverse method, of class SubMatrix.
     */
    @Test
    public void testInverse() {
        System.out.println("inverse");
        SubMatrix instance = null;
        Matrix<? extends Numeric> expResult = null;
        Matrix<? extends Numeric> result = instance.inverse();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of add method, of class SubMatrix.
     */
    @Test
    public void testAdd() {
        System.out.println("add");
        SubMatrix instance = null;
        Matrix expResult = null;
        Matrix result = instance.add(null);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of multiply method, of class SubMatrix.
     */
    @Test
    public void testMultiply() {
        System.out.println("multiply");
        SubMatrix<IntegerType> instance = new SubMatrix<>(A);
        Matrix<IntegerType> expResult = A.multiply(B);
        Matrix<IntegerType> result = instance.multiply(B);
        assertEquals(expResult, result);
    }

    /**
     * Test of duplicate method, of class SubMatrix.
     */
    @Test
    public void testDuplicate() {
        System.out.println("duplicate");
        SubMatrix instance = null;
        SubMatrix expResult = null;
        SubMatrix result = instance.duplicate();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRow method, of class SubMatrix.
     */
    @Test
    public void testGetRow() {
        System.out.println("getRow");
        long row = 0L;
        SubMatrix instance = null;
        RowVector expResult = null;
        RowVector result = instance.getRow(row);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getColumn method, of class SubMatrix.
     */
    @Test
    public void testGetColumn() {
        System.out.println("getColumn");
        long column = 0L;
        SubMatrix instance = null;
        ColumnVector expResult = null;
        ColumnVector result = instance.getColumn(column);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of equals method, of class SubMatrix.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Object o = null;
        SubMatrix instance = null;
        boolean expResult = false;
        boolean result = instance.equals(o);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hashCode method, of class SubMatrix.
     */
    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        SubMatrix instance = null;
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of scale method, of class SubMatrix.
     */
    @Test
    public void testScale() {
        System.out.println("scale");
        Numeric scaleFactor = null;
        SubMatrix instance = null;
        Matrix expResult = null;
        Matrix result = instance.scale(scaleFactor);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
