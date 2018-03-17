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
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.NumericHierarchy;
import tungsten.types.numerics.RationalType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.Sign;

/**
 *
 * @author tarquin
 */
public class IntegerImpl implements IntegerType {
    private boolean exact = true;
    private BigInteger val;
    private static final BigInteger TWO = BigInteger.valueOf(2L);
    private static final BigInteger NINE = BigInteger.valueOf(9L);

    public IntegerImpl(BigInteger initialVal) {
        if (initialVal == null) {
            throw new IllegalArgumentException("Must supply a non-null value");
        }
        val = initialVal;
    }

    public IntegerImpl(String representation) {
        val = new BigInteger(representation);
    }
    
    public IntegerImpl(BigInteger initialVal, boolean exact) {
        this(initialVal);
        this.exact = exact;
    }
    
    public IntegerImpl(String representation, boolean exact) {
        this(representation);
        this.exact = exact;
    }

    @Override
    public IntegerType magnitude() {
        return new IntegerImpl(val.abs());
    }

    @Override
    public IntegerType negate() {
        return new IntegerImpl(val.negate());
    }

    @Override
    public IntegerType modulus(IntegerType divisor) {
        return new IntegerImpl(val.mod(divisor.asBigInteger()));
    }

    @Override
    public boolean isEven() {
        return val.mod(TWO).equals(BigInteger.ZERO);
    }

    @Override
    public boolean isOdd() {
        return !isEven();
    }

    /**
     * Determine if this integer is a perfect square. Negative numbers will
     * always return a false value.
     *
     * @return true if this is a perfect square, false otherwise
     * @see
     * <a href="http://burningmath.blogspot.com/2013/09/how-to-check-if-number-is-perfect-square.html">the
     * general algorithm</a>
     */
    @Override
    public boolean isPerfectSquare() {
        if (val.compareTo(BigInteger.ZERO) < 0) {
            return false;
        }
        int lastDigit = digitAt(0L);
        if (lastDigit == 2 || lastDigit == 3 || lastDigit == 7 || lastDigit == 8) {
            return false;
        }
        // compute the digital root
        IntegerType temp = this;
        do {
            temp = sumDigits(temp);
        } while (temp.asBigInteger().compareTo(NINE) > 0);
        int digroot = temp.asBigInteger().intValueExact();
        if (digroot == 0 || digroot == 1 || digroot == 4 || digroot == 7) {
            // this is a candidate for a perfect square, but the only way
            // to be sure is to take the square root and then square the result,
            // comparing with this value to see if it matches
            IntegerType root = this.sqrt();
            return root.multiply(root).equals(this);
        }
        return false;
    }

    protected IntegerType sumDigits(IntegerType temp) {
        BigInteger sum = BigInteger.ZERO;

        for (long idx = 0L; idx < temp.numberOfDigits(); idx++) {
            sum = sum.add(BigInteger.valueOf((long) temp.digitAt(idx)));
        }
        return new IntegerImpl(sum);
    }

    private long numDigitsCache = -1L;
    private final Lock numDigitsLock = new ReentrantLock();

    /**
     * This is based on an algorithm posted by OldCurmudgeon on StackOverflow.
     * It has been modified to avoid code repetition and for compatibility with
     * small values (e.g., 2 and 3 digit numbers).
     *
     * @return the number of digits in this integral value
     * @see
     * <a href="https://stackoverflow.com/questions/18828377/biginteger-count-the-number-of-decimal-digits-in-a-scalable-method">the
     * original StackOverflow article</a>
     */
    @Override
    public long numberOfDigits() {
        // corner case for zero
        if (val.equals(BigInteger.ZERO)) {
            return 1L;
        }

        numDigitsLock.lock();
        try {
            if (numDigitsCache > 0) {
                return numDigitsCache;
            }

            long digits = 0L;
            BigInteger temp = val.abs();
            int bits;

            // Serious reductions.
            do {
                // calculate bitLength
                bits = temp.bitLength();
                // 4 > log2(10) so we should not reduce it too far.
                int reduce = bits / 4;
                // Divide by 10^reduce
                temp = temp.divide(BigInteger.TEN.pow(reduce));
                // Removed that many decimal digits.
                digits += reduce;
            } while (bits > 4); // Now 4 bits or less - add 1 if necessary.
            // using intValue() instead of intValueExact() so we don't throw
            // any unwanted exceptions
            if (temp.intValue() > 0) {  // original algorithm compared with 9
                digits += 1L;
            }
            numDigitsCache = digits;
            return digits;
        } finally {
            numDigitsLock.unlock();
        }
    }

