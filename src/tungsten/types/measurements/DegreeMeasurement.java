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
package tungsten.types.measurements;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import tungsten.types.Measurement;
import tungsten.types.Numeric;
import tungsten.types.exceptions.CoercionException;
import tungsten.types.numerics.IntegerType;
import tungsten.types.numerics.RealType;
import tungsten.types.numerics.impl.RealImpl;
import tungsten.types.units.impl.Degree;
import tungsten.types.util.MathUtils;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class DegreeMeasurement extends Measurement {
    public class DMSTuple {
        private IntegerType degrees;
        private IntegerType minutes;
        private RealType seconds;

        public DMSTuple(IntegerType degrees, IntegerType minutes, Numeric seconds) {
            try {
                this.degrees = degrees;
                this.minutes = minutes;
                this.seconds = (RealType) seconds.coerceTo(RealType.class);
            } catch (CoercionException ex) {
                Logger.getLogger(DegreeMeasurement.class.getName()).log(Level.SEVERE, "Could not construct DMSTuple.", ex);
                throw new ArithmeticException("DegreeMeasurement.DMSTuple failed construction.");
            }
        }

        public IntegerType getDegrees() {
            return degrees;
        }

        public IntegerType getMinutes() {
            return minutes;
        }

        public RealType getSeconds() {
            return seconds;
        }
        
        public RealType asDecimalDegrees() {
            final BigDecimal MINUTES_PER_DEGREE = new BigDecimal("60", mctx);
            final BigDecimal SECONDS_PER_DEGREE = new BigDecimal("3600", mctx);
            BigDecimal decDegrees = new BigDecimal(degrees.asBigInteger(), mctx);
            BigDecimal decMinutes = new BigDecimal(minutes.asBigInteger(), mctx);
            BigDecimal decSeconds = seconds.asBigDecimal();
            decDegrees = decDegrees.add(decMinutes.divide(MINUTES_PER_DEGREE, mctx), mctx);
            decDegrees = decDegrees.add(decSeconds.divide(SECONDS_PER_DEGREE, mctx), mctx);
            RealImpl result = new RealImpl(decDegrees, true);
            result.setMathContext(mctx);
            return result;
        }
    }
    
    private DMSTuple tuple;
    
    public DegreeMeasurement(IntegerType degrees, IntegerType minutes, Numeric seconds) {
        super(Degree.getInstance());
        tuple = new DMSTuple(degrees, minutes, seconds);
        mctx = MathUtils.inferMathContext(Arrays.asList(degrees, minutes, seconds));
    }
    
    @Override
    public RealType getValue() {
        return tuple.asDecimalDegrees();
    }
    
    public DMSTuple getValueAsTuple() {
        return tuple;
    }
    
    @Override
    public String toString() {
        // emit in DMS notation
        StringBuilder buf = new StringBuilder();
        // 0x2009 is a thinspace; 0x2032 and 0x2033 are prime and double-prime, respectively
        buf.append(tuple.getDegrees()).append(getUnit().unitSymbol()).append("\u2009");
        buf.append(tuple.getMinutes()).append("\u2032\u2009");
        buf.append(tuple.getSeconds()).append("\u2033");
        return buf.toString();
    }
}
