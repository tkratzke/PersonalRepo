package com.skagit.linearSystemGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import com.skagit.advMath.mitLinearAlgebra.PaqLu;
import com.skagit.advMath.mitLinearAlgebra.RandomMatrixBuilder;
import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.MyFraction;
import com.skagit.utils.NumericalRoutines;
import com.skagit.utils.ResourcesLister;
import com.skagit.utils.StaticUtilities;

class LinearSystemStatics {
	final static long _Seed;

	final static int _LoMult;
	final static int _HiMult;
	final static int _LoNudge;
	final static int _HiNudge;
	final static double _ThresholdForFraction;

	final static String _EqutnNamesString;
	final static String _VblNamesString;
	final static String[] _RemarksForInfeasible;
	final static String[] _RemarksForSolved;

	static {
		final Class<?> clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		final ArrayList<ResourcesLister.ResourceNamePlus> resourceNamePluses = ResourcesLister
				.getRelativeResourceNamePluses(pckg, "StartUp.properties", true, true);
		final Properties properties = new Properties();

		/** Generating linear systems: */
		Long seed = null;
		Integer loMult = null;
		Integer hiMult = null;
		Integer loNudge = null;
		Integer hiNudge = null;

		/** Fraction determination: */
		Double thresholdForFraction = null;

		/** Equation and variable names. */
		String equtnNamesString = null;
		String vblNamesString = null;

		/** The remarks for infeasible and solved problems. */
		final ArrayList<String> forInfeasible = new ArrayList<>();
		final ArrayList<String> forSolved = new ArrayList<>();

		for (final ResourcesLister.ResourceNamePlus resourceNamePlus : resourceNamePluses) {
			String relativePath = resourceNamePlus._relativePath;
			relativePath = "/" + relativePath.replace('\\', '/');
			try (InputStream in = clazz.getResourceAsStream(relativePath)) {
				properties.load(in);
				if (seed == null) {
					try {
						seed = Long.parseLong((String) properties.get("seed"));
					} catch (final NumberFormatException e) {
					}
				}
				if (loMult == null) {
					try {
						loMult = Integer.parseInt((String) properties.get("loMult"));
					} catch (final NumberFormatException e) {
					}
				}
				if (hiMult == null) {
					try {
						hiMult = Integer.parseInt((String) properties.get("hiMult"));
					} catch (final NumberFormatException e) {
					}
				}
				if (loNudge == null) {
					try {
						loNudge = Integer.parseInt((String) properties.get("loNudge"));
					} catch (final NumberFormatException e) {
					}
				}
				if (hiNudge == null) {
					try {
						hiNudge = Integer.parseInt((String) properties.get("hiNudge"));
					} catch (final NumberFormatException e) {
					}
				}
				if (thresholdForFraction == null) {
					try {
						thresholdForFraction = Double.parseDouble((String) properties.get("thresholdForFraction"));
					} catch (final NumberFormatException e) {
					}
				}
				if (equtnNamesString == null) {
					equtnNamesString = properties.getProperty("equationNamesString");
				}
				if (vblNamesString == null) {
					vblNamesString = properties.getProperty("variableNamesString");
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
		_Seed = seed == null ? -1L : seed;
		_LoMult = loMult == null ? -2 : loMult;
		_HiMult = hiMult == null ? 8 : hiMult;
		_LoNudge = loNudge == null ? -3 : loNudge;
		_HiNudge = hiNudge == null ? 7 : hiNudge;
		_ThresholdForFraction = (thresholdForFraction != null && thresholdForFraction > 0d) ? thresholdForFraction
				: 1.0e-3;

		_EqutnNamesString = equtnNamesString == null ? "ABCDEFGHIJKLMN" : equtnNamesString;
		_VblNamesString = vblNamesString == null ? "xyzwuvpqrstabc" : vblNamesString;

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

	public static double[][] generateLinearSystemDbl(final RandomMatrixBuilder rmb, final Difficulty difficulty) {
		final int m = difficulty._m;
		final Random r = rmb.getRng();
		final double[][] coefs;
		if (difficulty._singular) {
			final boolean extraClmn = m == 1 ? true : r.nextBoolean();
			if (extraClmn) {
				final int[] indClmns = CombinatoricTools.getInitial(m);
				coefs = rmb.buildMatrix(m, m + 1, indClmns, /* upperTriangular= */false);
			} else {
				final int[] indClmns = CombinatoricTools.getInitial(m - 1);
				coefs = rmb.buildMatrix(m, m, indClmns, /* upperTriangular= */false);
			}
		} else {
			final int ezM = difficulty._ezM;
			if (ezM == 0) {
				final int[] indClmns = CombinatoricTools.getInitial(m);
				coefs = rmb.buildMatrix(m, m, indClmns, /* upperTriangular= */false);
			} else {
				/**
				 * <pre>
				 * There are 4 parts to the matrix.
				 * 1. Top left square and non-singular,
				 * 2. Bottom right, square and upper triangular,
				 * 3. Bottom left: All 0
				 * 4. Top right: Arbitrary
				 * </pre>
				 */
				final int mTopLeft = m - ezM;
				final double[][] topLeft;
				final double[][] topRight;
				if (mTopLeft == 0) {
					topLeft = topRight = null;
				} else {
					final int[] topLeftIndClmns = CombinatoricTools.getInitial(mTopLeft);
					topLeft = rmb.buildMatrix(mTopLeft, mTopLeft, topLeftIndClmns, /* upperTriangular= */false);
					if (ezM > 0) {
						topRight = new double[mTopLeft][];
						for (int i = 0; i < mTopLeft; ++i) {
							topRight[i] = new double[ezM];
							for (int j = 0; j < ezM; ++j) {
								topRight[i][j] = rmb.nextInt();
							}
						}
					} else {
						topRight = null;
					}
				}
				final double[][] bottomRight;
				if (ezM == 0) {
					bottomRight = null;
				} else {
					final int[] bottomRightIndClmns = CombinatoricTools.getInitial(ezM);
					bottomRight = rmb.buildMatrix(ezM, ezM, bottomRightIndClmns, /* upperTriangular= */true);
				}
				coefs = new double[m][];
				for (int i = 0; i < m; ++i) {
					coefs[i] = new double[m];
					for (int j = 0; j < m; ++j) {
						if (i < mTopLeft && j < mTopLeft) {
							coefs[i][j] = topLeft[i][j];
						} else if (i >= mTopLeft && j >= mTopLeft) {
							coefs[i][j] = bottomRight[i - mTopLeft][j - mTopLeft];
						} else if (i >= mTopLeft && j < mTopLeft) {
							coefs[i][j] = 0d;
						} else {
							coefs[i][j] = topRight[i][j - mTopLeft];
						}
					}
				}
			}
		}
		return coefs;
	}

	public static void main(final String[] args) {
		final long seed = LinearSystemStatics._Seed;
		final int loMult = LinearSystemStatics._LoMult;
		final int hiMult = LinearSystemStatics._HiMult;
		final int loNudge = LinearSystemStatics._LoNudge;
		final int hiNudge = LinearSystemStatics._HiNudge;
		final RandomMatrixBuilder rmb = new RandomMatrixBuilder(seed, loMult, hiMult, loNudge, hiNudge);
		final Difficulty difficulty = new Difficulty(7, 4, false);
		final double[][] coefs = generateLinearSystemDbl(rmb, difficulty);
		final PaqLu paqLu = new PaqLu(coefs);
		System.out.println(NumericalRoutines.getString(coefs));
		System.out.println(paqLu.getString());
	}

}
