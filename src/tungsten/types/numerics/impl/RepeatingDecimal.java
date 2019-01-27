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
package tungsten.types.numerics.impl;

import java.math.BigInteger;
import java.math.MathContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RationalType;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class RepeatingDecimal extends RationalImpl {
    private static BigInteger TWO  = BigInteger.valueOf(2L);
    private static BigInteger FIVE = BigInteger.valueOf(5L);
    
    private BigInteger position;  // the position after the decimal point where repetition begins
    private BigInteger decimalPeriod;  // the length of the repetition

    public RepeatingDecimal(IntegerType numerator, IntegerType denominator, MathContext mctx) {
        super(numerator, denominator);
        super.setMathContext(mctx);
    }
    
    public RepeatingDecimal(RationalType source, MathContext mctx) {
        super(source.numerator(), source.denominator());
        super.setMathContext(mctx);
    }
    
    private void characterize() {
        final BigInteger denom = denominator().asBigInteger();
        final BigInteger dmod2 = denom.mod(TWO);
        final BigInteger dmod5 = denom.mod(FIVE);

        if (dmod2.equals(BigInteger.ZERO)
                || dmod5.equals(BigInteger.ZERO)) {
            // this should be a finite decimal representation if there are no other prime factors
            BigInteger[] tdenom = removeFactorsOf(denom, TWO);
            BigInteger alpha = tdenom[1];
            tdenom = removeFactorsOf(tdenom[0], FIVE);
            BigInteger beta = tdenom[1];
            Logger.getLogger(RepeatingDecimal.class.getName()).log(Level.FINER,
                    "Denominator {} with factors of 2 and 5 removed: {}",
                    new Object[]{denom, tdenom[0]});
            if (tdenom[0].equals(BigInteger.ONE)) {
                decimalPeriod = BigInteger.ZERO;
                position = BigInteger.valueOf(-1L);
                // There are no factors other than 2 or 5, so the decimal expansion is finite.
                Logger.getLogger(RepeatingDecimal.class.getName()).log(Level.FINER,
                        "Rational value {} has no repeating digits.", super.toString());
            } else {
                // there is repetition after an initial non-repeating string of digits
                decimalPeriod = multiplicativeOrder(BigInteger.TEN, tdenom[0]);
                position = alpha.max(beta);
                Logger.getLogger(RepeatingDecimal.class.getName()).log(Level.FINE,
                        "The cycle starts at position {} and has {} digits.",
                        new Object[]{position, decimalPeriod});
            }
        } else {
            // the denominator is relatively prime (coprime) to 10
            // therefore, the digits start repeating immediately
            decimalPeriod = multiplicativeOrder(BigInteger.TEN, denom);
            // period begins immediately after decimal point (position 0)
            position = BigInteger.ZERO;
        }
    }

    private static BigInteger[] removeFactorsOf(BigInteger denominator, BigInteger factor) {
        long exponent = 0L;
        do {
            BigInteger[] temp = denominator.divideAndRemainder(factor);
            if (!temp[1].equals(BigInteger.ZERO)) {
                break;
            }
            exponent++;
            denominator = temp[0];
        } while (denominator.compareTo(BigInteger.ONE) > 0);
        return new BigInteger[] {denominator, BigInteger.valueOf(exponent)};
    }

    // see http://mathworld.wolfram.com/MultiplicativeOrder.html for definition of this function
    // see http://mathworld.wolfram.com/DecimalPeriod.html for application
    private static BigInteger multiplicativeOrder(BigInteger b, BigInteger n) {
        if (!b.gcd(n).equals(BigInteger.ONE)) {
            Logger.getLogger(RepeatingDecimal.class.getName()).log(Level.INFO,
                    "Cannot compute multiplicative order: the GCD of {} and {} is {}.",
                    new Object[] {b, n, b.gcd(n)});
            // throw an exception because multiplicative order only works if b and n are relatively prime
            throw new ArithmeticException("Multiplicative order only exists for relatively prime arguments.");
        }

        int e = 1;
        while (!b.pow(e).mod(n).equals(BigInteger.ONE)) {
            e++;
        }
        return BigInteger.valueOf((long) e);
    }
}
