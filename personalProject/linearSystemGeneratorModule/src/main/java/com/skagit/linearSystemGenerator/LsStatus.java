package com.skagit.linearSystemGenerator;

public enum LsStatus {
	INFEASIBLE("Infbl"), FEASIBLE("Fbl"), SOLVED("Slvd");

	String _s;

	LsStatus(final String s) {
		_s = s;
	}
}
