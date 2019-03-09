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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;
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
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.IntegerImpl;
import tungsten.types.numerics.impl.RationalImpl;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.util.MathUtils;
import tungsten.types.vector.impl.ColumnVector;
import tungsten.types.vector.impl.RowVector;

/**
 * Unit test cases for {@link SubMatrix}.
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class SubMatrixTest {
    private Matrix<IntegerType> A;
    private Matrix<IntegerType> B;
    private Matrix<IntegerType> A1, B1;
    private Matrix<RealType> Ar, Br;
    private Matrix<IntegerType> C;
    private Matrix<IntegerType> D;
    
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
        
        A1 = new BasicMatrix<>(generateRandomSquareMatrix(16));
        B1 = new BasicMatrix<>(generateRandomSquareMatrix(16));
        
        Ar = new BasicMatrix<>(generateRandomSquareRealMatrix(16));
        Br = new BasicMatrix<>(generateRandomSquareRealMatrix(16));
        
        final long[][] unscaled = {{1L, 2L, 3L}, {4L, 7L, 6L}, {8L, 9L, 5L}};
        final long[][] scaled   = {{3L, 6L, 9L}, {12L, 21L, 18L}, {24L, 27L, 15L}};
        C = generateMatrixFromTable(unscaled);
        D = generateMatrixFromTable(scaled);
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
    
    private RealType[][] generateRandomSquareRealMatrix(int size) {
        RealType[][] result = new RealType[size][size];
        Random rand = new Random();
        MathContext mctx = MathContext.DECIMAL64;
        for (RealType[] row : result) {
            for (int idx = 0; idx < size; idx++) {
                final BigInteger randNum = BigInteger.valueOf(rand.nextLong() % 157L);
                final BigInteger randDenom = BigInteger.valueOf(rand.nextInt(99) + 1L); // value between 1 and 100
                RationalImpl randVal = new RationalImpl(randNum, randDenom);
                randVal.setMathContext(mctx);
                final RealImpl realVal = new RealImpl(randVal.asBigDecimal());
                realVal.setMathContext(mctx);
                row[idx] = realVal;
            }
        }
        return result;
    }
    
    private Matrix<IntegerType> generateMatrixFromTable(long[][] table) {
        BasicMatrix<IntegerType> result = new BasicMatrix<>();
        
        for (long[] row : table) {
            result.append(Arrays.stream(row)
                    .mapToObj(val -> new IntegerImpl(BigInteger.valueOf(val)))
                    .toArray(size -> new IntegerType[size]));
        }
        
        return result;
    }
    
    @After
    public void tearDown() {
        A = null;
        B = null;
        A1 = null;  B1 = null;
        Ar = null;  Br = null;
        C = null;   D  = null;
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
    public void testMultiplyInteger() {
        System.out.println("multiply for integers");
        SubMatrix<IntegerType> instance = new SubMatrix<>(A);
        Matrix<IntegerType> expResult = A.multiply(B);
        Matrix<IntegerType> result = instance.multiply(B);
        assertEquals(expResult, result);
        
        // now let's try it with a matrix that's 4x bigger in both dimensions
        instance = new SubMatrix<>(A1);
        long regStart = System.currentTimeMillis();
        expResult = A1.multiply(B1);
        long regDuration = System.currentTimeMillis() - regStart;
        long divStart = System.currentTimeMillis();
        result = instance.multiply(B1);
        long divDuration = System.currentTimeMillis() - divStart;
        assertEquals(expResult, result);
        
        System.out.println("Classic matrix multiply took " + regDuration + "ms.");
        System.out.println("Divide-and-conquer matrix multiply took " + divDuration + "ms.");
    }
    
    @Test
    public void testMultiplyReal() {
        System.out.println("multiply for reals");
        // TODO need to fix this test case -- having problems with low order digits
        // probably need to fix some rounding somewhere...
        SubMatrix<RealType> realInstance = new SubMatrix<>(Ar);
        long regRealStart = System.currentTimeMillis();
        Matrix<RealType> expRealResult = Ar.multiply(Br);
        long regRealDuration = System.currentTimeMillis() - regRealStart;
        long divRealStart = System.currentTimeMillis();
        Matrix<RealType> realResult = realInstance.multiply(Br);
        long divRealDuration = System.currentTimeMillis() - divRealStart;
//        assertEquals(expRealResult, realResult);
        final RealType epsilon = new RealImpl("0.00000000001");
        assertTrue(MathUtils.areEqualToWithin(expRealResult, realResult, epsilon));

        System.out.println("For real matrices, classic took " + regRealDuration + "ms, and divide-and-conquer took " + divRealDuration + "ms.");
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
     * Test of scale method, of class SubMatrix.
     */
    @Test
    public void testScale() {
        System.out.println("scale");
        Numeric scaleFactor = new IntegerImpl(BigInteger.valueOf(3L));
        SubMatrix instance = new SubMatrix(C);
        Matrix expResult = D;
        Matrix result = instance.scale(scaleFactor);
        assertEquals(expResult, result);
    }
    
}
