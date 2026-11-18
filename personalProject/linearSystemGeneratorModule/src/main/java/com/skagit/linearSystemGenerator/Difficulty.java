package com.skagit.linearSystemGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.ResourcesLister0;
import com.skagit.utils.StaticUtilities;

class Difficulty implements Comparable<Difficulty> {

	final static Difficulty _InitialDifficulty;
	final static Difficulty[] _StandardDifficulties;
	static {
		final Class<?> clazz = StaticUtilities.getMyClass();
		final Package pckg = clazz.getPackage();
		final ArrayList<ResourcesLister0.CpEntryAndRelativePath> cpEntryAndRelativePaths1 = ResourcesLister0
				.getCpEntryAndRelativePaths(pckg, "Difficulty.properties", true, true);
		final Properties properties = new Properties();

		/** InitialDifficulty parameters: */
		Integer m0 = null;
		Integer ezM0 = null;
		Boolean singular0 = null;

		for (final ResourcesLister0.CpEntryAndRelativePath cpEntryAndRelativePath : cpEntryAndRelativePaths1) {
			String relativePath = cpEntryAndRelativePath._relativePath;
			relativePath = "/" + relativePath.replace('\\', '/');
			try (InputStream in = clazz.getResourceAsStream(relativePath)) {
				properties.load(in);
				if (m0 == null) {
					final String s = ((String) properties.get("Initial")).trim();
					if (s != null) {
						final String[] fields = s.split("\\s+");
						if (fields.length == 3) {
							try {
								m0 = Integer.parseInt(fields[0]);
								ezM0 = Integer.parseInt(fields[1]);
								singular0 = Boolean.parseBoolean(fields[2]);
							} catch (final NumberFormatException e) {
								m0 = ezM0 = null;
								singular0 = null;
							}
						}
					}
				}
			} catch (final IOException e) {
			}
		}
		m0 = (m0 != null && m0 > 0) ? m0 : 2;
		ezM0 = (ezM0 != null && 0 <= ezM0 && ezM0 <= m0) ? ezM0 : m0;
		singular0 = singular0 != null ? singular0 : false;

		final ArrayList<Difficulty> difficultyList = new ArrayList<>();
		final String s0 = (String) properties.get("Standard_Difficulties");
		if (s0 != null) {
			final String s = s0.trim();
			final String[] bigFields = s.split("\\n+");
			if (bigFields != null) {
				final int nFields = bigFields.length;
				for (int k = 0; k < nFields; ++k) {
					final String bigField = bigFields[k];
					final String[] littleFields = bigField.split("\\s+");
					if (littleFields.length == 3) {
						Integer m = null;
						Integer ezM = null;
						Boolean singular = null;
						try {
							m = Integer.parseInt(littleFields[0]);
							ezM = Integer.parseInt(littleFields[1]);
							singular = Boolean.parseBoolean(littleFields[2]);
							difficultyList.add(new Difficulty(m, ezM, singular, difficultyList.size()));
						} catch (final NumberFormatException e) {
						}
					}
				}
			}
		}
		_StandardDifficulties = difficultyList.toArray(new Difficulty[difficultyList.size()]);
		Arrays.sort(_StandardDifficulties);
		if (_StandardDifficulties.length > 0) {
			_InitialDifficulty = _StandardDifficulties[0];
		} else {
			_InitialDifficulty = new Difficulty(m0, ezM0, singular0, -1);
		}
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
	private Difficulty(final int m, final int ezM, final boolean singular, final int ordinal) {
		_m = Math.max(1, m);
		_ezM = Math.max(0, Math.min(_m - 1, ezM));
		_singular = singular;
		_ordinal = ordinal;
	}

	Difficulty(final int m, final int ezM, final boolean singular) {
		this(m, ezM, singular, -1);
	}

	Difficulty getHarder() {
		if (_ordinal < 0 || _StandardDifficulties == null || _StandardDifficulties.length == 0) {
			return oldGetHarder();
		}
		final int nDifficulties = _StandardDifficulties.length;
		final int glbIdx = CombinatoricTools.getGlbIndex(_StandardDifficulties, this);
		for (int k = glbIdx; k < nDifficulties; ++k) {
			final Difficulty difficulty = _StandardDifficulties[k];
			if (difficulty.compareTo(this) > 0) {
				return difficulty;
			}
		}
		return null;
	}

	Difficulty getEasier() {
		if (_ordinal < 0 || _StandardDifficulties == null || _StandardDifficulties.length == 0) {
			return oldGetEasier();
		}
		final int glbIdx = CombinatoricTools.getGlbIndex(_StandardDifficulties, this);
		for (int k = glbIdx; k >= 0; --k) {
			final Difficulty difficulty = _StandardDifficulties[k];
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
		final String ordinalString = _ordinal < 0 ? "" : ("," + _ordinal);
		final String singularString = _singular ? ",Singular" : "";
		return String.format("[m,ezM]=[%d,%d%s%s]", _m, _ezM, ordinalString, singularString);
	}

	@Override
	public String toString() {
		return getString();
	}

	public static void main0(final String[] args) {
		for (Difficulty difficulty = _StandardDifficulties[0]; difficulty != null; difficulty = difficulty
				.getHarder()) {
			System.out.println(difficulty.getString());
		}
	}

	public static void main(final String[] args) {
		final int nStandardDifficulties = _StandardDifficulties.length;
		for (Difficulty difficulty = _StandardDifficulties[nStandardDifficulties - 1];;) {
			System.out.println(difficulty.getString());
			final Difficulty oldDifficulty = difficulty;
			difficulty = oldDifficulty.getEasier();
			if (difficulty == oldDifficulty) {
				break;
			}
		}
	}

}