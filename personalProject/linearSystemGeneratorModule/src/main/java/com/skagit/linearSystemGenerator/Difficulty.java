package com.skagit.linearSystemGenerator;

class Difficulty {
	final int _m;
	final int _ezM;
	final boolean _extraColumn;
	final boolean _squareButSingular;

	public Difficulty(final int m, final int ezM, final boolean extraColumn, final boolean squareButSingular) {
		_m = Math.max(1, m);
		_ezM = Math.max(0, Math.min(_m, ezM));
		_extraColumn = extraColumn;
		_squareButSingular = squareButSingular;
	}

	Difficulty getHarder() {
		if (_ezM > 0) {
			return new Difficulty(_m, _ezM - 1, /* squareButSingular= */false, /* extraColumn= */false);
		}
		if (!_extraColumn) {
			return new Difficulty(_m, 0, /* extraColumn= */true, /* squareButSingular= */false);
		}
		if (!_squareButSingular) {
			return new Difficulty(_m, 0, /* extraColumn= */false, /* squareButSingular= */true);
		}
		return new Difficulty(_m + 1, 0, /* extraColumn= */false, /* squareButSingular= */false);
	}

	Difficulty getEasier() {
		if (_squareButSingular) {
			return new Difficulty(_m, 0, /* extraColumn= */true, /* squareButSingular= */false);
		}
		if (_extraColumn) {
			return new Difficulty(_m, 0, /* extraColumn= */false, /* squareButSingular= */false);
		}
		if (_ezM < _m) {
			return new Difficulty(_m, _ezM + 1, /* extraColumn= */false, /* squareButSingular= */false);
		}
		if (_m > 1) {
			return new Difficulty(_m - 1, 0, /* extraColumn= */false, /* squareButSingular= */false);
		}
		return new Difficulty(1, 1, /* extraColumn= */false, /* squareButSingular= */false);
	}
}