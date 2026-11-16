package com.skagit.linearSystemGenerator;

class Difficulty {
	final int _m;
	final int _ezM;
	final boolean _singular;

	/** The maximum value for _ezM is _m-1; this gives a triangular matrix. */
	public Difficulty(final int m, final int ezM, final boolean singular) {
		_m = Math.max(1, m);
		_ezM = Math.max(0, Math.min(_m - 1, ezM));
		_singular = singular;
	}

	Difficulty getHarder() {
		if (_ezM > 0) {
			return new Difficulty(_m, _ezM - 1, /* singular= */false);
		}
		if (!_singular) {
			return new Difficulty(_m, 0, /* singular= */true);
		}
		/** _ezM=0 and singular. Go to next _m. */
		return new Difficulty(_m + 1, _m + 1, /* singular= */false);
	}

	Difficulty getEasier() {
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
		final String singularString = _singular ? ",Singular" : "";
		return String.format("[m,ezM]=[%d,%d%s]", _m, _ezM, singularString);
	}

	@Override
	public String toString() {
		return getString();
	}
}