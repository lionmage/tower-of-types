# tower-of-types
A numeric tower and related types implemented in Java.

This is a small part of a larger project I'm working on.  This library implements type interfaces and implementations using
Java's BigInteger and BigDecimal classes (although future type implementations may rely on other internal representations).
The idea is to create a type system that can work in very high precision for applications in modeling, numerical analysis,
and the implementation of high-level languages.  It's very much a work in progress at this time.

One goal is to implement complex numbers in a relatively clean way, with coercion to and from other types.  Other complex
number libraries exist for Java, but they usually rely on primitives (e.g., float or double) with limited precision, and
many implement complex as the base type with other types subclassing the complex type.  The problem with the latter approach
is that you wind up with integer types with methods that are simply inappropriate for anything but a complex number.  I am
attempting to avoid that and code to interfaces with the philosophy that interfaces trump object oriented inheritance
in utility.
