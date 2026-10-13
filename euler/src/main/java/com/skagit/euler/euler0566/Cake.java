package com.skagit.euler.euler0566;

import java.util.ArrayList;
import java.util.Scanner;

public class Cake {
    final private static double _Eps = 1.0e-10;

    private static class Cell {
	private double _start;
	private boolean _pink;
	private Cell _nxt;

	public Cell(final double start, final boolean pink) {
	    _start = start;
	    _pink = pink;
	    _nxt = null;
	}

	private double computeLength() {
	    if (_nxt == this) {
		return 1d;
	    }
	    final double nxtStart = _nxt._start;
	    if (nxtStart >= _start) {
		return nxtStart - _start;
	    }
	    return (1d - _start) + nxtStart;
	}

	public String getString() {
	    final double nxtStart = this == _nxt ? (_start + 1d) : _nxt._start;
	    return String.format("[%.3f:%.3f(%.3f)],%s", //
		    _start, nxtStart, computeLength(), _pink ? "PINK" : "GRAY");
	}

	@Override
	public String toString() {
	    return getString();
	}
    }

    private Cell _current;

    public Cake() {
	_current = new Cell(/* start= */0d, /* pink= */true);
	_current._nxt = _current;
    }

    private void reset() {
	_current._start = 0d;
	_current._nxt = _current;
    }

    private Cell getContainingCell(final double d) {
	for (Cell c = _current;; c = c._nxt) {
	    final double cStart = c._start;
	    if (isWithinEps(cStart, d)) {
		return c;
	    }
	    final Cell nxt = c._nxt;
	    final double nxtStart = nxt._start;
	    if (isWithinEps(nxtStart, d)) {
		return nxt;
	    }
	    if (cStart < nxtStart && (d < cStart || d > nxtStart)) {
		continue;
	    } else if (nxtStart < cStart && (nxtStart < d && d < cStart)) {
		continue;
	    }
	    final Cell newC = new Cell(d, c._pink);
	    newC._nxt = nxt;
	    c._nxt = newC;
	    return newC;
	}
    }

    public int countFlips(final double[] lengths) {
	reset();
	final int nLengths = lengths.length;
	double start = 0d;
	for (int nFlips = 0;; ++nFlips) {
	    /** Find startC. */
	    final Cell startC = getContainingCell(start);
	    start = startC._start;
	    _current = startC;
	    /** Find firstNotToFlipC. */
	    final double length = lengths[nFlips % nLengths];
	    double end = (start + length) % 1d;
	    final Cell firstNotToFlipC = getContainingCell(end);
	    end = firstNotToFlipC._start;
	    _current = firstNotToFlipC;

	    final ArrayList<Double> lengthsList = new ArrayList<>();
	    final ArrayList<Boolean> pinksList = new ArrayList<>();
	    for (Cell c = startC; c != firstNotToFlipC; c = c._nxt) {
		lengthsList.add(c.computeLength());
		pinksList.add(c._pink);
	    }
	    final int nToFlip = lengthsList.size();
	    int k = 0;
	    for (Cell c = startC; c != firstNotToFlipC; c = c._nxt) {
		final int kX = nToFlip - 1 - k;
		c._pink = !pinksList.get(kX);
		final double lengthX = lengthsList.get(kX);
		if (k < nToFlip - 1) {
		    c._nxt._start = (c._start + lengthX) % 1d;
		}
		++k;
	    }
	    /** Check if we're done. */
	    if (_current._pink) {
		boolean fail = false;
		for (Cell c = _current._nxt; c != _current; c = c._nxt) {
		    if (c._pink != _current._pink) {
			fail = true;
			break;
		    }
		}
		if (!fail) {
		    return nFlips + 1;
		}
	    }
	    start = end;
	}
    }

    public int f(final int a, final int b, final int c) {
	final double[] lengths = new double[] { 1d / a, 1d / b, 1d / Math.sqrt(c) };
	return countFlips(lengths);
    }