    @Override
    public int digitAt(long position) throws IndexOutOfBoundsException {
        if (position < 0L) {
            throw new IndexOutOfBoundsException("Negative index is not supported");
        }

        BigInteger[] resultAndRemainder;
        BigInteger temp = val.abs();
        long count = 0L;

        do {
            resultAndRemainder = temp.divideAndRemainder(BigInteger.TEN);
            if (count == position) {
                return resultAndRemainder[1].intValue();
            }
            temp = resultAndRemainder[0];
            count++;
        } while (temp.compareTo(BigInteger.ZERO) != 0);
        // if we fell through here, it means position is not valid
        throw new IndexOutOfBoundsException("Index " + position
                + " exceeds max value " + (count - 1L));
    }
    
    /**
     * Generate a stream of {@link Character}s representing the digits of this
     * integer in base {@code radix}, starting from the least significant digit.
     * @param radix the base of the representation; decimal representation is base 10
     * @return a stream of characters representing this integer value in the given radix, or base
     */
    public Stream<Character> stream(int radix) {
        Stream.Builder<Character> builder = Stream.builder();
        BigInteger[] resultAndRemainder;
        BigInteger temp = val.abs();
        BigInteger bigRadix = radix == 10 ? BigInteger.TEN : BigInteger.valueOf((long) radix);

        do {
            resultAndRemainder = temp.divideAndRemainder(bigRadix);
            builder.accept(Character.forDigit(resultAndRemainder[1].intValue(), radix));
            temp = resultAndRemainder[0];
        } while (temp.compareTo(BigInteger.ZERO) != 0);
        return builder.build();
    }

    @Override
    public Numeric pow(IntegerType exponent) {
        if (exponent.sign() == Sign.NEGATIVE) {
            IntegerType negexp = exponent.negate();
            return new RationalImpl(BigInteger.ONE,
                    val.pow(negexp.asBigInteger().intValueExact()));
        }
        return new IntegerImpl(val.pow(exponent.asBigInteger().intValueExact()));
    }

    @Override
    public BigInteger asBigInteger() {
        return val;
    }

    @Override
    public boolean isExact() {
        return exact;
    }

    @Override
    public boolean isCoercibleTo(Class<? extends Numeric> numtype) {
        NumericHierarchy hval = NumericHierarchy.forNumericType(numtype);
        return hval != null;  // integer can be upconverted to any known type
    }

    @Override
    public Numeric coerceTo(Class<? extends Numeric> numtype) throws CoercionException {
        NumericHierarchy hval = NumericHierarchy.forNumericType(numtype);
        switch (hval) {
            case INTEGER:
                return this;
            case RATIONAL:
                return new RationalImpl(this);
            case REAL:
                return new RealImpl(this);
            case COMPLEX:
                final RealType zero = new RealImpl(BigDecimal.ZERO);
                return new ComplexRectImpl(new RealImpl(this), zero, exact);
            default:
                throw new CoercionException("Cannot coerce integer to specified type",
                        this.getClass(), numtype);
        }
    }

