package com.skagit.linearSystemGenerator;

import com.skagit.utils.MyFraction;

class LinearSystemStatics {
	final private static String _EqutnNamesString = "ABCDEFGHIJKLMN";
	final private static String _VblNamesString = "xyzwuvpqrstabc";
	final static double _ThresholdForFraction = 1.0e-4;

	static String getNominalEqutnName(final int m, final int i) {
		return getNominalName(_EqutnNamesString, m, i);
	}

	static String getNominalVblName(final int n, final int j) {
		return getNominalName(_VblNamesString, n, j);
	}

	static String[] getNominalEqutnNames(final int m) {
		final String[] equtnNames = new String[m];
		for (int i = 0; i < m; ++i) {
			equtnNames[i] = getNominalEqutnName(m, i);
		}
		return equtnNames;
	}

	static String[] getNominalVblNames(final int n) {
		final String[] vblNames = new String[n];
		for (int j = 0; j < n; ++j) {
			vblNames[j] = getNominalVblName(n, j);
		}
		return vblNames;
	}

	static String getNominalName(final String names, final int m, final int i) {
		/** It is assumed that i < m. */
		final int nAtoms = names.length();
		if (m <= nAtoms) {
			return String.valueOf(names.charAt(i));
		}
		final int k0 = i / nAtoms;
		final int k1 = i % nAtoms;
		return String.valueOf(names.charAt(k0)) + String.valueOf(names.charAt(k1));
	}

	static double[][] toDouble(final MyFraction[][] a) {
		final int m = a.length;
		final double[][] d = new double[m][];
		for (int i = 0; i < m; ++i) {
			d[i] = toDouble(a[i]);
		}
		return d;
	}

	static double[] toDouble(final MyFraction[] a) {
		final int n = a.length;
		final double[] d = new double[n];
		for (int j = 0; j < n; ++j) {
			d[j] = a[j].toDouble();
		}
		return d;
	}

	public static MyFraction[][] toFractions(final double[][] d) {
		final int m = d.length;
		final MyFraction[][] a = new MyFraction[m][];
		for (int i = 0; i < m; ++i) {
			a[i] = toFractions(d[i]);
		}
		return a;
	}

	public static MyFraction[] toFractions(final double[] d) {
		final int n = d.length;
		final MyFraction[] a = new MyFraction[n];
		for (int j = 0; j < n; ++j) {
			a[j] = new MyFraction(d[j], _ThresholdForFraction);
		}
		return a;
	}

}
