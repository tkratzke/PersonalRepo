package com.skagit.audioDirProcessor;

public class CheckForBoringFNamesAndTitles extends EliminateBoringFNamesAndTitles {

	public CheckForBoringFNamesAndTitles() {
		super();
	}

	@Override
	void doTheDirItself(final DirInfo dirInfo) {
	}

	@Override
	String getJobString() {
		return "CHECKING FOR BORING FNAMES";
	}

	@Override
	String getLogFNameBase() {
		return "Check For Boring FNames";
	}
}
