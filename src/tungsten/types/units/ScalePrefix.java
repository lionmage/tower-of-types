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
package tungsten.types.units;

import java.math.BigDecimal;

/**
 * Encapsulates standard unit scale prefixes.  Per Wikipedia, these are:
 *  yotta       Y                               10^24
 *  zetta       Z                               10^21
 *  exa 	E 	1000000000000000000 	10^18
    peta 	P 	1000000000000000 	10^15
    tera 	T 	1000000000000           10^12
    giga 	G 	1000000000              10^9
    mega 	M 	1000000                 10^6
    kilo 	k 	1000                    10^3
    hecto 	h 	100                     10^2
    deca 	da 	10                      10^1
    (none) 	(none) 	1                       10^0
    deci 	d 	0.1                     10^−1
    centi 	c 	0.01                    10^−2
    milli 	m 	0.001                   10^−3
    micro 	μ 	0.000001                10^−6
    nano 	n 	0.000000001             10^−9
    pico 	p 	0.000000000001          10^−12
    femto 	f 	0.000000000000001 	10^−15
    atto 	a 	0.000000000000000001 	10^−18
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public enum ScalePrefix {
    YOTTA("yotta", "Y", new BigDecimal("1E24")),
    ZETTA("zetta", "Z", new BigDecimal("1E21")),
    EXA("exa", "E", new BigDecimal("1E18")),
    PETA("peta", "P", new BigDecimal("1E15")),
    TERA("tera", "T", new BigDecimal("1E12")),
    GIGA("giga", "G", new BigDecimal("1E9")),
    MEGA("mega", "M", new BigDecimal("1E6")),
    KILO("kilo", "k", new BigDecimal("1000")),
    HECTO("hecto", "h", new BigDecimal("100")),
    DECA("deca", "da", BigDecimal.TEN),
    DECI("deci", "d", new BigDecimal("0.1")),
    CENTI("centi", "c", new BigDecimal("0.01")),
    MILLI("milli", "m", new BigDecimal("0.001")),
    MICRO("micro", "\u00B5", new BigDecimal("0.000001")),
    NANO("nano", "n", new BigDecimal("1E-9")),
    PICO("pico", "p", new BigDecimal("1E-12")),
    FEMTO("femto", "f", new BigDecimal("1E-15")),
    ATTO("atto", "a", new BigDecimal("1E-18"));
    
    private String name, symbol;
    private BigDecimal scale;
    
    private ScalePrefix(String name, String symbol, BigDecimal scale) {
        this.name = name;
        this.symbol = symbol;
        this.scale = scale;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getScale() {
        return scale;
    }
    
    @Override
    public String toString() {
        return getSymbol();
    }
}
