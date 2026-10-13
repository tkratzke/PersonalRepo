package com.skagit.audioDirProcessor;

public class CheckReadyForInstall extends Install {

	public CheckReadyForInstall() {
	}

	@Override
	void doTheDirItself(final DirInfo dirInfo) {
	}

	@Override
	String getJobString() {
		return "CHECKING READY FOR INSTALL";
	}

	@Override
	String getLogFNameBase() {
		return "Checking Ready for Install";
	}

}
