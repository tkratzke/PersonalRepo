package com.skagit.audioDirProcessor;

import java.io.File;
import java.util.TreeSet;

public class CheckForInstalled extends AudioDirProcessor {

	public CheckForInstalled() {
		super();
	}

	@Override
	String getJobString() {
		return "CHECKING for INSTALLED";
	}

	@Override
	String getLogFNameBase() {
		return "Checking for Installed";
	}

	@Override
	void doTheDirItself(final DirInfo dirInfo) {
	}

	@Override
	void doAfterSubdirsAreDone(final DirInfo dirInfo) {
	}

	@Override
	boolean isHealthyDir(final DirInfo dirInfo) {
		if (!dirInfo._toDelete.isEmpty() || !dirInfo._srcFName_to_cnvrtdFName.isEmpty()) {
			return false;
		}
		/**
		 * Check that there are converted versions in destnDir of the files in _toCopy
		 * and _needsCnvrtng.
		 */
		final File destnDir = dirInfo._destnDir;
		for (int iPass = 0; iPass < 2; ++iPass) {
			final TreeSet<String> fNames = iPass == 0 ? dirInfo._toCopy : dirInfo._needsCnvrtng;
			for (final String fName : fNames) {
				final String installedFName = getCnvrtdFName(fName);
				final File f = new File(destnDir, installedFName);
				if (!f.isFile()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	String getCautionString(final DirInfo dirInfo) {
		return "";
	}

}
