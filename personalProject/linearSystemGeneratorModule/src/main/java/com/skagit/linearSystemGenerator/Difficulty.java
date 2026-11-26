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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.ResourcesLister;
import com.skagit.utils.StaticUtilities;

class Difficulty implements Comparable<Difficulty> {

	final static Difficulty[] _GivenDifficulties;
	static {
		final Class<?> clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		final ResourcesLister.Resource[] rsrcs = ResourcesLister.getRsrcs(pckg, "Difficulty.properties");

		final ArrayList<Difficulty> difficultyList = new ArrayList<>();

		Stream.of(rsrcs).takeWhile(r -> difficultyList.isEmpty()).forEach(rsrc -> {
			if (difficultyList.isEmpty()) {
				final Properties properties = new Properties();
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
				final String s0 = (String) properties.get("Given_Difficulties");
				if (s0 != null) {
					final String s = s0.trim();
					final String[] bigFields = s.split("\\n+");
					if (bigFields != null) {
						final int nFields = bigFields.length;
						for (int k = 0; k < nFields; ++k) {
							final String bigField = bigFields[k];
							final String[] littleFields = bigField.split("\\s+");
							if (littleFields.length == 3) {
								try {
									final int m = Integer.parseInt(littleFields[0]);
									final int ezM = Integer.parseInt(littleFields[1]);
									final boolean singular = Boolean.parseBoolean(littleFields[2]);
									difficultyList.add(new Difficulty(m, ezM, singular, difficultyList.size()));
								} catch (final NumberFormatException e) {
								}
							}
						}
					}
				}
				if (difficultyList.isEmpty()) {
					final String s1 = ((String) properties.get("Initial"));
					if (s1 != null) {
						final String s = s1.trim();
						final String[] fields = s.split("\\s+");
						if (fields.length == 3) {
							try {
								final int m = Integer.parseInt(fields[0]);
								final int ezM = Integer.parseInt(fields[1]);
								final boolean singular = Boolean.parseBoolean(fields[2]);
								difficultyList.add(new Difficulty(m, ezM, singular));
							} catch (final NumberFormatException e) {
							}
						}
					}
				}
			}
		});

		if (difficultyList.isEmpty()) {
			difficultyList.add(new Difficulty(2, 1, false));
		}
		_GivenDifficulties = difficultyList.toArray(new Difficulty[difficultyList.size()]);
		Arrays.sort(_GivenDifficulties);
	}

	@Override
	public int compareTo(final Difficulty difficulty) {
		if (difficulty == null) {
			return 1;
		}
		final int ordinal0 = _ordinal;
		final int ordinal1 = difficulty._ordinal;
		return ordinal0 < ordinal1 ? -1 : (ordinal0 > ordinal1 ? 1 : 0);
	}

	final int _m;
	final int _ezM;
	final boolean _singular;
	final int _ordinal;

	/** The maximum value for _ezM is _m-1; this gives a triangular matrix. */
	Difficulty(final int m, final int ezM, final boolean singular) {
		this(m, ezM, singular, /* ordinal= */0);
	}

	private Difficulty(final int m, final int ezM, final boolean singular, final int ordinal) {
		_m = Math.max(1, m);
		_ezM = Math.max(0, Math.min(_m - 1, ezM));
		_singular = singular;
		_ordinal = ordinal;
	}

	Difficulty getHarder() {
		if (_GivenDifficulties.length == 1) {
			return oldGetHarder();
		}
		final int nDifficulties = _GivenDifficulties.length;
		final int glbIdx = CombinatoricTools.getGlbIndex(_GivenDifficulties, this);
		for (int k = glbIdx; k < nDifficulties; ++k) {
			final Difficulty difficulty = _GivenDifficulties[k];
			if (difficulty.compareTo(this) > 0) {
				return difficulty;
			}
		}
		return null;
	}

	Difficulty getEasier() {
		if (_GivenDifficulties.length == 1) {
			return oldGetEasier();
		}
		final int glbIdx = CombinatoricTools.getGlbIndex(_GivenDifficulties, this);
		for (int k = glbIdx; k >= 0; --k) {
			final Difficulty difficulty = _GivenDifficulties[k];
			if (difficulty.compareTo(this) < 0) {
				return difficulty;
			}
		}
		return this;
	}

	Difficulty oldGetHarder() {
		if (_ezM > 0) {
			return new Difficulty(_m, _ezM - 1, /* singular= */false);
		}
		if (!_singular) {
			return new Difficulty(_m, 0, /* singular= */true);
		}
		/** _ezM=0 and singular. Go to next _m. */
		return new Difficulty(_m + 1, _m + 1, /* singular= */false);
	}

	Difficulty oldGetEasier() {
		if (_singular) {
			return new Difficulty(_m, 0, /* singular= */false);
		}
		if (_ezM < _m - 1) {
			return new Difficulty(_m, _ezM + 1, /* singular= */false);
		}
		/** We're as easy as we can get with _m. Back off to previous _m if possible. */
		if (_m > 1) {
			return new Difficulty(_m - 1, 0, /* singular= */true);
		}
		return new Difficulty(1, 1, /* singular= */false);
	}

	public String getString() {
		return String.format("[m,ezM]=[%d,%d%s]", _m, _ezM, _singular ? ",Snglr" : ",NonSnglr");
	}

	@Override
	public String toString() {
		return getString();
	}

}