    public int g(final int n) {
	int nFlips = 0;
	for (int a = 9; a <= n - 2; ++a) {
	    for (int b = a + 1; b <= n - 1; ++b) {
		for (int c = b + 1; c <= n; ++c) {
		    final int nFlips0 = f(a, b, c);
		    nFlips += nFlips0;
		    System.out.printf("\n\t[a,b,c][%d,%d,%d]: [%d/%d]", //
			    a, b, c, nFlips0, nFlips);
		}
	    }
	}
	System.out.println();
	return nFlips;
    }

    private static boolean isWithinEps(double d0, double d1) {
	if (d0 > d1) {
	    final double d = d0;
	    d0 = d1;
	    d1 = d;
	}
	return (d1 - d0 <= _Eps) || (1d - d1 + d0 <= _Eps);
    }

    public String getString() {
	final String f = "  %d. %s\n";
	String s = String.format(f, 0, _current.getString());
	int k = 1;
	for (Cell c = _current._nxt; c != _current; c = c._nxt) {
	    s += String.format(f, k++, c.getString());
	}
	return s;
    }

    @Override
    public String toString() {
	return getString();
    }

    public static void main(final String[] args) {
	final String[] cannedStrings = { "Print", //
		"", //
		"", //
		"", //
	};
	cannedStrings[1] = String.format("F %d %d %d", 15, 16, 17);
	cannedStrings[2] = String.format("G %d", 13);
	cannedStrings[3] = //
		String.format("Cycle %.12f %.12f %.12f", //
			1d / 12d, 1d / 13d, Math.sqrt(1d / 14d));
	final int nCanned = cannedStrings.length;
	final Cake cake = new Cake();
	try (final Scanner sc = new Scanner(System.in)) {
	    for (int iTest = 0;; ++iTest) {
		int nFlips = -1;
		if (iTest > 0) {
		    System.out.println();
		}
		final String s;
		if (iTest < nCanned) {
		    s = cannedStrings[iTest].toUpperCase();
		} else {
		    System.out.printf("%d. Enter BLACK(uit), " + //
			    " P(rint)," + //
			    " CY(cle) <len1> <len2> ..." + //
			    " F <a> <b> <c>", //
			    " G <n>", //
			    iTest);
		    s = sc.nextLine().toUpperCase();
		}
		final long millis = System.currentTimeMillis();
		if (s.startsWith("BLACK")) {
		    break;
		} else if (s.startsWith("P")) {
		} else {
		    final String[] fields = s.trim().split("[\\s,\\[\\]]+");
		    final int nFields = fields.length;
		    if (s.startsWith("F")) {
			final int a = Integer.parseInt(fields[1]);
			final int b = Integer.parseInt(fields[2]);
			final int c = Integer.parseInt(fields[3]);
			nFlips = cake.f(a, b, c);
		    } else if (s.startsWith("G")) {
			final int n = Integer.parseInt(fields[1]);
			nFlips = cake.g(n);
		    } else if (s.startsWith("CY")) {
			final ArrayList<Double> lengthsList = new ArrayList<Double>();
			for (int k = 1; k < nFields; ++k) {
			    try {
				lengthsList.add(Double.parseDouble(fields[k]));
			    } catch (final NumberFormatException e) {
			    }
			}
			final int nLengths = lengthsList.size();
			final double[] lengths = new double[nLengths];
			for (int k = 0; k < nLengths; ++k) {
			    lengths[k] = lengthsList.get(k);
			}
			nFlips = cake.countFlips(lengths);
		    }
		}
		final double secs = (System.currentTimeMillis() - millis) * 0.001;
		if (nFlips < 0) {
		    System.out.printf("%d(%.3f secs): %s\n%s", iTest, secs, s, cake.getString());
		} else {
		    System.out.printf("%d(%.3f secs): %s\n     nFlips[%d]", iTest, secs, s, nFlips);
		}
	    }
	} catch (final Exception e) {
	}
    }
}