    @Override
    public Numeric add(Numeric addend) {
        if (addend instanceof IntegerType) {
            IntegerType that = (IntegerType) addend;
            return new IntegerImpl(this.asBigInteger().add(that.asBigInteger()));
        } else {
            try {
                return this.coerceTo(addend.getClass()).add(addend);
            } catch (CoercionException ex) {
                Logger.getLogger(IntegerImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during integer add.", ex);
            }
        }
        throw new UnsupportedOperationException("Addition operation unsupported");
    }

    @Override
    public Numeric subtract(Numeric subtrahend) {
        return this.add(subtrahend.negate());
    }

    @Override
    public Numeric multiply(Numeric multiplier) {
        if (multiplier instanceof IntegerType) {
            IntegerType that = (IntegerType) multiplier;
            return new IntegerImpl(this.asBigInteger().multiply(that.asBigInteger()));
        } else if (multiplier instanceof RationalType) {
            RationalType that = (RationalType) multiplier;
            BigInteger numResult = val.multiply(that.numerator().asBigInteger());
            BigInteger denomResult = that.denominator().asBigInteger();
            final BigInteger gcd = numResult.gcd(denomResult);
            if (gcd.equals(denomResult)) {
                // reducing would give a denominator of 1, so result is an integer
                return new IntegerImpl(numResult.divide(gcd));
            } else {
                return new RationalImpl(numResult, denomResult).reduce();
            }
        } else {
            try {
                this.coerceTo(multiplier.getClass()).multiply(multiplier);
            } catch (CoercionException ex) {
                Logger.getLogger(IntegerImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during integer multiply.", ex);
            }
        }
        throw new UnsupportedOperationException("Subtraction operation unsupported.");
    }

    @Override
    public Numeric divide(Numeric divisor) {
        if (divisor instanceof IntegerType) {
            IntegerType that = (IntegerType) divisor;
            BigInteger[] resultAndRemainder = val.divideAndRemainder(that.asBigInteger());
            // if the remainder is 0, we can return an integer
            if (resultAndRemainder[1].equals(BigInteger.ZERO)) {
                return new IntegerImpl(resultAndRemainder[0]);
            } else {
                return new RationalImpl(val, that.asBigInteger()).reduce();
            }
        } else {
            try {
                this.coerceTo(divisor.getClass()).divide(divisor);
            } catch (CoercionException ex) {
                Logger.getLogger(IntegerImpl.class.getName()).log(Level.SEVERE, "Failed to coerce type during integer divide.", ex);
            }
        }
        throw new UnsupportedOperationException("Division operation unsupported.");
    }

    @Override
    public Numeric inverse() {
        if (val.equals(BigInteger.ONE)) {
            // 1 is its own inverse
            return this;
        }
        return new RationalImpl(BigInteger.ONE, val);
    }

    /**
     * This computes an integer square root.  Thus, it will only give
     * exact results for perfect squares.  If you want a closer decimal
     * approximation, try using {@link #coerceTo(java.lang.Class) } with
     * a type argument of {@link RealType}.
     * This algorithm was presented by Edward Falk on StackOverflow, with
     * only minor corrections by me.
     * @return an integer approximation of a square root
     * @see <a href="https://stackoverflow.com/questions/4407839/how-can-i-find-the-square-root-of-a-java-biginteger">the StackOverflow article</a>
     */
    @Override
    public IntegerType sqrt() {
        if (val.signum() < 0) {
            throw new ArithmeticException("Cannot obtain square root of negative integers");
        }
        BigInteger div = BigInteger.ZERO.setBit(val.bitLength()/2);
        BigInteger div2 = div;
        // Loop until we hit the same value twice in a row, or wind
        // up alternating.
        while (true) {
            BigInteger y = div.add(val.divide(div)).shiftRight(1);
            if (y.equals(div) || y.equals(div2)) {
                BigInteger lowest = div.min(div2);
                boolean exactness = exact && lowest.multiply(lowest).equals(this.asBigInteger());
                // original algorithm returned y
                return new IntegerImpl(lowest, exactness);
            }
            div2 = div;
            div = y;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IntegerType) {
            IntegerType that = (IntegerType) other;
            if (this.isExact() != that.isExact()) {
                return false;
            }
            return this.asBigInteger().equals(that.asBigInteger());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.exact ? 1 : 0);
        hash = 59 * hash + Objects.hashCode(this.val);
        return hash;
    }

    @Override
    public int compareTo(IntegerType o) {
        return this.val.compareTo(o.asBigInteger());
    }

    @Override
    public Sign sign() {
        return Sign.fromValue(val);
    }

    @Override
    public MathContext getMathContext() {
        return MathContext.UNLIMITED;
    }
    
    @Override
    public String toString() {
        return val.toString();
    }
}
