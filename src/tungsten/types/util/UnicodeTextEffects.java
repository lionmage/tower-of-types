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
package tungsten.types.util;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Robert Poole <Tarquin.AZ@gmail.com>
 */
public class UnicodeTextEffects {
    // Unicode superscript numerals 0 - 9
    private static final String[] superscriptDigits = {
        "\u2070", "\u00B9", "\u00B2", "\u00B3", "\u2074", "\u2075",
        "\u2076", "\u2077", "\u2078", "\u2079"
    };
    // Unicode subscript numerals 0 - 9
    private static final String[] subscriptDigits = {
        "\u2080", "\u2081", "\u2082", "\u2083", "\u2084", "\u2085",
        "\u2086", "\u2087", "\u2088", "\u2089"
    };
    private static final Map<Character, String> superscriptMap;
    private static final Map<Character, String> subscriptMap;
    static {
        superscriptMap = new HashMap<>();
        subscriptMap = new HashMap<>();
        
        superscriptMap.put('-', "\u207B");
        superscriptMap.put('+', "\u207A");
        superscriptMap.put('(', "\u207D");
        superscriptMap.put(')', "\u207E");
        superscriptMap.put('i', "\u2071");
        superscriptMap.put('n', "\u207F");
        superscriptMap.put('=', "\u207C");
        
        subscriptMap.put('-', "\u208B");
        subscriptMap.put('+', "\u208A");
        subscriptMap.put('(', "\u208D");
        subscriptMap.put(')', "\u208E");
        subscriptMap.put('=', "\u208C");
        subscriptMap.put('a', "\u2090");
        subscriptMap.put('e', "\u2091");
        subscriptMap.put('o', "\u2092");
        subscriptMap.put('x', "\u2093");
        subscriptMap.put('k', "\u2096");
        subscriptMap.put('n', "\u2099");
        subscriptMap.put('p', "\u209A");
        subscriptMap.put('t', "\u209C");
    }
    
    public static String numericSuperscript(int n) {
        StringBuilder buf = new StringBuilder();
        int digit;
        int k = Math.abs(n);
        do {
            digit = k % 10;
            k /= 10;
            
            buf.insert(0, superscriptDigits[digit]);
        } while (k != 0);
        if (n < 0) buf.insert(0, superscriptMap.get('-'));
        return buf.toString();
    }

    public static String numericSubscript(int n) {
        StringBuilder buf = new StringBuilder();
        int digit;
        int k = Math.abs(n);
        do {
            digit = k % 10;
            k /= 10;
            
            buf.insert(0, subscriptDigits[digit]);
        } while (k != 0);
        if (n < 0) buf.insert(0, subscriptMap.get('-'));
        return buf.toString();
    }
    
    public static String convertToSuperscript(String source) {
        StringBuilder buf = new StringBuilder();
        
        for (Character c : source.toCharArray()) {
            if (Character.isDigit(c)) {
                int digit = c - '0';
                buf.append(superscriptDigits[digit]);
            } else if (superscriptMap.containsKey(c)) {
                buf.append(superscriptMap.get(c));
            }
        }
        return buf.toString();
    }

    public static String convertToSubscript(String source) {
        StringBuilder buf = new StringBuilder();
        
        for (Character c : source.toCharArray()) {
            if (Character.isDigit(c)) {
                int digit = c - '0';
                buf.append(subscriptDigits[digit]);
            } else if (subscriptMap.containsKey(c)) {
                buf.append(subscriptMap.get(c));
            }
        }
        return buf.toString();
    }
}
