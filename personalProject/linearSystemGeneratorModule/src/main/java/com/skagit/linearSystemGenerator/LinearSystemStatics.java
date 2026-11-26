package com.skagit.linearSystemGenerator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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
		final Properties properties = new Properties();
		final ResourcesLister.Resource[] rsrcs = ResourcesLister.getRsrcs(pckg, "StartUp.properties");
		Stream.of(rsrcs).forEach(rsrc -> {
			final Path path = rsrc.getFilePath();
			if (path != null) {
				try (FileInputStream is = new FileInputStream(path.toFile())) {
					properties.load(is);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			} else {
				try (JarFile jarFile = new JarFile(rsrc._cpEntry.toFile())) {
					final JarEntry entry = jarFile.getJarEntry(rsrc._rltvPath);
					try (InputStream is = jarFile.getInputStream(entry);
							BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
						properties.load(reader);
					}
				} catch (final IOException e) {
				}
			}
		});

		/** Generating linear systems, fraction determination, and names: */
		final long[] seed = { Long.MIN_VALUE };
		final int[] loMult = { Integer.MIN_VALUE };
		final int[] hiMult = { Integer.MIN_VALUE };
		final int[] loNudge = { Integer.MIN_VALUE };
		final int[] hiNudge = { Integer.MIN_VALUE };
		final double[] thresholdForFraction = { Double.NaN };
		final String[] equtnNamesString = { null };
		final String[] vblNamesString = { null };

		/** The remarks for infeasible and solved problems. */
		final ArrayList<String> forInfeasible = new ArrayList<>();
		final ArrayList<String> forSolved = new ArrayList<>();

		Stream.of(rsrcs).takeWhile(rsrc -> !isComplete(seed, loMult, hiMult, loNudge, hiNudge, thresholdForFraction,
				equtnNamesString, vblNamesString)).forEach(rsrc -> {
					String rltvPath = rsrc._rltvPath;
					rltvPath = "/" + rltvPath.replace('\\', '/');
					try (InputStream in = clazz.getResourceAsStream(rltvPath)) {
						properties.load(in);
						if (seed[0] == Long.MIN_VALUE) {
							try {
								seed[0] = Long.parseLong((String) properties.get("seed"));
							} catch (final NumberFormatException e) {
							}
						}
						if (loMult[0] == Integer.MIN_VALUE) {
							try {
								loMult[0] = Integer.parseInt((String) properties.get("loMult"));
							} catch (final NumberFormatException e) {
							}
						}
						if (hiMult[0] == Integer.MIN_VALUE) {
							try {
								hiMult[0] = Integer.parseInt((String) properties.get("hiMult"));
							} catch (final NumberFormatException e) {
							}
						}
						if (loNudge[0] == Integer.MIN_VALUE) {
							try {
								loNudge[0] = Integer.parseInt((String) properties.get("loNudge"));
							} catch (final NumberFormatException e) {
							}
						}
						if (hiNudge[0] == Integer.MIN_VALUE) {
							try {
								hiNudge[0] = Integer.parseInt((String) properties.get("hiNudge"));
							} catch (final NumberFormatException e) {
							}
						}
						if (!Double.isFinite(thresholdForFraction[0])) {
							try {
								thresholdForFraction[0] = Double
										.parseDouble((String) properties.get("thresholdForFraction"));
							} catch (final NumberFormatException e) {
							}
						}
						if (equtnNamesString[0] == null) {
							equtnNamesString[0] = properties.getProperty("equationNamesString");
						}
						if (vblNamesString[0] == null) {
							vblNamesString[0] = properties.getProperty("variableNamesString");
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
				});
		_Seed = seed[0] == Long.MIN_VALUE ? -1L : seed[0];
		_LoMult = loMult[0] == Integer.MIN_VALUE ? -2 : loMult[0];
		_HiMult = hiMult[0] == Integer.MIN_VALUE ? 8 : hiMult[0];
		_LoNudge = loNudge[0] == Integer.MIN_VALUE ? -3 : loNudge[0];
		_HiNudge = hiNudge[0] == Integer.MIN_VALUE ? 7 : hiNudge[0];
		_ThresholdForFraction = (Double.isFinite(thresholdForFraction[0]) && thresholdForFraction[0] > 0d)
				? thresholdForFraction[0]
				: 1.0e-3;

		_EqutnNamesString = equtnNamesString[0] == null ? "ABCDEFGHIJKLMN" : equtnNamesString[0];
		_VblNamesString = vblNamesString[0] == null ? "xyzwuvpqrstabc" : vblNamesString[0];

		if (forInfeasible.isEmpty()) {
			forInfeasible.add("This is not correct.");
		}
		if (forSolved.isEmpty()) {
			forSolved.add("Finished this problem.");
		}
		_RemarksForInfeasible = forInfeasible.toArray(new String[forInfeasible.size()]);
		_RemarksForSolved = forSolved.toArray(new String[forSolved.size()]);
	}

	static private boolean isComplete(final long[] seed, final int[] loMult, final int[] hiMult, final int[] loNudge,
			final int[] hiNudge, final double[] thresholdForFraction, final String[] equtnNamesString,
			final String[] vblNamesString) {
		if (seed[0] == Long.MIN_VALUE) {
			return false;
		}
		if (loMult[0] == Integer.MIN_VALUE) {
			return false;
		}
		if (hiMult[0] == Integer.MIN_VALUE) {
			return false;
		}
		if (loNudge[0] == Integer.MIN_VALUE) {
			return false;
		}
		if (hiNudge[0] == Integer.MIN_VALUE) {
			return false;
		}
		if (Double.isFinite(thresholdForFraction[0])) {
			return false;
		}
		if (equtnNamesString[0] == null) {
			return false;
		}
		if (vblNamesString[0] == null) {
			return false;
		}
		return true;
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
