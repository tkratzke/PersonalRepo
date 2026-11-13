package com.skagit.linearSystemGenerator;

class Difficulty {
	final int _nEquations;
	final TypeOfProblem _typeOfProblem;

	public Difficulty(final int nEquations, final TypeOfProblem typeOfProblem) {
		_nEquations = Math.max(1, nEquations);
		_typeOfProblem = typeOfProblem;
	}

	Difficulty next() {
		switch (_typeOfProblem) {
		case TRI:
			return new Difficulty(_nEquations, TypeOfProblem.FULL);
		case FULL:
			return new Difficulty(_nEquations, TypeOfProblem.ONE_EXTRA);
		case ONE_EXTRA:
			return new Difficulty(_nEquations, TypeOfProblem.FORBID_ZERO_OUT);
		case FORBID_ZERO_OUT:
		default:
			return new Difficulty(_nEquations + 1, TypeOfProblem.TRI);
		}
	}

	Difficulty pvs() {
		switch (_typeOfProblem) {
		case TRI:
			return new Difficulty(Math.max(1, _nEquations - 1), TypeOfProblem.FULL);
		case FULL:
			return new Difficulty(_nEquations, TypeOfProblem.TRI);
		case ONE_EXTRA:
			return new Difficulty(_nEquations, TypeOfProblem.FULL);
		case FORBID_ZERO_OUT:
		default:
			return new Difficulty(_nEquations, TypeOfProblem.ONE_EXTRA);
		}
	}
}