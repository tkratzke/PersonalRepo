package com.skagit.linearSystemGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import com.skagit.utils.MyFraction;
import com.skagit.utils.ResourcesLister0;
import com.skagit.utils.StaticUtilities;

class LinearSystemStatics {
	final static long _InitialSeed;

	final static int _LoMult;
	final static int _HiMult;
	final static int _LoNudge;
	final static int _HiNudge;
	final static double _ThresholdForFraction;

	final static Difficulty _InitialDifficulty;
	final static String _EqutnNamesString;
	final static String _VblNamesString;
	final static String[] _RemarksForInfeasible;
	final static String[] _RemarksForSolved;

	static {
		final Class<?> clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		final ArrayList<ResourcesLister0.CpEntryAndRelativePath> cpEntryAndRelativePaths1 = ResourcesLister0
				.getCpEntryAndRelativePaths(pckg, "StartUp.properties", true, true);
		final Properties properties = new Properties();
		Integer initialM = null;
		TypeOfProblem initialTypeOfProblem = null;
		Long initialSeed = null;
		Integer loMult = null;
		Integer hiMult = null;
		Integer loNudge = null;
		Integer hiNudge = null;
		String equtnNamesString = "ABCDEFGH";
		String vblNamesString = "xyzwuvabc";
		Double thresholdForFraction = null;

		final ArrayList<String> forInfeasible = new ArrayList<>();
		final ArrayList<String> forSolved = new ArrayList<>();
		for (final ResourcesLister0.CpEntryAndRelativePath cpEntryAndRelativePath : cpEntryAndRelativePaths1) {
			String relativePath = cpEntryAndRelativePath._relativePath;
			relativePath = "/" + relativePath.replace('\\', '/');
			try (InputStream in = clazz.getResourceAsStream(relativePath)) {
				properties.load(in);
				try {
					initialSeed = Long.parseLong((String) properties.get("initialSeed"));
				} catch (final NumberFormatException e) {
				}
				try {
					loMult = Integer.parseInt((String) properties.get("loMult"));
				} catch (final NumberFormatException e) {
				}
				try {
					hiMult = Integer.parseInt((String) properties.get("hiMult"));
				} catch (final NumberFormatException e) {
				}
				try {
					loNudge = Integer.parseInt((String) properties.get("loNudge"));
				} catch (final NumberFormatException e) {
				}
				try {
					hiNudge = Integer.parseInt((String) properties.get("hiNudge"));
				} catch (final NumberFormatException e) {
				}
				try {
					thresholdForFraction = Double.parseDouble((String) properties.get("thresholdForFraction"));
				} catch (final NumberFormatException e) {
				}
				try {
					initialM = Integer.parseInt((String) properties.get("initialM"));
					initialM = initialM <= 0 ? null : initialM;
				} catch (final NumberFormatException e) {
				}
				initialTypeOfProblem = TypeOfProblem.valueOf(properties.getProperty("initialTypeOfProblem"));

				final String equtnNamesString0 = properties.getProperty("equationNamesString");
				if (equtnNamesString0 != null) {
					equtnNamesString = equtnNamesString0;
				}
				final String vblNamesString0 = properties.getProperty("variableNamesString");
				if (vblNamesString0 != null) {
					vblNamesString = vblNamesString0;
				}
				final String sInfeasible = (String) properties.get("Infeasible");
				if (sInfeasible != null) {
					final String[] fields = sInfeasible.split("\\n+");
					if (fields != null) {
						forInfeasible.addAll(Arrays.asList(fields));
					}
				}
				final String sSolved = (String) properties.get("Solved");
				if (sSolved != null) {
					final String[] fields = sSolved.split("\\n+");
					if (fields != null) {
						forSolved.addAll(Arrays.asList(fields));
					}
				}
			} catch (final IOException e) {
			}
		}
		_InitialSeed = initialSeed == null ? -1L : initialSeed;
		_LoMult = loMult == null ? -2 : loMult;
		_HiMult = hiMult == null ? 8 : hiMult;
		_LoNudge = loNudge == null ? -3 : loNudge;
		_HiNudge = hiNudge == null ? 7 : hiNudge;
		_ThresholdForFraction = (thresholdForFraction != null && thresholdForFraction > 0d) ? thresholdForFraction
				: 1.0e-3;
		if (initialM == null || initialTypeOfProblem == null) {
			initialM = 3;
			initialTypeOfProblem = TypeOfProblem.TRI;
		}
		_InitialDifficulty = new Difficulty(initialM, initialTypeOfProblem);
		_EqutnNamesString = equtnNamesString;
		_VblNamesString = vblNamesString;
		if (forInfeasible.isEmpty()) {
			forInfeasible.add("This is not correct.");
		}
		if (forSolved.isEmpty()) {
			forSolved.add("Finished this problem.");
		}
		_RemarksForInfeasible = forInfeasible.toArray(new String[forInfeasible.size()]);
		_RemarksForSolved = forSolved.toArray(new String[forSolved.size()]);
	}

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
